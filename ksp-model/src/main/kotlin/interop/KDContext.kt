package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import ru.it_arch.clean_ddd.ksp.model.KDType
import ru.it_arch.kddd.ValueObject

public class KDContext private constructor(
    public val kdType: KDType,
    public val packageName: PackageName,
    public val toBeGenerated: ClassName,
    public val builderFunName: BuilderFunctionName,
) {
    public val receiver: ClassName = ClassName(packageName.boxed, toBeGenerated.simpleName, KDType.Data.DSL_BUILDER_CLASS_NAME)

    @JvmInline
    public value class PackageName(override val boxed: String): ValueObject.Boxed<String> {
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
    public value class BuilderFunctionName(override val boxed: String): ValueObject.Boxed<String> {
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
            generateClassName: KSClassDeclaration.() -> ClassName
        ): KDContext = KDContext(
            kdType,
            PackageName.create(packageName),
            declaration.generateClassName(),
            declaration.simpleName.asString().replaceFirstChar { it.lowercaseChar() }.let(BuilderFunctionName::create)
        )
    }
}
