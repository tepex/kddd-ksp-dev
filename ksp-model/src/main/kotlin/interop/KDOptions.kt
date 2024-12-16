package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.symbol.KSClassDeclaration
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
public data class KDOptions private constructor(
    private val subpackage: Subpackage?,
    private val generatedClassNameRe: Regex,
    private val generatedClassNameResult: ResultTemplate
) {

    public fun getPackage(declaration: KSClassDeclaration): String =
        declaration.packageName.asString().let { base -> subpackage?.let { "$base.${it.boxed}" } ?: base }

    public fun generateClassName(src: String): String {
        var result = generatedClassNameResult.boxed
        generatedClassNameRe.find(src)?.groupValues?.forEachIndexed() { i, group ->
            group.takeIf { i > 0 }?.also { result = result.replace("\$$i", it) }
        }
        return result
    }

    @JvmInline
    public value class Subpackage(override val boxed: String): ValueObject.Boxed<String> {
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

        public companion object {
            public fun create(boxed: String): Subpackage =
                Subpackage(boxed)
        }
    }

    @JvmInline
    public value class ResultTemplate(override val boxed: String): ValueObject.Boxed<String> {
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

        public companion object {
            public fun create(boxed: String): ResultTemplate =
                ResultTemplate(boxed)
        }
    }

    public companion object {
        private const val OPTION_SUBPACKAGE = "subpackage"
        private const val OPTION_GENERATED_CLASS_NAME_RE = "generatedClassNameRe"
        private const val OPTION_GENERATED_CLASS_NAME_RESULT = "generatedClassNameResult"
        private const val DEFAULT_RE = "(.+)"
        private const val DEFAULT_RESULT = "$1Impl"

        public fun create(src: Map<String, String>): KDOptions =
            KDOptions(
                src[OPTION_SUBPACKAGE]?.let(Subpackage::create),
                (src[OPTION_GENERATED_CLASS_NAME_RE] ?: DEFAULT_RE).toRegex(),
                (src[OPTION_GENERATED_CLASS_NAME_RESULT] ?: DEFAULT_RESULT).let(ResultTemplate::create)
            )
    }
}
