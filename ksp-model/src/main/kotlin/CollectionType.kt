package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET

internal enum class CollectionType(
    val classNames: List<ClassName>,
) {
    SET(listOf(com.squareup.kotlinpoet.SET, MUTABLE_SET)),
    LIST(listOf(com.squareup.kotlinpoet.LIST, MUTABLE_LIST)),
    MAP(listOf(com.squareup.kotlinpoet.MAP, MUTABLE_MAP));

    fun initializer(isMutable: Boolean): String = when(this) {
        MAP  -> if (isMutable) "mutableMapOf()" else "emptyMap()"
        LIST -> if (isMutable) "mutableListOf()" else "emptyList()"
        SET  -> if (isMutable) "mutableSetOf()" else "emptySet()"
    }

    fun getLambdaArgName(i: Int): String = when(this) {
        MAP  -> "it.key".takeIf { i == 0 } ?: "it.value"
        else -> "it"
    }

    fun mapperAsString(lambdaArgs: List<String>, isMutable: Boolean, isNoMap: Boolean, isNoTerminal: Boolean): String =
        ("".takeIf { isNoMap } ?: when (this) {
            MAP -> ".entries.associate { ${lambdaArgs[0]} to ${lambdaArgs[1]} }"
            else -> ".map { ${lambdaArgs.first()} }"
        }).let { "$it${toCounterpart(isMutable, isNoTerminal)}" }

    /** На последнем этапе не нужно имутабельное преобразование для list и map (если есть вложенность) */
    private fun toCounterpart(isMutable: Boolean, isNoTerminal: Boolean): String = when(this) {
        MAP  -> if (isMutable) ".toMutableMap()" else if (!isNoTerminal) ".toMap()" else ""
        LIST -> if (isMutable) ".toMutableList()" else if (!isNoTerminal) ".toList()" else ""
        SET  -> if (isMutable) ".toMutableSet()" else ".toSet()"
    }
}
