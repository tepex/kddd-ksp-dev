// TODO: разнести эту свалку на утилиты и use cases

package ru.it_arch.clean_ddd.ksp_model

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import ru.it_arch.clean_ddd.ksp_model.model.CollectionType.MAP
import ru.it_arch.clean_ddd.ksp_model.model.CollectionType.SET
import ru.it_arch.clean_ddd.ksp_model.model.KDType.Boxed.Companion.FABRIC_PARSE_METHOD
import ru.it_arch.clean_ddd.ksp_model.model.CollectionType
import ru.it_arch.clean_ddd.ksp_model.model.KDOptions
import ru.it_arch.clean_ddd.ksp_model.model.KDProperty
import ru.it_arch.clean_ddd.ksp_model.model.KDType
import ru.it_arch.clean_ddd.ksp_model.model.PackageName
import ru.it_arch.kddd.Kddd
import java.util.Locale

internal fun TypeName.toNullable(nullable: Boolean = true) =
    if (isNullable != nullable) copy(nullable = nullable) else this

internal fun FunSpec.Builder.addUncheckedCast(): FunSpec.Builder =
    addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("\"UNCHECKED_CAST\"").build())

internal fun ParameterizedTypeName.toCollectionType(): CollectionType =
    CollectionType.entries.find { it.classNames.contains(rawType) }
        ?: error("Not supported collection type $this")

internal val KDType.Boxed.isPrimitive: Boolean get() =
    boxedType == STRING ||
        boxedType == BOOLEAN ||
        boxedType == BYTE ||
        boxedType == CHAR ||
        boxedType == FLOAT ||
        boxedType == DOUBLE ||
        boxedType == INT ||
        boxedType == LONG ||
        boxedType == SHORT

internal val KDType.Boxed.isString: Boolean get() =
    boxedType == STRING

internal fun KDType.Boxed.asSimplePrimitive(): String {
    check(isPrimitive)
    return rawType.toString().substringAfterLast('.')
}

internal fun KDType.Boxed.asDeserialize(isInner: Boolean): String =
    if (isParsable) ".let($classNameRef::$FABRIC_PARSE_METHOD)" else ".let { $classNameRef(it) }"
        //(FABRIC_PARSE_METHOD.takeIf { isParsable } ?: FABRIC_CREATE_METHOD).let { ".let($classNameRef::$it)" }
//    else "xxx"//TODO()
// .let(CommonTypesImpl.MyUUIDImpl::parse)

/**
 * Результат метода [KDType.Generatable.getKDType].
 *
 * first — найденный [KDType]
 * second — true: вложенный тип
 * */
internal typealias KDTypeSearchResult = Pair<KDType, Boolean>

internal fun List<String>.createMapper(type: CollectionType, isMutable: Boolean, hasNotContainsBoxed: Boolean): String = (
    ".toMutable${type.originName}()".takeIf { isMutable }
        ?: ".toSet()".takeIf { type == SET }
        ?: ".to${type.originName}()".takeIf { hasNotContainsBoxed || type == MAP } ?: ""
    ).let { term -> (
        "".takeIf { hasNotContainsBoxed }
            ?: ".map { ${this.first()} }".takeUnless { type == MAP }
            ?: ".entries.associate { ${this[0]} to ${this[1]} }"
        ).let { "$it$term" }
    }

internal val CollectionType.originName: String
    get() = name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

internal infix fun CollectionType.initializer(isMutable: Boolean): String =
    "mutable${originName}Of()".takeIf { isMutable } ?: "empty$originName()"

internal infix fun CollectionType.getItArgNameForIndex(i: Int): KDProperty.Name = when(this) {
    MAP  -> KDProperty.Name("it.key".takeIf { i == 0 } ?: "it.value")
    else -> KDProperty.Name("it")
}

internal fun TypeName.isCollection(): Boolean =
    this is ParameterizedTypeName

internal fun TypeName.toParametrizedType(): ParameterizedTypeName = when(this) {
    is ParameterizedTypeName -> this
    else -> error("Expected ParameterizedTypeName")
}

public typealias TypeCatalog = Set<KDType>

public val TypeName.simpleName: String
    get() = toString().substringAfterLast('.')

/**
 * Имя фабричной функции DSL-билдера.
 *
 * Пример для типа `MyType`:
 * ```
 * fun myType(block: MyTypeImpl.DslBuilder.() -> Unit): MyType
 * ```
 * */
internal val TypeName.dslBuilderName: String
    get() = simpleName.replaceFirstChar { it.lowercaseChar() }

/**
 * Создание DSL функции-билдера.
 *
 * Создается для вложенных [Kddd]-моделей типа [KDType.Model] в классе `DslBuilder` и для корневых [Kddd]-моделей в файле для расширений.
 *
 * Пример:
 * ```
 * fun myType(block: MyTypeImpl.DslBuilder.() -> Unit): MyType =
 *     MyTypeImpl.DslBuilder().apply(block).build()
 * ```
 *
 * @receiver модель, для которой создается функция-билдер.
 * @param useContextParameters параметр опции [KDOptions.UseContextParameters].
 * @return декларация функции [FunSpec].
 * */
@OptIn(ExperimentalKotlinPoetApi::class)
public infix fun KDType.Model.createDslBuilderFun(useContextParameters: KDOptions.UseContextParameters): FunSpec =
    FunSpec.builder(name.dslBuilderName).apply {
        ParameterSpec.builder(
            "block",
            if (useContextParameters.boxed) LambdaTypeName.get(
                contextReceivers = listOf(innerDslBuilderClassName),
                returnType = Unit::class.asTypeName()
            ) else LambdaTypeName.get(
                receiver = innerDslBuilderClassName,
                returnType = Unit::class.asTypeName()
            )
        ).build().also { param ->
            addParameter(param)
            addStatement("return ${KDType.Data.APPLY_BUILDER}", innerDslBuilderClassName, param)
        }
        returns(name)
    }.build()

public infix fun KDOptions.toImplementationClassName(kDddType: TypeName): String {
    var result = generatedClassNameResult.boxed
    generatedClassNameRe.find(kDddType.simpleName)?.groupValues?.forEachIndexed { i, group ->
        group.takeIf { i > 0 }?.also { result = result.replace("\$$i", it) }
    }
    return result
}
