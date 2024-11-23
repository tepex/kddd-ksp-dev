package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver

internal fun ConstructorParameter.toParameterSpec() = ParameterSpec(
    name,
    if (isNullable) type.copy(nullable = true) else type
)

internal fun ConstructorParameter.toPropertySpec() = PropertySpec.builder(
    name,
    if (isNullable) type.copy(nullable = true) else type,
    KModifier.OVERRIDE
).initializer(name).build()

internal fun KSClassDeclaration.toValueObjectType(logger: KSPLogger): VO_TYPE? {
    superTypes.forEach { parent ->
        val ksType = parent.resolve()
        val fullName = ksType.declaration.let { "${it.packageName.asString()}.${it.simpleName.asString()}" }
        VO_TYPE.entries.find { it.className == fullName }
            ?.also {
                if (it == VO_TYPE.VALUE_OBJECT_SINGLE) {
                    logger.warn("-- super type: ${ksType}, ${parent.toTypeName()}")
                }
                return it
            }
    }
    return null
}

internal fun KSClassDeclaration.toClassNameImpl(): String =
    "${simpleName.asString()}Impl"

internal fun TypeSpec.Builder.createConstructor(parameters: List<ConstructorParameter>) {
    addProperties(parameters.map { it.toPropertySpec() })
    primaryConstructor(
        FunSpec.constructorBuilder()
            .addParameters(parameters.map { it.toParameterSpec() })
            .addStatement("validate()")
            .build()
    )
}

internal fun KSPropertyDeclaration.toConstructorParameter() =
    ConstructorParameter(
        simpleName.asString(),
        type.toTypeName(),
        type.resolve().isMarkedNullable
    )

internal fun KSClassDeclaration.toTypeSpecBuilder(logger: KSPLogger, voType: VO_TYPE): TypeSpec.Builder =
    TypeSpec.classBuilder(toClassNameImpl()).apply {
        val superType = asType(emptyList()).toTypeName()
        addModifiers(KModifier.INTERNAL)
        addSuperinterface(superType)

        when (voType) {
            VO_TYPE.VALUE_OBJECT -> {
                addModifiers(KModifier.DATA)
                getAllProperties().map { it.toConstructorParameter() }.toList().also(::createConstructor)
            }
            VO_TYPE.VALUE_OBJECT_SINGLE -> {
                addModifiers(KModifier.VALUE)
                addAnnotation(JvmInline::class)
                this@toTypeSpecBuilder.typeParameters.forEach { logger.warn("--- type param: $it") }

                FunSpec.builder("toString")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(String::class)
                    .addStatement("return value.toString()")
                    .build()
                    .also(::addFunction)

                createConstructor(listOf(ConstructorParameter("value", String::class.asTypeName())))
                FunSpec.builder(simpleName.asString().lowercase())
                    .addModifiers(KModifier.INTERNAL)
                    .addParameter("value", String::class)
                    .returns(superType)
                    .addStatement("return ${toClassNameImpl()}(value)")
                    .build()
                    .let { func ->
                        TypeSpec.companionObjectBuilder()
                            .addModifiers(KModifier.INTERNAL)
                            .addFunction(func)
                            .build()
                    }.also(::addType)
            }
        }

    }
