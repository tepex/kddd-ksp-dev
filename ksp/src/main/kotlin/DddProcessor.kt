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
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import ru.it_arch.clean_ddd.ksp.interop.KDReference
import ru.it_arch.clean_ddd.ksp.interop.KDType
import ru.it_arch.clean_ddd.ksp.interop.KDValueObjectType
import ru.it_arch.clean_ddd.ksp.interop.toClassNameImpl
import ru.it_arch.clean_ddd.ksp.interop.createImplBuilder
import ru.it_arch.clean_ddd.ksp.interop.isValueObject
import ru.it_arch.clean_ddd.ksp.interop.toKDType
import ru.it_arch.clean_ddd.ksp.interop.toNullable
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
            FileSpec.builder("${declaration.packageName.asString()}.intern", declaration.toClassNameImpl().simpleName)
                .also { fileBuilder ->
                    logger.warn("process: $declaration")
                    createKDType(ValueObjectVisitor(), declaration, KDValueObjectType.KDValueObject).builder.build()
                        .also(fileBuilder::addType)
                    file?.also { fileBuilder.build().writeTo(codeGenerator, Dependencies(false, file)) }
                }
        }
    }

    private fun createKDType(visitor: ValueObjectVisitor, declaration: KSClassDeclaration, voType: KDValueObjectType) =
        declaration.toKDType(logger, voType).also { kdType ->
            logger.warn("create KDType: $declaration, implName: ${kdType.className.simpleName}, $voType")
            declaration.accept(visitor, kdType)
        }

    private inner class ValueObjectVisitor : KSDefaultVisitor<KDType, Unit>() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KDType) {
            logger.warn("visit: $classDeclaration, impl: ${data.className.simpleName}, params: ${data.parameters}")
            classDeclaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .forEach { nestedDeclaration ->
                    val nestedTypeName = nestedDeclaration.asType(emptyList()).toTypeName()
                    logger.warn("inner decl: $nestedDeclaration, typeName: $nestedTypeName")
                    nestedDeclaration.toValueObjectType(logger)?.also { voType ->
                        createKDType(this, nestedDeclaration, voType)
                            .also { data.addInnerType(nestedDeclaration.asType(emptyList()).toTypeName(), it) }
                    } ?: logger.error("Unsupported type declaration", nestedDeclaration)
                }

            // KDType.Builder()
            if (data.valueObjectType is KDValueObjectType.KDValueObject) {
                //logger.warn("... Builder ${data.innerTypes.map { "${it.key} -> ${it.value.className} boxed: ${data.getBoxedTypeOrNull(it.key)}" } }")

                data.parameters.forEach { param ->
                    when (param.typeReference) {
                        is KDReference.Element -> {
                            val innerType = data.getInnerType(param.typeReference.typeName)
                            //logger.warn("  Element innerType: $innerType")
                        }
                        is KDReference.Collection -> {}
                    }
                }


                data.createImplBuilder(classDeclaration.asType(emptyList()).toTypeName(), logger)
            }
        }

        override fun defaultHandler(node: KSNode, data: KDType) {}
    }
}
