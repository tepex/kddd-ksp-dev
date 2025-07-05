package ru.it_arch.clean_ddd.app

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val json: Json = Json {
    prettyPrint = true
    namingStrategy = kotlinx.serialization.json.JsonNamingStrategy.KebabCase
}
