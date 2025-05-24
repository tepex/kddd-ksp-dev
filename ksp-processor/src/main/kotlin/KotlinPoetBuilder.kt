package ru.it_arch.clean_ddd.ksp

import ru.it_arch.clean_ddd.domain.KotlinCodeBoxedBuilder
import ru.it_arch.clean_ddd.domain.KotlinCodeBuilder
import ru.it_arch.clean_ddd.domain.KotlinCodeDataBuilder
import ru.it_arch.clean_ddd.domain.KotlinCodeEntityBuilder

internal class KotlinPoetBuilder(
    private val boxedBuilder: KotlinCodeBoxedBuilder,
    private val dataBuilder: KotlinCodeDataBuilder,
    private val entityBuilder: KotlinCodeEntityBuilder
) : KotlinCodeBoxedBuilder by boxedBuilder,
    KotlinCodeDataBuilder by dataBuilder,
    KotlinCodeEntityBuilder by entityBuilder,
    KotlinCodeBuilder
