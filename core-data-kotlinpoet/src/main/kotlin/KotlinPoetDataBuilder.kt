package ru.it_arch.clean_ddd.core.data

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import ru.it_arch.clean_ddd.core.data.model.TypeHolder
import ru.it_arch.clean_ddd.domain.KotlinCodeDataBuilder
import ru.it_arch.clean_ddd.domain.model.kddd.Data

internal class KotlinPoetDataBuilder(
    private val implClassBuilder: TypeSpec.Builder,
    private val typeHolder: TypeHolder,
    private val data: Data
) : KotlinCodeDataBuilder {

    private val parametersSpec =
        typeHolder.propertyTypes.entries.map { ParameterSpec(it.key.boxed, it.value) }

    override fun generateImplementationClass() {
        implClassBuilder
            .addModifiers(KModifier.DATA)
            .addAnnotation(ConsistentCopyVisibility::class)
    }

    override fun generateProperties() {
        parametersSpec.map { param ->
            PropertySpec.builder(param.name, param.type, KModifier.OVERRIDE)
                //.initializer(param.name)
                .build()
        }.also(implClassBuilder::addProperties)
    }

    override fun generateConstructor() {
        KotlinPoetCommonBuilder.generateConstructor(parametersSpec)
            .also(implClassBuilder::primaryConstructor)
    }

    override fun generateFork() {

    }
}
