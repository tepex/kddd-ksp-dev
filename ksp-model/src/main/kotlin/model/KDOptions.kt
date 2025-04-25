package ru.it_arch.clean_ddd.ksp_model.model

import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

/**
 * Опции KSP.
 *
 * @property subpackage имя подпакета сгенерированных имплементаций относительно пакета исходного [Kddd]-интерфейса.
 * @property generatedClassNameRe регулярное выражение для генерации имени класса имплементации на основе имени
 * интерфейса [Kddd]-модели. По умолчанию — `(.+)`.
 * @property generatedClassNameResult шаблон, применяемый к результату регулярного выражения [generatedClassNameRe].
 * С.м. [ResultTemplate]. По умолчанию — `$1Impl`.
 * @property useContextParameters [UseContextParameters]. По умолчанию — `false`.
 * @property jsonNamingStrategy [JsonNamingStrategy]. По умолчанию — `null`.
 * */
@ConsistentCopyVisibility
public data class KDOptions private constructor(
    public val subpackage: Subpackage,
    public val generatedClassNameRe: Regex,
    public val generatedClassNameResult: ResultTemplate,
    public val useContextParameters: UseContextParameters,
    public val jsonNamingStrategy: JsonNamingStrategy?
) : ValueObject.Data {

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T =
        TODO("Not yet implemented")

    @JvmInline
    public value class Subpackage private constructor(override val boxed: String): ValueObject.Boxed<String> {
        init {
            validate()
        }

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T =
            TODO("Not yet implemented")

        // TODO: validate name
        override fun validate() { }

        override fun toString(): String =
            boxed

        public fun isEmpty(): Boolean = boxed.isEmpty()

        public companion object {
            public operator fun invoke(boxed: String?): Subpackage =
                boxed?.let { Subpackage(it) } ?: Subpackage("")
        }
    }

    /**
     * Шаблон, применяемый к результату регулярного выражения в опции [generatedClassNameRe] для формирования
     * имени класса имплементации.
     *
     * Маркеры шаблона — `$<N>`, где N — номер группы в регулярном выражении.
     *
     * Пример. Допустим для исходного [Kddd]-интерфейса с именем `IMyType1` необходимо сгенерировать имя имплементации `MyTypeDefault1`, тогда:
     * ```
     * generatedClassNameRe = "I(\\w+)(\\d+)"
     * generatedClassNameResult = "$1Default$2"
     *
     * ```
     * */
    @JvmInline
    public value class ResultTemplate(override val boxed: String): ValueObject.Boxed<String> {
        init {
            validate()
        }

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T =
            TODO("Not yet implemented")

        override fun validate() {
            require(boxed.contains("\\$\\d+".toRegex())) { "KSP arg $OPTION_GENERATED_CLASS_NAME_RESULT: \"$this\" must contain patterns '$<N>'" }
        }

        override fun toString(): String =
            boxed

        internal companion object {
            operator fun invoke(boxed: String): ResultTemplate =
                ResultTemplate(boxed)
        }
    }

    /** Использование фичи
     * [Context parameters](https://github.com/Kotlin/KEEP/blob/context-parameters/proposals/context-parameters.md) в
     * генерируемой DSL builder функции.
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
    @JvmInline
    public value class UseContextParameters private constructor(override val boxed: Boolean) : ValueObject.Boxed<Boolean> {
        init {
            validate()
        }

        override fun <T : ValueObject.Boxed<Boolean>> fork(boxed: Boolean): T =
            TODO("Not yet implemented")

        override fun validate() {}

        override fun toString(): String =
            boxed.toString()

        public companion object {
            public operator fun invoke(value: Boolean): UseContextParameters =
                UseContextParameters(value)
        }
    }

    /**
     * Стратегия трансформации сложных имен полей в JSON-формате.
     *
     * Варианты:
     * - не задан — имя поля используется как есть.
     * - `kebab` — [через черточку][kotlinx.serialization.json.JsonNamingStrategy.KebabCase].
     * - `snake` — [через подчеркивание][kotlinx.serialization.json.JsonNamingStrategy.SnakeCase].
     */
    public enum class JsonNamingStrategy(
        public val key: String,
        public val className: String
    ) {
        KEBAB("kebab", "kotlinx.serialization.json.JsonNamingStrategy.KebabCase"),
        SNAKE("snake", "kotlinx.serialization.json.JsonNamingStrategy.SnakeCase")
    }

    public companion object {
        private const val OPTION_CONTEXT_PARAMETERS = "contextParameters"
        private const val OPTION_IMPLEMENTATION_SUBPACKAGE = "implementationSubpackage"
        private const val OPTION_GENERATED_CLASS_NAME_RE = "generatedClassNameRe"
        private const val OPTION_GENERATED_CLASS_NAME_RESULT = "generatedClassNameResult"
        private const val DEFAULT_RE = "(.+)"
        private const val DEFAULT_RESULT = "$1Impl"
        private const val OPTION_JSON_NAMING_STRATEGY = "jsonNamingStrategy"

        public operator fun invoke(src: Map<String, String>): KDOptions =
            KDOptions(
                Subpackage(src.getOrDefault(OPTION_IMPLEMENTATION_SUBPACKAGE, "")),
                src.getOrDefault(OPTION_GENERATED_CLASS_NAME_RE, DEFAULT_RE).toRegex(),
                ResultTemplate(src.getOrDefault(OPTION_GENERATED_CLASS_NAME_RESULT, DEFAULT_RESULT)),
                UseContextParameters(src[OPTION_CONTEXT_PARAMETERS]?.toBooleanStrictOrNull() ?: false),
                JsonNamingStrategy.entries.find { it.key == src[OPTION_JSON_NAMING_STRATEGY] }
            )
    }
}
