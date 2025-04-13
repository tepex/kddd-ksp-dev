package ru.it_arch.clean_ddd.ksp.model

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
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.ValueObject

/**
 * Основная модель фреймворка.
 *
 * Определяет все имеющиеся типы.
 * */
public sealed interface KDType {
    public val sourceTypeName: TypeName

    /**
     * Определяет вид DDD-модели: `Value Object` или `Entity`
     * */
    public interface Model : Generatable {
        // MyModel.Builder
        public val builderClassName: ClassName
        // MyModel.DslBuilder
        public val dslBuilderClassName: ClassName
    }

    /**
     * Определяет тип, генерирующий объекты KotlinPoet
     * */
    public interface Generatable : KDType {
        /** Имя класса сгенерированной имплементации */
        public val className: ClassName
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
        /** Список свойств KDDD-модели */
        public val propertyHolders: List<KDProperty>
        /** Реестр (внутренний реестр) вложенных [KDType] типов объявленных в KDDD-модели */
        public val nestedTypes: Map<TypeName, KDType>
        //public val annotation: KDGeneratable?
        public val hasDsl: Boolean
        public val hasJson: Boolean

        //public fun dslBuilderFunName(isInner: Boolean): String
        /**
         * Добавление инстанса [KDType] во внутренний реестр
         * */
        public fun addNestedType(type: KDType)
        /**
         *  Получение инстанса [KDType] из внутреннего или глобального реестра KDDD-типов.
         *
         *  @param typeName тип свойства модели
         *  @return результат поиска. [KDTypeSearchResult].first: [KDType], [KDTypeSearchResult].second: true — найден во внутреннем реестре, false — найден в других моделях
         *  @throws IllegalStateException — не найден
         * */
        public fun getKDType(typeName: TypeName): KDTypeSearchResult
    }

    /**
     * TODO: Not implemented yet
     * */
    @JvmInline
    public value class Sealed private constructor(
        override val sourceTypeName: TypeName,
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
        override val sourceTypeName: TypeName = ValueObject::class.asTypeName()
        val TYPENAME: TypeName = ValueObject::class.asTypeName()

        override fun toString(): String = "Abstraction"
    }

    /**
     * Определяет DDD `Value Object`
     * */
    public class Data private constructor(
        private val forGeneration: KDTypeForGeneration
    ) : Generatable by forGeneration, Model {

        override val builderClassName: ClassName = ClassName.bestGuess("${className.simpleName}.$BUILDER_CLASS_NAME")
        override val dslBuilderClassName: ClassName = ClassName.bestGuess("${className.simpleName}.$DSL_BUILDER_CLASS_NAME")

        override fun toString(): String =
            "Data($className)"

        public companion object {
            public const val BUILDER_CLASS_NAME: String = "Builder"
            public const val DSL_BUILDER_CLASS_NAME: String = "DslBuilder"
            public const val BUILDER_BUILD_METHOD_NAME: String = "build"
            public const val APPLY_BUILDER: String = "%T().apply(%N).$BUILDER_BUILD_METHOD_NAME()"

            context(ctx: KDTypeContext)
            public operator fun invoke(annotations: List<Annotation>, isEntity: Boolean): Data =
                Data(KDTypeForGeneration(ctx, annotations, null, isEntity))
        }
    }

    /**
     * Определяет DDD сущность `Entity`
     * */
    public class IEntity private constructor(private val data: Data) : Model by data {

        private val paramId = propertyHolders.find { it.name.simpleName == ID_NAME }
            ?: error("ID parameter not found for Entity $className")

        override fun toString(): String =
            "IEntity[id: $paramId, data: $data]"

        public fun generateBaseContract() {
            FunSpec.builder("hashCode").apply {
                addModifiers(KModifier.OVERRIDE)
                addStatement("return %N.hashCode()", paramId.name)
                returns(Int::class)
            }.build().also(builder::addFunction)

            val paramOther = ParameterSpec.builder("other", ANY.toNullable()).build()
            FunSpec.builder("equals").apply {
                addModifiers(KModifier.OVERRIDE)
                addParameter(paramOther)
                addStatement("if (this === other) return true")
                addStatement("if (%N !is %T) return false", paramOther, className)
                addStatement("if (%N != %N.%N) return false", paramId.name, paramOther, paramId.name)
                addStatement("return true")
                returns(Boolean::class)
            }.build().also(builder::addFunction)

            // override fun toString()

            propertyHolders.filter { it.name.simpleName != ID_NAME }
                .fold(mutableListOf<Pair<String, MemberName>>()) { acc, param -> acc.apply { add("%N: $%N" to param.name) } }
                .let { it.joinToString { pair -> pair.first } to it.fold(mutableListOf(paramId.name)) { acc, pair ->
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
            public operator fun invoke(annotations: List<Annotation>): IEntity =
                Data(annotations, true).let(KDType::IEntity)
        }
    }

    /**
     * Определяет котлиновскую класс-обертку `value class`
     * */
    public class Boxed private constructor(
        private val forGeneration: KDTypeForGeneration,
        public val boxedType: TypeName,
    ) : Generatable by forGeneration, KDType {

        public val isUseStringInDsl: Boolean =
            forGeneration.annotations.filterIsInstance<KDParsable>().firstOrNull()?.useStringInDsl ?: false

        public val isParsable: Boolean =
            forGeneration.annotations.filterIsInstance<KDParsable>().isNotEmpty()

        public val fabricMethod: String =
            FABRIC_PARSE_METHOD.takeIf { isParsable && isUseStringInDsl } ?: FABRIC_CREATE_METHOD

        /** Тип источника создания объекта через DSL. String, если [isParsable] или непосредственно. */
        public val rawTypeName: TypeName =
            String::class.asTypeName().takeIf { isParsable && isUseStringInDsl } ?: boxedType

        public fun asIsOrSerialize(variable: String, isNullable: Boolean): String =
            StringBuilder().apply {
                append("$variable?.$PARAM_NAME".takeIf { isNullable } ?: "$variable.$PARAM_NAME")
                forGeneration.annotations.filterIsInstance<KDParsable>().firstOrNull()?.serialization
                    .takeIf { isUseStringInDsl }?.also { append(".$it") }
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
                forGeneration.annotations.filterIsInstance<KDParsable>().firstOrNull()?.serialization?.also { append(".$it") }
            }.toString()

        public fun asDeserialize(variable: String, isNullable: Boolean): String =
            if (isNullable) "$variable?.let { $classNameRef(it) }" else "$classNameRef($variable)"

        override fun toString(): String =
            "KDType.Boxed<$boxedType>"

        public companion object {
            public const val PARAM_NAME: String = "boxed"
            public const val FABRIC_CREATE_METHOD: String = "create"
            public const val FABRIC_PARSE_METHOD: String = "parse"
            public const val CREATE_METHOD: String = "copy"

            context(ctx: KDTypeContext)
            public operator fun invoke(annotations: List<Annotation>, superInterfaceName: TypeName): Boxed = run {
                require(superInterfaceName is ParameterizedTypeName && superInterfaceName.typeArguments.size == 1) {
                    "Class name `$superInterfaceName` expected type parameter"
                }
                superInterfaceName.typeArguments.first()
                    .let { Boxed(KDTypeForGeneration(ctx, annotations, it), it) }
            }
        }
    }
}
