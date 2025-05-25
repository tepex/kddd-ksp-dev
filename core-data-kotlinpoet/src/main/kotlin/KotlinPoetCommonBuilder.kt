package ru.it_arch.clean_ddd.core.data

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec

internal object KotlinPoetCommonBuilder {

    fun generateConstructor(parametersSpec: List<ParameterSpec>): FunSpec =
        FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addParameters(parametersSpec)
            .addStatement("validate()")
            .build()

    /**
     * ```
     * @Suppress("UNCHECKED_CAST")
     * ```
     * */
    fun addUncheckedCast(funSpec: FunSpec.Builder): FunSpec.Builder =
        funSpec.addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("\"UNCHECKED_CAST\"").build())
}
