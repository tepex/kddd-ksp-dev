package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asTypeName
import ru.it_arch.kddd.ValueObject

public data class KDOutputFile(
    public val kdType: KDType,
    private val packageName: KDOptions.PackageName,
    private val toBeGenerated: ClassName,
    private val builderFunName: KDOptions.BuilderFunctionName,
    private val useContextReceivers: KDOptions.UseContextReceivers
) : ValueObject.Data {

    override fun validate() {}

    @OptIn(ExperimentalKotlinPoetApi::class)
    public fun buildFileSpec(): FileSpec =
        FileSpec.builder(packageName.boxed, toBeGenerated.simpleName).also { fileBuilder ->
            fileBuilder.addFileComment(FILE_HEADER_STUB)

            (kdType as? KDType.Generatable)?.builder?.build()?.also(fileBuilder::addType)

            /* Root DSL builder */
            val receiver: ClassName = ClassName(packageName.boxed, toBeGenerated.simpleName, KDType.Data.DSL_BUILDER_CLASS_NAME)
            ParameterSpec.builder(
                "block",
                if (useContextReceivers.boxed) LambdaTypeName.get(contextReceivers = listOf(receiver), returnType = Unit::class.asTypeName())
                else LambdaTypeName.get(receiver = receiver, returnType = Unit::class.asTypeName())
            ).build().also { builderParam ->
                FunSpec.builder(builderFunName.boxed)
                    .addParameter(builderParam)
                    .addStatement(
                        "return %T().apply(%N).${KDType.Data.BUILDER_BUILD_METHOD_NAME}()",
                        receiver,
                        builderParam
                    )
                    .returns(toBeGenerated)
                    .build().also(fileBuilder::addFunction)
            }
        }.build()

    private companion object {
        const val FILE_HEADER_STUB: String = """
AUTO-GENERATED FILE. DO NOT MODIFY.
This file generated automatically by «KDDD» framework.
Author: Tepex <tepex@mail.ru>, Telegram: @Tepex
"""
    }
}
