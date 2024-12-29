package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asTypeName
import ru.it_arch.clean_ddd.ksp.model.KDType
import ru.it_arch.kddd.ValueObject

public data class KDOutputFile private constructor(
    public val kdType: KDType,
    public val dependencies: Dependencies,
    private val packageName: KDOptions.PackageName,
    private val toBeGenerated: ClassName,
    private val builderFunName: KDOptions.BuilderFunctionName
) : ValueObject.Data {

    override fun validate() {}

    public fun buildFileSpec(): FileSpec =
        FileSpec.builder(packageName.boxed, toBeGenerated.simpleName).also { fileBuilder ->
            fileBuilder.addFileComment(FILE_HEADER_STUB)

            (kdType as? KDType.Generatable)?.builder?.build()?.also(fileBuilder::addType)

            /* Root DSL builder */
            val receiver: ClassName = ClassName(packageName.boxed, toBeGenerated.simpleName, KDType.Data.DSL_BUILDER_CLASS_NAME)
            ParameterSpec.builder(
                "block",
                LambdaTypeName.get(receiver = receiver, returnType = Unit::class.asTypeName())
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

    public companion object {
        context(KDOptions)
        public fun create(
            declaration: KSClassDeclaration,
            kdType: KDType,
            srcFile: KSFile,
        ): KDOutputFile =KDOutputFile(
            kdType,
            Dependencies(false, srcFile),
            declaration.implementationPackage,
            declaration.implementationClassName,
            declaration.builderFunctionName,
        )
    }
}
