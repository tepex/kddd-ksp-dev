package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import ru.it_arch.clean_ddd.ksp.interop.BoxedType
import ru.it_arch.clean_ddd.ksp.interop.IKDParameter
import ru.it_arch.clean_ddd.ksp.interop.KDValueObjectType
import ru.it_arch.clean_ddd.ksp.interop.TypeSpecWrapper
import ru.it_arch.clean_ddd.ksp.interop.WrapperType
import ru.it_arch.clean_ddd.ksp.interop.asClassNameImplString
import ru.it_arch.clean_ddd.ksp.interop.isValueObject
import ru.it_arch.clean_ddd.ksp.interop.toBuilderPropertySpec
import ru.it_arch.clean_ddd.ksp.interop.toTypeSpecBuilder
import ru.it_arch.clean_ddd.ksp.interop.toValueObjectType

public class DddProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {


    override fun process(resolver: Resolver): List<KSAnnotated> {
        //val symbols = resolver.getAllFiles()
        val symbols = resolver.getNewFiles()
        if (!symbols.iterator().hasNext()) return emptyList()

        logger.warn("symbols: ${symbols.toList()}")
        symbols.forEach { file ->
            file.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.INTERFACE }
                .forEach { declaration ->
                    processDeclaration(file, declaration)
                }
        }

        /*
        resolver.getDeclarationsFromPackage("ru.it_arch.ddd")
            .filterIsInstance<KSClassDeclaration>()
            .find { it.simpleName.asString() == "TestStruct" }
            ?.also { processDeclaration(null, it) }

         */


        return symbols.filterNot { it.validate() }.toList()
    }

    private fun processDeclaration(file: KSFile?, declaration: KSClassDeclaration) {
        declaration.takeIf { it.toValueObjectType(logger).isValueObject }?.apply {
            FileSpec.builder("${declaration.packageName.asString()}.intern", declaration.asClassNameImplString())
                .also { fileBuilder ->
                    logger.warn("process: $declaration")

                    /*
                    declaration.toTypeSpecBuilder(logger, ValueObjectType.ValueObject)
                        .also { accept(ValueObjectVisitor(), it).also(fileBuilder::addType) }*/

                    declaration.createDomainType(ValueObjectVisitor(), KDValueObjectType.KDValueObject).also(fileBuilder::addType)

                    file?.also { fileBuilder.build().writeTo(codeGenerator, Dependencies(false, file)) }
                }
        }
    }

    private fun KSClassDeclaration.createDomainType(visitor: ValueObjectVisitor, voType: KDValueObjectType): TypeSpec =
        toTypeSpecBuilder(logger, voType).let { wrapper ->
            logger.warn("create domain type: ${this}, ${wrapper.parameters}, $voType")
            accept(visitor, wrapper)
        }

    private inner class ValueObjectVisitor : KSDefaultVisitor<TypeSpecWrapper, TypeSpec>() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: TypeSpecWrapper): TypeSpec {
            logger.warn("visit: $classDeclaration, impl: , params: ${data.parameters}")
            val replacements = mutableMapOf<WrapperType, BoxedType>()
            classDeclaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .forEach { nestedDeclaration ->
                    val nestedTypeName = nestedDeclaration.asType(emptyList()).toTypeName()
                    logger.warn("inner decl: $nestedDeclaration, typeName: $nestedTypeName")
                    nestedDeclaration.toValueObjectType(logger)?.also { voType ->
                        nestedDeclaration.createDomainType(this, voType).also(data.builder::addType)
                        if (voType is KDValueObjectType.KDValueObjectSingle)
                            replacements[nestedTypeName] = voType.boxedType
                    } ?: logger.error("Unsupported type declaration", nestedDeclaration)
                }

            // KDType.Builder()
            if (data.valueObjectType is KDValueObjectType.KDValueObject) {
                TypeSpec.classBuilder("Builder").apply {
                    val thisTypeName = classDeclaration.asType(emptyList()).toTypeName()
                    FunSpec.builder("build").also { buildFunBuilder ->
                        buildFunBuilder.addModifiers(KModifier.INTERNAL).returns(thisTypeName)

                        data.parameters.forEach { param ->
                            val type = param.kdType
                            buildFunBuilder.takeIf { type is IKDParameter.KDType.Element && !type.typeName.isNullable }
                                ?.addStatement("""requireNotNull(${param.name.value}) { "Property '$thisTypeName.${param.name.value}' is not set!" }""")
                            param.toBuilderPropertySpec(replacements).also(::addProperty)
                        }


                    }.also { addFunction(it.build()) }
                    build().also(data.builder::addType)
                }
            }

            return data.builder.build()
        }

        override fun defaultHandler(node: KSNode, data: TypeSpecWrapper): TypeSpec {
            TODO("Not yet implemented")
        }
    }
}
