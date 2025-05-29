package ru.it_arch.kddd.core.data

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import ru.it_arch.kddd.core.data.model.TypeHolder
import ru.it_arch.kddd.domain.KotlinCodeBoxedBuilder
import ru.it_arch.kddd.domain.model.type.ValueClassWithCommon
import ru.it_arch.kddd.domain.model.type.KdddType
import ru.it_arch.kddd.domain.shortName
import ru.it_arch.kddd.domain.templateParseBody
import ru.it_arch.kddd.ValueObject

internal class KotlinPoetBoxedBuilder(
    private val implClassBuilder: TypeSpec.Builder,
    private val typeHolder: TypeHolder,
    private val valueClass: KdddType.ValueClass
) : KotlinCodeBoxedBuilder {

    private val parameterSpec =
        ParameterSpec.builder(KdddType.ValueClass.PARAM_NAME, typeHolder.propertyTypes.values.first()).build()

    override fun generateImplementationClass() {
        implClassBuilder
            .addModifiers(KModifier.VALUE)
            .addAnnotation(JvmInline::class)
    }

    override fun generateProperty() {
        parameterSpec.let { PropertySpec.builder(it.name, it.type, KModifier.OVERRIDE) }
            //.initializer(parameterSpec.name)
            .build()
            .also(implClassBuilder::addProperty)
    }

    override fun generateConstructor() {
        KotlinPoetCommonBuilder.generateConstructor(listOf(parameterSpec))
            .also(implClassBuilder::primaryConstructor)
    }

    override fun generateToString() {
        FunSpec.builder("toString")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement(KdddType.ValueClass.TEMPLATE_TO_STRING_BODY, parameterSpec)
            .build()
            .also(implClassBuilder::addFunction)
    }

    override fun generateFork() {
        TypeVariableName(
            "T",
            ValueObject.Value::class.asTypeName().parameterizedBy(parameterSpec.type)
        ).also { tvn ->
            FunSpec.builder(KdddType.ValueClass.FORK_METHOD)
                .addTypeVariable(tvn)
                .addParameter(parameterSpec)
                .addModifiers(KModifier.OVERRIDE)
                .also(KotlinPoetCommonBuilder::addUncheckedCast)
                .returns(tvn)
                .addStatement(KdddType.ValueClass.TEMPLATE_FORK_BODY, valueClass.impl.className.shortName, parameterSpec, tvn)
                .build()
                .also(implClassBuilder::addFunction)
        }
    }

    override fun generateCompanion() {
        TypeSpec.companionObjectBuilder().also { companionBuilder ->
            FunSpec.builder("invoke")
                .addModifiers(KModifier.OPERATOR)
                .addParameter(parameterSpec)
                .returns(typeHolder.classType)
                .addStatement(KdddType.ValueClass.TEMPLATE_COMPANION_INVOKE_BODY, valueClass.impl.className.shortName, parameterSpec)
                .build()
                .also(companionBuilder::addFunction)

            // `public fun parse(src: String): <implClassName> { <body> }`
            if (valueClass is ValueClassWithCommon) {
                ParameterSpec.builder("src", String::class).build().also { srcParam ->
                    FunSpec.builder(ValueClassWithCommon.FABRIC_PARSE_METHOD)
                        .addParameter(srcParam)
                        .returns(ClassName.bestGuess(valueClass.impl.className.shortName))
                        .addStatement(
                            valueClass.templateParseBody,
                            parameterSpec.type,
                            srcParam,
                            valueClass.impl.className.shortName
                        )
                        .build()
                        .also(companionBuilder::addFunction)
                }
            }
        }.build().also(implClassBuilder::addType)
    }
}
