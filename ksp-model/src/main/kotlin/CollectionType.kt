package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import java.util.Locale

internal enum class CollectionType(
    val classNames: List<ClassName>,
) {
    SET(listOf(com.squareup.kotlinpoet.SET, MUTABLE_SET)),
    LIST(listOf(com.squareup.kotlinpoet.LIST, MUTABLE_LIST)),
    MAP(listOf(com.squareup.kotlinpoet.MAP, MUTABLE_MAP));

    val originName: String =
        name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    fun initializer(isMutable: Boolean): String =
        "mutable${originName}Of()".takeIf { isMutable } ?: "empty$originName()"

    fun getItArgName(i: Int): String = when(this) {
        MAP  -> "it.key".takeIf { i == 0 } ?: "it.value"
        else -> "it"
    }

    fun mapperAsString(lambdaArgs: List<String>, isMutable: Boolean, isNoMap: Boolean, isNoTerminal: Boolean): String =
        ("".takeIf { isNoMap } ?: when (this) {
            MAP -> ".entries.associate { ${lambdaArgs[0]} to ${lambdaArgs[1]} }"
            else -> ".map { ${lambdaArgs.first()} }"
        }).let { "$it${toCounterpart(isMutable, isNoTerminal)}" }

    /** На последнем этапе не нужно имутабельное преобразование для list и map (если есть вложенность) */
    private fun toCounterpart(isMutable: Boolean, isNoTerminal: Boolean): String =
        if (isMutable) ".toMutable${originName}()" else
            if (this == SET) ".toSet()" else ".to${originName}()".takeUnless { isNoTerminal } ?: ""
}
