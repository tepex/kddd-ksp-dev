package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import ru.it_arch.clean_ddd.ksp.model.CollectionType.MAP
import ru.it_arch.clean_ddd.ksp.model.CollectionType.SET
import ru.it_arch.clean_ddd.ksp.model.KDType.Boxed.Companion.FABRIC_CREATE_METHOD
import ru.it_arch.clean_ddd.ksp.model.KDType.Boxed.Companion.FABRIC_PARSE_METHOD
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
    return rawTypeName.toString().substringAfterLast('.')
}

internal fun KDType.Boxed.asDeserialize(isInner: Boolean): String =
        (FABRIC_PARSE_METHOD.takeIf { isParsable } ?: FABRIC_CREATE_METHOD).let { ".let($classNameRef::$it)" }
//    else "xxx"//TODO()
// .let(CommonTypesImpl.MyUUIDImpl::parse)

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

internal fun CollectionType.initializer(isMutable: Boolean): String =
    "mutable${originName}Of()".takeIf { isMutable } ?: "empty$originName()"

internal fun CollectionType.getItArgName(i: Int): String = when(this) {
    MAP  -> "it.key".takeIf { i == 0 } ?: "it.value"
    else -> "it"
}
