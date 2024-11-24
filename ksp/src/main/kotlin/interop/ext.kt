package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName

internal fun ConstructorParameter.toParameterSpec() = ParameterSpec(
    name,
    if (isNullable) type.copy(nullable = true) else type
)

internal fun ConstructorParameter.toPropertySpec(isInnerBuilder: Boolean = false) = PropertySpec.builder(
    name,
    if (isNullable) type.copy(nullable = true) else type,
    KModifier.OVERRIDE
).initializer(name).build()

internal fun KSClassDeclaration.toValueObjectType(logger: KSPLogger): ValueObjectType? {
    superTypes.forEach { parent ->
        val fullName = parent.resolve().declaration.let { "${it.packageName.asString()}.${it.simpleName.asString()}" }
        when(fullName) {
            ValueObjectType.ValueObject.CLASSNAME -> ValueObjectType.ValueObject
            ValueObjectType.ValueObjectSingle.CLASSNAME ->
                runCatching { ValueObjectType.ValueObjectSingle.create(parent.toTypeName().toString()) }.getOrElse {
                    logger.warn(it.message ?: "Cant parse parent type $parent")
                    null
                }
            else -> null
        }?.also { return it }
    }
    return null
}

internal fun KSClassDeclaration.asClassNameImplString(): String =
    "${simpleName.asString()}Impl"

internal fun TypeSpec.Builder.createConstructor(parameters: List<ConstructorParameter>) {
    addProperties(parameters.map { it.toPropertySpec() })
    primaryConstructor(
        FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addParameters(parameters.map { it.toParameterSpec() })
            .addStatement("validate()")
            .build()
    )
}

internal fun KSPropertyDeclaration.toConstructorParameter() =
    ConstructorParameter(
        simpleName.asString(),
        type.toTypeName(),
        null,
        type.resolve().isMarkedNullable
    )

internal fun KSClassDeclaration.toTypeSpecBuilder(logger: KSPLogger, voType: ValueObjectType): TypeSpec.Builder =
    TypeSpec.classBuilder(asClassNameImplString()).apply {
        val superType = asType(emptyList()).toTypeName()
        addModifiers(KModifier.INTERNAL)
        addSuperinterface(superType)

        when (voType) {
            ValueObjectType.ValueObject -> {
                addModifiers(KModifier.DATA)
                getAllProperties().map { it.toConstructorParameter() }.toList().also(::createConstructor)

                TypeSpec.classBuilder("Builder").also { innerBuilder ->

                }
            }
            is ValueObjectType.ValueObjectSingle -> {
                addModifiers(KModifier.VALUE)
                addAnnotation(JvmInline::class)

                FunSpec.builder("toString")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(String::class)
                    .addStatement("return value.toString()")
                    .build()
                    .also(::addFunction)

                // value class constructor
                createConstructor(listOf(ConstructorParameter("value", voType.boxedType, null)))
                FunSpec.builder(simpleName.asString().replaceFirstChar { it.lowercase() })
                    .addModifiers(KModifier.INTERNAL)
                    .addParameter("value", voType.boxedType)
                    .returns(superType)
                    .addStatement("return ${asClassNameImplString()}(value)")
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
