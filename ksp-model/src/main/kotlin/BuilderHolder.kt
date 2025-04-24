package ru.it_arch.clean_ddd.ksp_model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import ru.it_arch.clean_ddd.ksp_model.model.KDOptions
import ru.it_arch.clean_ddd.ksp_model.model.KDProperty
import ru.it_arch.clean_ddd.ksp_model.model.KDType
import ru.it_arch.clean_ddd.ksp_model.utils.KDLogger
import ru.it_arch.kddd.Kddd

/**
 * Контейнер `KotlinPoet`-билдеров для [KDType.Model] и соответствующих функций-расширений.
 *
 * Билдер состоит из:
 * 1. набора неинициализированных полей (для коллекций — пустые коллекции; нуллабельные коллекции не применяются),
 * 2. функции `build(): MyType`, которая валидирует инициализацию ненуллабельных скалярных полей,
 * 3. конструктора имплементации, которая возвращается этой функцией.
 *
 * Для классического билдера типы полей совпадают с типами полей [Kddd]-модели.
 *
 * Для DSL-билдера типы полей определяются их конкретным «примитивизированным» содержимым или строкой. Соответственно,
 * необходимо воссоздать их [Kddd]-тип в теле метода `build()` билдера с помощью соответствующих методов (их
 * собственных билдеров или их метода `parse()`).
 *
 * В случае коллекций воссоздается параметризированный тип коллекции. Т.к. коллекции могут быть вложенными
 * многоуровнево (Collection<Collection<T>>), то для подмены параметризованного типа производится рекурсивный обход.
 *
 * Пример генерируемых билдеров и соответствующих функций-расширений:
 * ```
 * public data class MyTypeImpl private constructor (
 *     override val prop1: MyType.KdddType1
 *     override val prop2: MyType.KdddType2
 *     ...
 * ) : MyType {
 *
 *     class Builder() {
 *         public lateinit var prop1: MyType.KdddType1
 *         public lateinit var prop2: MyType.KdddType2
 *         ...
 *
 *         public fun build(): MyType {
 *             require(::prop1.isInitialized) { "Property 'prop1' is not initialized!" }
 *             require(::prop2.isInitialized) { "Property 'prop2' is not initialized!" }
 *             ...
 *             return MyTypeImpl(prop1, prop2, ...)
 *         }
 *     }
 *
 *     class DslBuilder() {
 *         public var prop1: String? = null
 *         public var prop2: String? = null // если установлено `useStringInDsl` в аннотации @KDParsable, или `public var prop2: UUID? = null`
 *         ...
 *
 *         public fun build(): MyType {
 *             requireNotNull(prop1) { "Property 'prop1' is not initialized!" }
 *             requireNotNull(prop2) { "Property 'prop2' is not initialized!" }
 *             ...
 *             return MyTypeImpl(KdddType1Impl(prop1!!), KdddType2Impl.parse(prop2!!), ...)
 *         }
 *     }
 * }
 *
 * public fun MyType.toBuilder(): MyTypeImpl.Builder {
 *     val ret = MyTypeImpl.Builder()
 *     ret.prop1 = prop1
 *     ret.prop2 = prop2
 *     return ret
 * }
 *
 * public fun MyType.toDslBuilder(): MyTypeImpl.DslBuilder {
 *     val ret = MyTypeImpl.DslBuilder()
 *     ret.prop1 = prop1.boxed
 *     ret.prop2 = prop2.boxed.toString() // или метод из значения `serialization` в аннотации @KDParsable
 *     return ret
 * }
 * ```
 *
 * @property options опции фреймворка.
 * @property logger внутренний логгер.
 * @property model [KDType.Model], для которой создаются билдеры.
 * @property isDsl `true` — для DSL-билдера, `false` — для клоссического билдера.
 * @property innerBuilderClass [TypeSpec.Builder] для внутреннего класса билдера `class Builder`/`class DslBuilder`.
 * @property innerBuilderFunBuild [FunSpec.Builder] для метода `build()` в классе билдере [innerBuilderClass].
 * @property toBuilderFunHolder контейнер [ToBuilderFunHolder] для функций-расширений `toBuilder()`/`toDslBuilder()`.
 * */
public class BuilderHolder private constructor(
    private val options: KDOptions,
    private val logger: KDLogger,
    private val model: KDType.Model,
    private val isDsl: Boolean
) {

    // `class <Dsl>Builder`
    private val innerBuilderClass =
        (model.innerDslBuilderClassName.takeIf { isDsl } ?: model.innerBuilderClassName).let(TypeSpec::classBuilder)

    // `fun build(): MyType`
    private val innerBuilderFunBuild =
        FunSpec.builder(KDType.Data.BUILDER_BUILD_METHOD_NAME)
            .returns(model.name)

    // `fun MyType.to<Dsl>Builder()`
    private val toBuilderFunHolder =
        ToBuilderFunHolder(
            model.name,
            (model.innerDslBuilderClassName.takeIf { isDsl } ?: model.innerBuilderClassName), isDsl
        )

    /**
     * Формирование тела билдера.
     *
     * 1. Для скалярных типов
     * 2. Формирование списка параметров для конструктора имплементации, который возвращается в функции `build()` билдера.
     * ```
     * return MyTypeImpl(arg1 = ..., arg2 = ..., ...)
     * ```
     * 3. Для вложенных [Kddd]-типов типа [KDType.Model] создаются функции DSL-билдеров.
     * */
    init {
        FunSpecStatement(Chunk("return %T(", model.implName)).apply {
            model.properties.forEach { property ->
                property.type.takeIf { it.isCollection() }?.toParametrizedType()
                    ?.also { addParameterForCollection(property.memberName, it) }
                    ?: run { // элемент
                        // Блок валидации свойств билдера
                        if (property.type.isNullable.not())
                            innerBuilderFunBuild.addStatement(
                                """${if (isDsl) "requireNotNull(%M)" else "require(::%M.isInitialized)"} { "Property '%T.%M' is not set!" }""",
                                property.memberName,
                                model.name,
                                property.memberName
                            )

                        // return new PropertySpec
                        model.getKDType(property.type).first.let { kdType ->
                            addParameterForElement(property)
                            /* Error: must be from type
                            if (kdType is KDType.Model && isDsl)
                                createDslBuilder(property.name, kdType).also(innerBuilderClass::addFunction)*/

                            if (kdType is KDType.Boxed && isDsl) kdType.rawType else property.type
                        }.let {
                            if (!isDsl && !property.type.isNullable) PropertySpec.builder(property.memberName.simpleName, it)
                                .addModifiers(KModifier.LATEINIT)
                            else PropertySpec.builder(property.memberName.simpleName, it.toNullable()).initializer("null")
                        }
                    } // PropertySpec.Builder
                        .mutable()
                        .build()
                        .also(innerBuilderClass::addProperty)
            }

            final()
            addTo(innerBuilderFunBuild)
        }
        // <3>
        model.nestedTypes.filterIsInstance<KDType.Model>().forEach { type ->
            type createDslBuilderFun options.useContextParameters
        }
    }

    public fun build(): TypeSpec =
        innerBuilderClass.addFunction(innerBuilderFunBuild.build()).build()

    public fun buildToBuilderFun(): FunSpec =
        toBuilderFunHolder.build()

    /**
     * Формирование из свойства [Kddd]-модели соответствуюших:
     * 1. аргумента конструктора имплементации,
     * 2. инициализация поля билдера.
     *
     * @param property свойство [Kddd]-модели.
     **/
    private fun FunSpecStatement.addParameterForElement(property: KDProperty) {
        +Chunk("%N = ", property.memberName)
        val element =
            model.getKDType(property.type).let { DSLType.Element(it, property.type.isNullable) }

        if (element.kdType is KDType.Boxed && isDsl) {
            // <1>
            val parse = if (element.kdType.isParsable && element.kdType.isUseStringInDsl) ".${KDType.Boxed.FABRIC_PARSE_METHOD}" else ""
            if (element.name.isNullable) +Chunk("%N?.let { %T$parse(it) }, ", property.memberName, element.kdType.implName)
            else +Chunk("%T$parse(%N!!), ", element.kdType.implName, property.memberName)
            // <2>
            //logger.log("$property isParsable: ${element.kdType.isParsable} bozedType: ${element.kdType.boxedType} ")
            toBuilderFunHolder.element(property.memberName, element.name.isNullable, element.kdType.isParsable && element.kdType.isUseStringInDsl)
        } else {
            // <1>
            +Chunk("%N${if (!element.name.isNullable && isDsl) "!!" else ""},", property.memberName)
            // <2>
            toBuilderFunHolder.asIs(property.memberName)
        }
    }

    /**
     * Формирование из свойства-коллекции [Kddd]-модели соответствуюших:
     * 1. аргумента конструктора имплементации,
     * 2. инициализация поля билдера.
     *
     * @param name имя свойства [Kddd]-модели.
     * @param type параметризованный тип коллекции этого свойства.
     * @return [PropertySpec.Builder] билдер свойства.
     **/
    private fun FunSpecStatement.addParameterForCollection(name: MemberName, type: ParameterizedTypeName): PropertySpec.Builder {
        val collectionType = type.toCollectionType()
        return if (isDsl) {
            // Т.к. в DSL-варианте параметризованные типы коллекций полей билдера определяются примитивами,
            // то необ
            traverseParameterizedTypes(DSLType.Collection(type, logger)).let { substituted ->
                // <1>
                +Chunk("%N = %N${substituted.fromDslMapper}", name, name)
                endStatement()
                // <2>
                // TODO: refactor in case no DSL
                toBuilderFunHolder.fromDsl(name, substituted.toDslMapper)
                substituted.parameterizedName
                    .let { PropertySpec.builder(name.simpleName, it).initializer(collectionType initializer true) }
            }
        } else {
            // <1>
            +Chunk("%N = %N", name, name)
            endStatement()
            // <2>
            toBuilderFunHolder.asIs(name)
            PropertySpec.builder(name.simpleName, type).initializer(collectionType initializer false)
        }
    }

    // ValueObject.Boxed<BOXED> -> BOXED for DSL
    // https://discuss.kotlinlang.org/t/3-tailrec-questions/3981 #3
    private tailrec fun traverseParameterizedTypes(node: DSLType.Collection): DSLType.Collection {
        val ret = node.substituteOrNull()
        return if (ret != null) ret
        else {
            traverseParameterizedTypes(node.apply {
                substituteArgs { arg ->
                    @Suppress("NON_TAIL_RECURSIVE_CALL")
                    if (arg is ParameterizedTypeName) traverseParameterizedTypes(DSLType.Collection(arg, logger))
                    else model.getKDType(arg).let { DSLType.Element(it, arg.isNullable) }
                }
            })
        }
    }

    /**
     * Вспомогательный класс-holder для генерации `fun MyType.toBuilder(): MyTypeImpl.Builder` или `fun MyType.toDslBuilder(): MyTypeImpl.DslBuilder`
     *
     * @param receiverTypeName имя исходной KDDD-модели
     * @param builderTypeName тип билдера
     * @param isDsl вид билдера: false — обычный, true — DSL
     * */
    private class ToBuilderFunHolder(receiverTypeName: TypeName, builderTypeName: ClassName, isDsl: Boolean) {
        private val builder =
            FunSpec.builder("toDslBuilder".takeIf { isDsl } ?: "toBuilder")
                .receiver(receiverTypeName)
                .returns(builderTypeName)
                .addStatement("val $RET = %T()", builderTypeName)

        // toDslBuilder: ret.<name> = <name>.boxed.toString(), ret.<name> = <name>?.boxed?.toString()
        fun element(name: MemberName, isNullable: Boolean, isCommonType: Boolean) {
            StringBuilder("$RET.${name.simpleName} = ${name.simpleName}").apply {
                commonTypeOrNot(isNullable, isCommonType)
            }.toString().also(builder::addStatement)
        }

        fun asIs(name: MemberName) {
            builder.addStatement("$RET.${name.simpleName} = ${name.simpleName}")
        }

        fun fromDsl(name: MemberName, str: String) {
            builder.addStatement("$RET.${name.simpleName} = ${name.simpleName}$str")
        }

        private fun StringBuilder.commonTypeOrNot(isNullable: Boolean, isCommonType: Boolean) {
            if (isNullable) append('?')
            append(".${KDType.Boxed.PARAM_NAME}")
            if (isCommonType) {
                if (isNullable) append('?')
                append(".toString()")
            }
        }

        fun build(): FunSpec =
            builder.addStatement("return $RET").build()

        private companion object {
            const val RET = "ret"
        }
    }

    private class FunSpecStatement(_init: Chunk?) {
        private val chunks = mutableListOf<Chunk>()
        init {
            _init?.also(chunks::add)
        }

        operator fun Chunk.unaryPlus() {
            chunks += this
        }

        operator fun plusAssign(other: FunSpecStatement) {
            chunks += other.chunks
        }

        fun endStatement() {
            +Chunk(",♢")
        }

        fun final() {
            +Chunk(")")
        }

        fun addTo(funBuilder: FunSpec.Builder) {
            funBuilder.addStatement(
                chunks.fold(StringBuilder()) { acc, chunk -> acc.apply { append(chunk.str) } }.toString(),
                *chunks.fold(mutableListOf<Any>()) { acc, chunk -> acc.apply { addAll(chunk.args) } }.toTypedArray()
            )
        }
    }

    private class Chunk(val str: String, vararg params: Any) {
        val args = params.toList()
    }

    public companion object {
        context(options: KDOptions, logger: KDLogger)
        public operator fun invoke(holder: KDType.Model, isDsl: Boolean): BuilderHolder =
            BuilderHolder(options, logger, holder, isDsl)
    }
}
