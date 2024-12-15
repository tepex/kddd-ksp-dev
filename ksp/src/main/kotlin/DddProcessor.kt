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
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import ru.it_arch.clean_ddd.ksp.interop.KDTypeHelper
import ru.it_arch.clean_ddd.ksp.interop.KDTypeHelper.Property
import ru.it_arch.clean_ddd.ksp.interop.KDLogger
import ru.it_arch.clean_ddd.ksp.interop.KDType
import ru.it_arch.clean_ddd.ksp.interop.KDTypeBuilderBuilder
import ru.it_arch.clean_ddd.ksp.interop.toClassNameImpl

internal class DddProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
    private val isTesting: Boolean
) : SymbolProcessor {

    private val kdLogger = KDLoggerImpl(logger)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        //val symbols = resolver.getAllFiles()
        val symbols = resolver.getNewFiles()
        if (!symbols.iterator().hasNext()) return emptyList()

        logger.warn("options: $options, isTesting: $isTesting")
        logger.warn("symbols: ${symbols.toList()}")
        symbols.forEach { file ->
            file.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.INTERFACE }
                .forEach { declaration ->
                    createKDType(ValueObjectVisitor(), declaration)?.let { kdType ->
                        KDContext.create(declaration, kdType).also { it.processDeclaration(file) }
                    }
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

    private fun KDContext.processDeclaration(file: KSFile?) {
        FileSpec.builder(packageName, implClassName.simpleName).also { fileBuilder ->
            fileBuilder.addFileComment("""
AUTO-GENERATED FILE. DO NOT MODIFY.
This file generated automatically by «KDDD» framework.
Author: Tepex <tepex@mail.ru>, Telegram: @Tepex
""".trimIndent())

            if (kdType is KDType.HasImpl) kdType.builder.build().also(fileBuilder::addType)

            /* Root DSL builder */
            ParameterSpec.builder(
                "block",
                LambdaTypeName.get(receiver = receiver, returnType = Unit::class.asTypeName())
            ).build().also { builderParam ->
                FunSpec.builder(builderFunName)
                    .addParameter(builderParam)
                    .addStatement(
                        "return %T().apply(%N).${KDType.Data.BUILDER_BUILD_METHOD_NAME}()",
                        receiver,
                        builderParam
                    )
                    .returns(implClassName)
                    .build().also(fileBuilder::addFunction)
            }
            file?.also { fileBuilder.build().writeTo(codeGenerator, Dependencies(false, file)) }
        }
    }

    private fun createKDType(visitor: ValueObjectVisitor, declaration: KSClassDeclaration): KDType? =
        declaration.toKDType(isTesting, kdLogger)
            .also { if (it is KDType.HasImpl) declaration.accept(visitor, it) }

    private inner class ValueObjectVisitor : KSDefaultVisitor<KDType.HasImpl, Unit>() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KDType.HasImpl) {
            classDeclaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .forEach { nestedDeclaration ->
                    createKDType(this, nestedDeclaration)
                        ?.also { data.addNestedType(nestedDeclaration.asType(emptyList()).toTypeName(), it) }
                        ?: logger.error("Unsupported type declaration", nestedDeclaration)
                }

            /* KDType.Builder() */
            if (data is KDType.Model) {
                KDTypeBuilderBuilder(data, false, kdLogger).also { helper ->
                    data.builder.addType(helper.build())
                    helper.buildFunToBuilder().also(data.builder::addFunction)
                }
                KDTypeBuilderBuilder(data, true, kdLogger).also { helper ->
                    data.builder.addType(helper.build())
                    helper.buildFunToBuilder().also(data.builder::addFunction)
                }
            }
        }

        override fun defaultHandler(node: KSNode, data: KDType.HasImpl) {}
    }

    private class KDContext private constructor(
        val kdType: KDType,
        val packageName: String,
        val implClassName: ClassName,
        val builderFunName: String,
    ) {
        val receiver = ClassName(packageName, implClassName.simpleName, KDType.Data.DSL_BUILDER_CLASS_NAME)

        companion object {
            fun create(declaration: KSClassDeclaration, kdType: KDType) = KDContext(
                kdType,
                "${declaration.packageName.asString()}.impl",
                declaration.toClassNameImpl(),
                declaration.simpleName.asString().replaceFirstChar { it.lowercaseChar() }
            )
        }
    }
}

internal fun KSClassDeclaration.toKDType(isTesting: Boolean, logger: KDLogger): KDType? {
    val className = toClassNameImpl()
    val helper = KDTypeHelper(
        className,
        asType(emptyList()).toTypeName(),
        getAllProperties()
            .map { Property(className.member(it.simpleName.asString()), it.type.toTypeName()) }.toList()
    )
    superTypes.forEach { parent ->
        when(parent.toString()) {
            KDType.Sealed::class.java.simpleName -> KDType.Sealed.create(helper.typeName)
            KDType.Data::class.java.simpleName -> KDType.Data.create(helper, false)
            KDType.IEntity::class.java.simpleName -> KDType.IEntity.create(helper)
            KDType.Boxed::class.java.simpleName -> {
                runCatching { KDType.Boxed.create(helper, parent.toTypeName()) }.getOrElse {
                    logger.log(it.message ?: "Cant parse parent type $parent")
                    null
                }
            }
            else -> null
        }?.also { return it }
    }
    return null
}
