package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asTypeName
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
public data class KDOutputFile private constructor(
    public val generatable: KDType.Generatable,
    private val packageName: KDOptions.PackageName,
    private val builderFunName: KDOptions.BuilderFunctionName,
    private val isUseContextParameters: Boolean
) : ValueObject.Data {

    @kotlin.OptIn(ExperimentalKotlinPoetApi::class)
    public val fileSpecBuilder: FileSpec.Builder by lazy {
        FileSpec.builder(packageName.boxed, generatable.className.simpleName).also { fileBuilder ->
            fileBuilder.addFileComment(FILE_HEADER_STUB)

            fileBuilder.addType(generatable.builder.build())

            if (generatable.hasDsl) {
                /* Root DSL builder */
                val receiver =
                    ClassName(packageName.boxed, generatable.className.simpleName, KDType.Data.DSL_BUILDER_CLASS_NAME)
                ParameterSpec.builder(
                    "block",
                    if (isUseContextParameters) LambdaTypeName.get(
                        contextReceivers = listOf(receiver),
                        returnType = Unit::class.asTypeName()
                    )
                    else LambdaTypeName.get(receiver = receiver, returnType = Unit::class.asTypeName())
                ).build().also { builderParam ->
                    FunSpec.builder(builderFunName.boxed)
                        .addParameter(builderParam)
                        .addStatement(
                            "return %T().apply(%N).${KDType.Data.BUILDER_BUILD_METHOD_NAME}()",
                            receiver,
                            builderParam
                        )
                        //.returns(generatable.className)
                        .returns(generatable.kDddTypeName)
                        .build().also(fileBuilder::addFunction)
                }
            }
        }
    }


    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    public companion object {
        private const val FILE_HEADER_STUB: String = """
AUTO-GENERATED FILE. DO NOT MODIFY.
This file generated automatically by «KDDD» framework.
Author: Tepex <tepex@mail.ru>, Telegram: @Tepex
"""
        public operator fun invoke(
            generatable: KDType.Generatable,
            packageName: KDOptions.PackageName,
            builderFunName: KDOptions.BuilderFunctionName,
            isUseContextParameters: Boolean
        ): KDOutputFile = KDOutputFile(generatable, packageName, builderFunName, isUseContextParameters)
    }
}
