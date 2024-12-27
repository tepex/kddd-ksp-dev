package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import ru.it_arch.clean_ddd.ksp.model.KDType
import ru.it_arch.kddd.ValueObject

public data class KDOutputFile private constructor(
    public val kdType: KDType,
    private val packageName: PackageName,
    private val toBeGenerated: ClassName,
    private val builderFunName: BuilderFunctionName,
    private val dependencies: Dependencies
) : ValueObject.Data {

    override fun validate() {}

    private val receiver: ClassName = ClassName(packageName.boxed, toBeGenerated.simpleName, KDType.Data.DSL_BUILDER_CLASS_NAME)

    public fun generate(codeGenerator: CodeGenerator) {
        FileSpec.builder(packageName.boxed, toBeGenerated.simpleName).also { fileBuilder ->
            fileBuilder.addFileComment(FILE_HEADER_STUB)

            (kdType as? KDType.Generatable)?.builder?.build()?.also(fileBuilder::addType)

            /* Root DSL builder */
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
            fileBuilder.build().writeTo(codeGenerator, dependencies)
        }
    }

    @JvmInline
    private value class PackageName(override val boxed: String): ValueObject.Boxed<String> {
        init {
            validate()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<String>> copy(boxed: String): T =
            create(boxed) as T

        override fun validate() {}

        override fun toString(): String =
            boxed

        public companion object {
            public fun create(boxed: String): PackageName =
                PackageName(boxed)
        }
    }

    @JvmInline
    private value class BuilderFunctionName(override val boxed: String): ValueObject.Boxed<String> {
        init {
            validate()
        }

        override fun validate() {}

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<String>> copy(boxed: String): T =
            create(boxed) as T

        override fun toString(): String =
            boxed

        public companion object {
            public fun create(boxed: String): BuilderFunctionName =
                BuilderFunctionName(boxed)
        }
    }


    public companion object {
        public fun create(
            declaration: KSClassDeclaration,
            packageName: String,
            kdType: KDType,
            srcFile: KSFile,
            generateClassName: KSClassDeclaration.() -> ClassName
        ): KDOutputFile = KDOutputFile(
            kdType,
            PackageName.create(packageName),
            declaration.generateClassName(),
            declaration.simpleName.asString().replaceFirstChar { it.lowercaseChar() }.let(BuilderFunctionName::create),
            Dependencies(false, srcFile)
        )
    }
}
