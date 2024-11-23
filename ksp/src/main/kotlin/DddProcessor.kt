package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import ru.it_arch.clean_ddd.ksp.interop.VO_TYPE
import ru.it_arch.clean_ddd.ksp.interop.toClassNameImpl
import ru.it_arch.clean_ddd.ksp.interop.toTypeSpecBuilder
import ru.it_arch.clean_ddd.ksp.interop.toValueObjectType

public class DddProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {


    @OptIn(KspExperimental::class)
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

        resolver.getDeclarationsFromPackage("ru.it_arch.ddd")
            .filterIsInstance<KSClassDeclaration>()
            .find { it.simpleName.asString() == "TestStruct" }
            ?.also { processDeclaration(null, it) }


        return symbols.filterNot { it.validate() }.toList()
    }

    private fun processDeclaration(file: KSFile?, declaration: KSClassDeclaration) {
        declaration.takeIf { it.toValueObjectType(logger) == VO_TYPE.VALUE_OBJECT }?.also {
            val fileBuilder = FileSpec.builder(
                "${declaration.packageName.asString()}.intern",
                declaration.toClassNameImpl()
            )

            logger.warn("process: $declaration")
            val rootClass = declaration.toTypeSpecBuilder(logger, VO_TYPE.VALUE_OBJECT)
            it.accept(ValueObjectVisitor(), rootClass).also(fileBuilder::addType)

            file?.also { fileBuilder.build().writeTo(codeGenerator, Dependencies(false, it)) }
        }
    }

    private inner class ValueObjectVisitor() : KSDefaultVisitor<TypeSpec.Builder, TypeSpec>() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: TypeSpec.Builder): TypeSpec {
            logger.warn("visit: $classDeclaration")

            classDeclaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .forEach { declaration ->
                    logger.warn("inner decl: $declaration, typeName: ${declaration.asType(emptyList()).toTypeName()}")
                    declaration.toValueObjectType(logger)
                        ?.also { voType ->
                            declaration.accept(
                                this,
                                declaration.toTypeSpecBuilder(logger, voType)
                            ).also(data::addType)
                        }
                        ?: logger.error("Unsupported type declaration", declaration)

                }
            return data.build()
        }

        override fun defaultHandler(node: KSNode, data: TypeSpec.Builder): TypeSpec {
            TODO("Not yet implemented")
        }

    }

    private inner class TestVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            logger.warn("test: $classDeclaration")
            classDeclaration.getAllFunctions().forEach { visitFunctionDeclaration(it, Unit) }
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            logger.warn("func: $function")
        }
    }
}
