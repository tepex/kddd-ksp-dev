package ru.it_arch.clean_ddd.ksp_model.model

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import kotlinx.serialization.encoding.CompositeEncoder
import ru.it_arch.clean_ddd.ksp_model.KDTypeForGeneration
import ru.it_arch.clean_ddd.ksp_model.KDTypeSearchResult
import ru.it_arch.clean_ddd.ksp_model.toNullable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

/**
 * Основная модель фреймворка.
 *
 * Определяет все возможные [Kddd]-типы.
 *
 * @property name имя интерфейса исходной [Kddd]-модели.
 * */
public sealed interface KDType {
    public val name: TypeName

    /**
     * Определяет вид корневой [Kddd]-модели: [ValueObject.Data] или [IEntity].
     *
     * @property innerBuilderClassName имя класса обычного билдера в импллементации. Имя определяется константой [KDType.Data.BUILDER_CLASS_NAME].
     * @property innerDslBuilderClassName имя класса DSL-билдера в импллементации. Имя определяется константой [KDType.Data.DSL_BUILDER_CLASS_NAME].
     * */
    public interface Model : Generatable {
        public val innerBuilderClassName: ClassName
        public val innerDslBuilderClassName: ClassName
    }

    /**
     * Определяет тип, генерирующий объекты `KotlinPoet.`
     *
     * @property implName полностью квалифицированное имя класса имплементации для [KDType.name].
     * @property builder [KotlinPoet](https://square.github.io/kotlinpoet/1.x/kotlinpoet/kotlinpoet/com.squareup.kotlinpoet/-type-spec/-builder/index.html) билдер класса имплементации.
     * @property properties список свойств [Kddd]-типа.
     * @property nestedTypes внутренний реестр вложенных [KDType] типов объявленных в [Kddd]-типе.
     * @property hasDsl наличие DSL фичи.
     * @property hasJson наличие фичи JSON-сериализации.
     * */
    public interface Generatable : KDType {
        public val implName: ClassName
        /** Имя класса для ссылки на него из-вне. Если он внутренний (inner), то будет содержать имя класса-контейнера
         *
         * Пример:
         * ```
         * class Clazz {
         *     class Inner {
         *     }
         * }
         * ```
         * Результат: "Clazz.Inner"
         * */
        public val classNameRef: String
        public val builder: TypeSpec.Builder
        public val properties: List<KDProperty>
        public val nestedTypes: Set<KDType>
        public val hasDsl: Boolean
        public val hasJson: Boolean

        //public fun dslBuilderFunName(isInner: Boolean): String
        /**
         * Добавление инстанса [KDType] во внутренний реестр.
         *
         * @param type внутренний тип.
         * */
        public fun addNestedType(type: KDType)

        /**
         *  Поиск инстанса [KDType] из внутреннего или глобального реестра [Kddd]-типов.
         *
         *  @param name тип свойства [Kddd]-модели.
         *  @return результат поиска. [KDTypeSearchResult].first: [KDType], [KDTypeSearchResult].second: `true` — найден во внутреннем реестре, `false` — найден в других моделях
         *  @throws IllegalStateException — не найден.
         * */
        public fun getKDType(name: TypeName): KDTypeSearchResult
    }

    /**
     * TODO: Not implemented yet
     * */
    @JvmInline
    public value class Sealed private constructor(
        override val name: TypeName,
    ) : KDType {

        public companion object {
            context(ctx: KDTypeContext)
            public operator fun invoke(): Sealed = Sealed(ctx.typeName)
        }
    }

    /**
     * TODO: Not implemented yet
     * */
    public data object Abstraction : KDType {
        override val name: TypeName = ValueObject::class.asTypeName()
        val TYPENAME: TypeName = ValueObject::class.asTypeName()

        override fun toString(): String = "Abstraction"
    }

    /**
     * Определяет DDD `Value Object`.
     *
     * @property forGeneration делегат [KDTypeForGeneration].
     * */
    public class Data private constructor(
        private val forGeneration: KDTypeForGeneration
    ) : Generatable by forGeneration, Model {

        override val innerBuilderClassName: ClassName = ClassName.bestGuess("${implName.simpleName}.$BUILDER_CLASS_NAME")
        override val innerDslBuilderClassName: ClassName = ClassName.bestGuess("${implName.simpleName}.$DSL_BUILDER_CLASS_NAME")

        override fun toString(): String =
            "Data(${implName})"

        override fun hashCode(): Int =
            forGeneration.name.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Data) return false
            if (forGeneration.name != other.forGeneration.name) return false
            return true
        }

        public companion object {
            public const val BUILDER_CLASS_NAME: String = "Builder"
            public const val DSL_BUILDER_CLASS_NAME: String = "DslBuilder"
            public const val BUILDER_BUILD_METHOD_NAME: String = "build"
            public const val APPLY_BUILDER: String = "%T().apply(%N).$BUILDER_BUILD_METHOD_NAME()"

            context(ctx: KDTypeContext)
            public operator fun invoke(isEntity: Boolean = false): Data =
                Data(KDTypeForGeneration(ctx, null, isEntity))
        }
    }

    /**
     * Определяет DDD сущность `Entity`.
     *
     * @property id идентичность сущности.
     * @property data содержимое сущности.
     * */
    public class IEntity private constructor(private val data: Data) : Model by data {

        private val id = properties.find { it.name.simpleName == ID_NAME }
            ?: error("ID parameter not found for Entity $implName")

        override fun toString(): String =
            "IEntity[id: $id, data: $data]"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is IEntity) return false
            if (id != other.id) return false
            return true
        }

        override fun hashCode(): Int =
            id.hashCode()

        /**
         * Генерация переопределения контракта `hashCode()/equals()`, завязанного на [id].
         *
         * Также генерация переопределения метода `toString()`.
         * */
        public fun generateBaseContract() {
            FunSpec.builder("hashCode").apply {
                addModifiers(KModifier.OVERRIDE)
                addStatement("return %N.hashCode()", id.name)
                returns(Int::class)
            }.build().also(builder::addFunction)

            val paramOther = ParameterSpec.builder("other", ANY.toNullable()).build()
            FunSpec.builder("equals").apply {
                addModifiers(KModifier.OVERRIDE)
                addParameter(paramOther)
                addStatement("if (this === other) return true")
                addStatement("if (%N !is %T) return false", paramOther, implName)
                addStatement("if (%N != %N.%N) return false", id.name, paramOther, id.name)
                addStatement("return true")
                returns(Boolean::class)
            }.build().also(builder::addFunction)

            // override fun toString()

            properties.filter { it.name.simpleName != ID_NAME }
                .fold(mutableListOf<Pair<String, MemberName>>()) { acc, param -> acc.apply { add("%N: $%N" to param.name) } }
                .let { it.joinToString { pair -> pair.first } to it.fold(mutableListOf(id.name)) { acc, pair ->
                    acc.apply {
                        add(pair.second)
                        add(pair.second)
                    }
                } }.also { pair ->
                    FunSpec.builder("toString").apply {
                        addModifiers(KModifier.OVERRIDE)
                        returns(String::class)
                        addStatement("return \"[ID: $%N, ${pair.first}]\"", *pair.second.toTypedArray())
                    }.build().also(builder::addFunction)
                }
        }

        public companion object {
            public const val ID_NAME: String = "id"

            context(ctx: KDTypeContext)
            public operator fun invoke(): IEntity =
                Data(true).let(KDType::IEntity)
        }
    }

    /**
     * Определяет котлиновскую класс-обертку `value class`.
     *
     * @property forGeneration делегат [KDTypeForGeneration].
     * @property boxedType тип содержимого.
     * @property isUseStringInDsl `true` — DSL-builder использует строку для создания объекта. С.м. [KDParsable].
     * @property isParsable `true` — [Kddd]-тип помечен аннотацией [KDParsable] и содержимое можно имплементировать через статический метод, который создает объект из строки. С.м. [KDParsable].
     * @property fabricMethod имя фабричного метода для создания объекта из строки.
     * @property rawType тип источника создания объекта через DSL. [String] — если [isParsable] или непосредственно.
     * */
    public class Boxed private constructor(
        private val forGeneration: KDTypeForGeneration,
        public val boxedType: TypeName,
    ) : Generatable by forGeneration, KDType {

        public val isUseStringInDsl: Boolean =
            forGeneration.getAnnotation<KDParsable>()?.useStringInDsl ?: false

        public val isParsable: Boolean =
            forGeneration.getAnnotation<KDParsable>() != null

        public val fabricMethod: String =
            FABRIC_PARSE_METHOD.takeIf { isParsable && isUseStringInDsl } ?: FABRIC_CREATE_METHOD

        public val rawType: TypeName =
            boxedType.takeUnless { isParsable && isUseStringInDsl } ?: String::class.asTypeName()

        public fun asIsOrSerialize(variable: String, isNullable: Boolean): String =
            StringBuilder().apply {
                append("$variable?.$PARAM_NAME".takeIf { isNullable } ?: "$variable.$PARAM_NAME")
                forGeneration.getAnnotation<KDParsable>()?.serialization.takeIf { isUseStringInDsl }
                    ?.also { append(".$it") }
            }.toString()

        /** Формирует строковое представление параметра `value` для функций
         * [CompositeEncoder.encodeNullableSerializableElement], [CompositeEncoder.encodeStringElement] и прочих
         *
         * Если указана аннотация [KDParsable] с параметром `serialization`, то добавляется этот метод
         *
         * @param variable имя свойства KDDD-модели
         * @param isNullable признак нулабельности свойства
         * @return сгенерированный код параметра
         * */
        public fun asSerialize(variable: String, isNullable: Boolean): String =
            StringBuilder().apply {
                append("$variable?.$PARAM_NAME".takeIf { isNullable } ?: "$variable.$PARAM_NAME")
                forGeneration.getAnnotation<KDParsable>()?.serialization?.also { append(".$it") }
            }.toString()

        public fun asDeserialize(variable: String, isNullable: Boolean): String =
            "TODO"
            //if (isNullable) "$variable?.let { $classNameRef(it) }" else "$classNameRef($variable)"

        override fun toString(): String =
            "KDType.Boxed<$boxedType>"

        public companion object {
            public const val PARAM_NAME: String = "boxed"
            public const val FABRIC_CREATE_METHOD: String = "create"
            public const val FABRIC_PARSE_METHOD: String = "parse"
            public const val CREATE_METHOD: String = "fork"

            context(ctx: KDTypeContext)
            public operator fun invoke(annotations: List<Annotation>, superInterfaceName: TypeName): Boxed = run {
                require(superInterfaceName is ParameterizedTypeName && superInterfaceName.typeArguments.size == 1) {
                    "Class name `$superInterfaceName` expected type parameter"
                }
                superInterfaceName.typeArguments.first()
                    .let { Boxed(KDTypeForGeneration(ctx, it, false), it) }
            }
        }
    }
}
