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
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpecHolder
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

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
                    declaration.takeIf { it.isValueObject() }?.also {
                        val fileBuilder = FileSpec.builder(
                            "${declaration.packageName.asString()}.intern",
                            declaration.toClassNameImpl()
                        )

                        logger.warn("process: $declaration")
                        val rootClass = declaration.toTypeSpecBuilder()
                        it.accept(ValueObjectVisitor(), rootClass).also(fileBuilder::addType)

                        fileBuilder.build().writeTo(
                            codeGenerator,
                            Dependencies(false, file)
                        )
                    }
                }
        }

        /* for compiled
        resolver.getDeclarationsFromPackage("ru.it_arch.ddd")
            .find { it.simpleName.asString() == "Validatable" }?.also { it.accept(testVisitor, Unit) }

         */

        return symbols.filterNot { it.validate() }.toList()
    }

    private inner class ValueObjectVisitor() : KSDefaultVisitor<TypeSpec.Builder, TypeSpec>() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: TypeSpec.Builder): TypeSpec {
            logger.warn("visit: $classDeclaration")

            classDeclaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .forEach { declaration ->
                    logger.warn("inner decl: $declaration, typeName: ${declaration.asType(emptyList()).toTypeName()}")
                    declaration.accept(this, declaration.toTypeSpecBuilder()).also(data::addType)
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

    private companion object {
        private const val VALUE_OBJECT = "ru.it_arch.ddd.ValueObject"

        private fun KSClassDeclaration.isValueObject(): Boolean {
            superTypes.forEach { parent ->
                val fullName = parent.resolve().declaration.let { "${it.packageName.asString()}.${it.simpleName.asString()}" }
                if (fullName == VALUE_OBJECT) return true
            }
            return false
        }

        private fun KSClassDeclaration.toClassNameImpl(): String =
            "${simpleName.asString()}Impl"

        private fun KSClassDeclaration.toTypeSpecBuilder(): TypeSpec.Builder =
            TypeSpec.classBuilder(toClassNameImpl())
                .addModifiers(KModifier.INTERNAL)
                .addModifiers(KModifier.DATA)
                .addSuperinterface(asType(emptyList()).toTypeName())
    }
}
