package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import ru.it_arch.clean_ddd.ksp.interop.KDOptions.ResultTemplate.Companion
import ru.it_arch.kddd.ValueObject

public class KDOptions private constructor(
    private val subpackage: Subpackage?,
    private val generatedClassNameRe: Regex,
    private val generatedClassNameResult: ResultTemplate,
    public val useContextReceivers: UseContextReceivers
) {

    public val KSClassDeclaration.implementationPackage: PackageName get() =
        packageName.asString().let { base -> subpackage?.let { "$base.${it.boxed}" } ?: base }.let(::PackageName)

    public val String.implementationName: String get() {
        var result = generatedClassNameResult.boxed
        generatedClassNameRe.find(this)?.groupValues?.forEachIndexed { i, group ->
            group.takeIf { i > 0 }?.also { result = result.replace("\$$i", it) }
        }
        return result
    }

    public val KSClassDeclaration.implementationClassName: ClassName get() =
        simpleName.asString().implementationName.let(ClassName::bestGuess)

    public val KSClassDeclaration.builderFunctionName: BuilderFunctionName get() =
        simpleName.asString().replaceFirstChar { it.lowercaseChar() }.let(BuilderFunctionName::create)

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

    @JvmInline
    private value class Subpackage(override val boxed: String): ValueObject.Boxed<String> {
        init {
            validate()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<String>> copy(boxed: String): T =
            create(boxed) as T

        // TODO: validate name
        override fun validate() { }

        override fun toString(): String =
            boxed

        companion object {
            fun create(boxed: String): Subpackage =
                Subpackage(boxed)
        }
    }

    @JvmInline
    private value class ResultTemplate(override val boxed: String): ValueObject.Boxed<String> {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<String>> copy(boxed: String): T =
            create(boxed) as T

        init {
            validate()
        }

        override fun validate() {
            require(boxed.contains("\\$\\d+".toRegex())) { "ksp arg $OPTION_GENERATED_CLASS_NAME_RESULT: \"$this\" must contain patterns '$<N>'" }
        }

        override fun toString(): String =
            boxed

        companion object {
            fun create(boxed: String): ResultTemplate =
                ResultTemplate(boxed)
        }
    }

    @JvmInline
    public value class UseContextReceivers private constructor(override val boxed: Boolean) : ValueObject.Boxed<Boolean> {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<Boolean>> copy(boxed: Boolean): T =
            UseContextReceivers.create(boxed) as T

        init {
            validate()
        }

        override fun validate() {}

        public companion object {
            public fun create(value: Boolean): UseContextReceivers =
                UseContextReceivers(value)
        }
    }

    public companion object {
        private const val OPTION_CONTEXT_RECEIVERS = "contextReceivers"
        private const val OPTION_SUBPACKAGE = "subpackage"
        private const val OPTION_GENERATED_CLASS_NAME_RE = "generatedClassNameRe"
        private const val OPTION_GENERATED_CLASS_NAME_RESULT = "generatedClassNameResult"
        private const val DEFAULT_RE = "(.+)"
        private const val DEFAULT_RESULT = "$1Impl"

        public fun create(src: Map<String, String>): KDOptions =
            KDOptions(
                src[OPTION_SUBPACKAGE]?.let(Subpackage::create),
                (src[OPTION_GENERATED_CLASS_NAME_RE] ?: DEFAULT_RE).toRegex(),
                (src[OPTION_GENERATED_CLASS_NAME_RESULT] ?: DEFAULT_RESULT).let(ResultTemplate::create),
                (src[OPTION_CONTEXT_RECEIVERS]?.toBooleanStrictOrNull() ?: false).let(UseContextReceivers::create)
            )
    }
}
