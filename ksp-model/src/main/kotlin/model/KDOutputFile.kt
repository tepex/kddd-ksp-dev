package ru.it_arch.clean_ddd.ksp_model.model

import com.squareup.kotlinpoet.FileSpec
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

/**
 * Определяет генерируемый файл для класса имплементации.
 *
 * @property model
 * @property packageName
 * */
@ConsistentCopyVisibility
public data class KDOutputFile private constructor(
    public val model: KDType.Model,
    private val packageName: String,
    //private val builderFunName: KDOptions.BuilderFunctionName,
    //private val useContextParameters: KDOptions.UseContextParameters
) : ValueObject.Data {

    public val fileSpecBuilder: FileSpec.Builder by lazy {
        FileSpec.builder(packageName, model.implName.simpleName).also { fileBuilder ->
            fileBuilder.addFileComment(FILE_HEADER_STUB)

            fileBuilder.addType(model.builder.build())
            /*
            if (generatable.hasDsl) {
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
            }*/
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
            model: KDType.Model,
            packageName: String,
            //builderFunName: KDOptions.BuilderFunctionName,
            //useContextParameters: KDOptions.UseContextParameters
        ): KDOutputFile = KDOutputFile(model, packageName)
    }
}
