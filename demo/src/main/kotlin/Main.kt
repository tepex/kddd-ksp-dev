package ru.it_arch.clean_ddd.app

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    prettyPrint = true
    namingStrategy = JsonNamingStrategy.KebabCase
}

fun main() {
    testPrimitives()
    testNullables()
    testCommonTypes()
    testWithInner()
    testCollections()

    testEntity()
}
