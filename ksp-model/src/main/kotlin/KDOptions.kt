package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import kotlinx.serialization.ExperimentalSerializationApi
import ru.it_arch.k3dm.ValueObject
import kotlin.OptIn

/**
 * Опции KSP.
 * */
@ConsistentCopyVisibility
public data class KDOptions @OptIn(ExperimentalSerializationApi::class) private constructor(
    private val subpackage: Subpackage?,
    private val generatedClassNameRe: Regex,
    private val generatedClassNameResult: ResultTemplate,
    private val useContextParameters: UseContextParameters,
    public val jsonNamingStrategy: JsonNamingStrategy?
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
        src.replaceFirstChar { it.lowercaseChar() }.let { BuilderFunctionName(it) }

    @JvmInline
    public value class PackageName private constructor(override val boxed: String): ValueObject.Value<String> {
        init {
            validate()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<String>> apply(boxed: String): T =
            PackageName(boxed) as T

        override fun validate() {}

        override fun toString(): String =
            boxed

        public companion object {
            public operator fun invoke(boxed: String): PackageName =
                PackageName(boxed)
        }
    }

    @JvmInline
    public value class BuilderFunctionName private constructor(override val boxed: String): ValueObject.Value<String> {
        init {
            validate()
        }

        override fun validate() {}

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<String>> apply(boxed: String): T =
            BuilderFunctionName(boxed) as T

        override fun toString(): String =
            boxed

        public companion object {
            public operator fun invoke(boxed: String): BuilderFunctionName =
                BuilderFunctionName(boxed)
        }
    }

    @JvmInline
    private value class Subpackage private constructor(override val boxed: String): ValueObject.Value<String> {
        init {
            validate()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<String>> apply(boxed: String): T =
            Subpackage(boxed) as T

        // TODO: validate name
        override fun validate() { }

        override fun toString(): String =
            boxed

        companion object {
            operator fun invoke(boxed: String): Subpackage =
                Subpackage(boxed)
        }
    }

    @JvmInline
    private value class ResultTemplate private constructor(override val boxed: String): ValueObject.Value<String> {

        init {
            validate()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<String>> apply(boxed: String): T =
            ResultTemplate(boxed) as T

        override fun validate() {
            require(boxed.contains("\\$\\d+".toRegex())) { "ksp arg $OPTION_GENERATED_CLASS_NAME_RESULT: \"$this\" must contain patterns '$<N>'" }
        }

        override fun toString(): String =
            boxed

        companion object {
            operator fun invoke(boxed: String): ResultTemplate =
                ResultTemplate(boxed)
        }
    }

    @JvmInline
    public value class UseContextParameters private constructor(override val boxed: Boolean) : ValueObject.Value<Boolean> {

        init {
            validate()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<Boolean>> apply(boxed: Boolean): T =
            UseContextParameters(boxed) as T

        override fun validate() {}

        override fun toString(): String =
            boxed.toString()

        public companion object {
            public operator fun invoke(value: Boolean): UseContextParameters =
                UseContextParameters(value)
        }
    }

    public enum class JsonNamingStrategy(
        public val key: String,
        public val className: String
    ) {
        KEBAB("kebab", "kotlinx.serialization.json.JsonNamingStrategy.KebabCase"),
        SNAKE("snake", "kotlinx.serialization.json.JsonNamingStrategy.SnakeCase")
    }

    public companion object {
        private const val OPTION_CONTEXT_PARAMETERS = "contextParameters"
        private const val OPTION_SUBPACKAGE = "subpackage"
        private const val OPTION_GENERATED_CLASS_NAME_RE = "generatedClassNameRe"
        private const val OPTION_GENERATED_CLASS_NAME_RESULT = "generatedClassNameResult"
        private const val DEFAULT_RE = "(.+)"
        private const val DEFAULT_RESULT = "$1Impl"
        private const val OPTION_JSON_NAMING_STRATEGY = "jsonNamingStrategy"

        public operator fun invoke(src: Map<String, String>): KDOptions =
            KDOptions(
                src[OPTION_SUBPACKAGE]?.let { Subpackage(it) },
                (src[OPTION_GENERATED_CLASS_NAME_RE] ?: DEFAULT_RE).toRegex(),
                (src[OPTION_GENERATED_CLASS_NAME_RESULT] ?: DEFAULT_RESULT).let { ResultTemplate(it) },
                UseContextParameters((src[OPTION_CONTEXT_PARAMETERS]?.toBooleanStrictOrNull() ?: false)),
                JsonNamingStrategy.entries.find { it.key == src[OPTION_JSON_NAMING_STRATEGY] }
            )
    }
}
