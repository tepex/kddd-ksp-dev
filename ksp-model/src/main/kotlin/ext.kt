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

internal typealias KDTypeSearchResult = Pair<KDType, Boolean>

internal fun List<String>.createMapper(type: CollectionType, isMutable: Boolean, hasNotContainsBoxed: Boolean): String {
    val isNoTerminal =
        if (hasNotContainsBoxed) false
        else if (type == MAP) true
        else type != SET

    return ("".takeIf { hasNotContainsBoxed } ?: when (type) {
        MAP -> ".entries.associate { ${this[0]} to ${this[1]} }"
        else -> ".map { ${this.first()} }"
    }).let { "$it${type.toCounterpart(isMutable, !isNoTerminal)}" }
}

private fun CollectionType.toCounterpart(isMutable: Boolean, hasToImmutableTermination: Boolean): String =
    ".toMutable${originName}()".takeIf { isMutable }
        ?: ".toSet()".takeIf { this == SET }
        ?: ".to${originName}()".takeIf { hasToImmutableTermination }
        ?: ""
