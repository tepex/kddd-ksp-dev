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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import ru.it_arch.clean_ddd.ksp.interop.KDType
import ru.it_arch.clean_ddd.ksp.interop.KDValueObjectType
import ru.it_arch.clean_ddd.ksp.interop.toClassNameImpl
import ru.it_arch.clean_ddd.ksp.interop.createImplBuilder
import ru.it_arch.clean_ddd.ksp.interop.isValueObject
import ru.it_arch.clean_ddd.ksp.interop.toKDType
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

        logger.warn("options: $options")
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
            val packageName = "${declaration.packageName.asString()}.impl"
            val implClassName = declaration.toClassNameImpl()
            FileSpec.builder(packageName, implClassName.simpleName).also { fileBuilder ->
                fileBuilder.addFileComment("""
AUTO-GENERATED FILE. DO NOT MODIFY.
This file generated automatically by «KDDD» framework.
Author: Tepex <tepex@mail.ru>, Telegram: @Tepex
""".trimIndent())

                logger.warn("process: $declaration")
                createKDType(ValueObjectVisitor(), declaration, KDValueObjectType.KDValueObject).builder.build()
                    .also(fileBuilder::addType)

                /* Root DSL builder */
                val receiver = ClassName(packageName, implClassName.simpleName, KDType.BUILDER_CLASS_NAME)
                val builderParam = ParameterSpec.builder(
                    "block",
                    LambdaTypeName.get(
                        receiver = receiver,
                        returnType = Unit::class.asTypeName()
                    )
                ).build()
                FunSpec.builder(declaration.simpleName.asString().replaceFirstChar { it.lowercaseChar() })
                    .addParameter(builderParam)
                    .addStatement("return %T().apply(%N).${KDType.BUILDER_BUILD_METHOD_NAME}()", receiver, builderParam)
                    .returns(implClassName)
                    .build()
                    .also(fileBuilder::addFunction)

                file?.also { fileBuilder.build().writeTo(codeGenerator, Dependencies(false, file)) }
            }
        }
    }

    private fun createKDType(visitor: ValueObjectVisitor, declaration: KSClassDeclaration, voType: KDValueObjectType) =
        declaration.toKDType(voType, logger).also { declaration.accept(visitor, it) }

    private inner class ValueObjectVisitor : KSDefaultVisitor<KDType, Unit>() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KDType) {
            classDeclaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .forEach { nestedDeclaration ->
                    val nestedTypeName = nestedDeclaration.asType(emptyList()).toTypeName()
                    nestedDeclaration.toValueObjectType(logger)?.also { voType ->
                        createKDType(this, nestedDeclaration, voType)
                            .also { data.addNestedType(nestedDeclaration.asType(emptyList()).toTypeName(), it) }
                    } ?: logger.error("Unsupported type declaration", nestedDeclaration)
                }

            /* KDType.Builder() */
            if (data.valueObjectType is KDValueObjectType.KDValueObject) data.createImplBuilder(data.className, logger)
        }

        override fun defaultHandler(node: KSNode, data: KDType) {}
    }
}
