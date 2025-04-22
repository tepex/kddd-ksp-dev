package ru.it_arch.clean_ddd.ksp_model.model

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
}
