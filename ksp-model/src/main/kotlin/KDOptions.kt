package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import ru.it_arch.kddd.ValueObject

/**
 * Опции KSP.
 * */
public data class KDOptions private constructor(
    private val subpackage: Subpackage?,
    private val generatedClassNameRe: Regex,
    private val generatedClassNameResult: ResultTemplate,
    private val useContextParameters: UseContextParameters
) {

    /**
     * Опция определяет, будет ли использоваться фича [Context parameters](https://github.com/Kotlin/KEEP/blob/context-parameters/proposals/context-parameters.md)
     * в генерируемой DSL builder функции.
     *
     * По умолчанию — false
     *
     * Пример сгенерированного кода со значением `false`:
     * ```
     * fun myType(block: MyTypeImpl.DslBuilder.() -> Unit): MyTypeImpl = ...
     * ```
     * Со значением `true`:
     * ```
     * fun myType(block: context(MyTypeImpl.DslBuilder) () -> Unit): MyTypeImpl = ...
     * ```
     * */
    public val isUseContextParameters: Boolean = useContextParameters.boxed

    /**
     * Опция определяет имя подпакета для сгенерированных имплементаций.
     * */
    public fun getImplementationPackage(src: String): PackageName =
        PackageName(subpackage?.let { "$src.${it.boxed}" } ?: src)

    public fun toImplementationName(src: String): String {
        var result = generatedClassNameResult.boxed
        generatedClassNameRe.find(src)?.groupValues?.forEachIndexed { i, group ->
            group.takeIf { i > 0 }?.also { result = result.replace("\$$i", it) }
        }
        return result
    }

    public fun getImplementationClassName(src: String): ClassName =
        toImplementationName(src).let(ClassName::bestGuess)

    public fun getBuilderFunctionName(src: String): BuilderFunctionName =
        src.replaceFirstChar { it.lowercaseChar() }.let(BuilderFunctionName.Companion::create)

    @JvmInline
    public value class PackageName(override val boxed: String): ValueObject.Boxed<String> {
        init {
            validate()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<String>> create(boxed: String): T =
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
        override fun <T : ValueObject.Boxed<String>> create(boxed: String): T =
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
        override fun <T : ValueObject.Boxed<String>> create(boxed: String): T =
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
        override fun <T : ValueObject.Boxed<String>> create(boxed: String): T =
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
    public value class UseContextParameters private constructor(override val boxed: Boolean) : ValueObject.Boxed<Boolean> {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<Boolean>> create(boxed: Boolean): T =
            UseContextParameters(boxed) as T

        init {
            validate()
        }

        override fun validate() {}

        override fun toString(): String =
            boxed.toString()

        public companion object {
            public operator fun invoke(value: Boolean): UseContextParameters =
                UseContextParameters(value)
        }
    }

    public companion object {
        private const val OPTION_CONTEXT_PARAMETERS = "contextParameters"
        private const val OPTION_SUBPACKAGE = "subpackage"
        private const val OPTION_GENERATED_CLASS_NAME_RE = "generatedClassNameRe"
        private const val OPTION_GENERATED_CLASS_NAME_RESULT = "generatedClassNameResult"
        private const val DEFAULT_RE = "(.+)"
        private const val DEFAULT_RESULT = "$1Impl"

        public operator fun invoke(src: Map<String, String>): KDOptions =
            KDOptions(
                src[OPTION_SUBPACKAGE]?.let(Subpackage.Companion::create),
                (src[OPTION_GENERATED_CLASS_NAME_RE] ?: DEFAULT_RE).toRegex(),
                (src[OPTION_GENERATED_CLASS_NAME_RESULT] ?: DEFAULT_RESULT).let(ResultTemplate.Companion::create),
                UseContextParameters((src[OPTION_CONTEXT_PARAMETERS]?.toBooleanStrictOrNull() ?: false))
            )
    }
}
