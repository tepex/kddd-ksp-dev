package ru.it_arch.clean_ddd.ksp_model.utils

/**
 * Грязный хак для генерации аннотации @OptIn(ExperimentalSerializationApi::class).
 * Проблема в том, что штатным путем с помощью KotlinPoet ее не вставишь из-за ограничения использования. Написать:
 * ```
 * AnnotationSpec.builder(OptIn::class)
 * ```
 * не получится.
 *
 * После кодогенерации в результирующем файле
 * `import ru.it_arch.clean_ddd.ksp.model.utils.OptIn` подменяется на `import kotlinx.serialization.ExperimentalSerializationApi`
 * */
@Retention(AnnotationRetention.SOURCE)
public annotation class OptIn(val markerClass: String)
