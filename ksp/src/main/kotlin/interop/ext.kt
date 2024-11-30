package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName

internal fun IKDParameter.toParameterSpec() =
    ParameterSpec(name.value, kdType.type)

internal fun IKDParameter.toPropertySpec() =
    PropertySpec.builder(name.value, kdType.type, KModifier.OVERRIDE).initializer(name.value).build()

internal fun IKDParameter.toBuilderPropertySpec(replacements: Map<WrapperType, BoxedType>) =
    kdType.let { parameterType ->
        when(parameterType) {
            is IKDParameter.KDType.Collection -> {
                parameterType.typeName.typeArguments.toMutableList().let { args ->
                    args.forEachIndexed { i, arg ->
                        replacements.getBoxedType(arg)?.also { args[i] = it.toNullable(arg.isNullable) }
                    }
                    PropertySpec.builder(name.value, parameterType.typeName.copy(typeArguments = args))
                        .initializer(parameterType.collectionType.initializer)
                }
            }
            is IKDParameter.KDType.Element ->
                (replacements.getBoxedType(parameterType.typeName) ?: parameterType.typeName)
                    .let { PropertySpec.builder(name.value, it.toNullable()).initializer("null") }
        }.mutable().build()
    }

internal typealias WrapperType = TypeName
internal typealias BoxedType = TypeName

internal fun Map<WrapperType, BoxedType>.getBoxedType(key: WrapperType): BoxedType? =
    this[key.toNullable(false)]

internal fun KSClassDeclaration.asClassNameImpl(): ClassName =
    ClassName.bestGuess("${simpleName.asString()}Impl")

internal fun TypeName.toNullable(nullable: Boolean = true) =
    if (isNullable != nullable) copy(nullable = nullable) else this


internal fun TypeSpec.Builder.createConstructor(parameters: Set<IKDParameter>) {
    addProperties(parameters.map { it.toPropertySpec() })
    primaryConstructor(
        FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addParameters(parameters.map { it.toParameterSpec() })
            .addStatement("validate()")
            .build()
    )
}

internal fun KSClassDeclaration.toTypeSpecBuilder(logger: KSPLogger, voType: KDValueObjectType) =
    asClassNameImpl().let { className ->
        TypeSpec.classBuilder(className).let { builder ->
            val superType = asType(emptyList()).toTypeName()
            builder.addModifiers(KModifier.INTERNAL)
            builder.addSuperinterface(superType)

            when (voType) {
                KDValueObjectType.KDValueObject -> {
                    builder.addModifiers(KModifier.DATA)
                    getAllProperties().map {
                        val typeName = it.type.toTypeName()
                        logger.warn(">>> to KDParameter: `${it.simpleName.asString()}`: $typeName ${typeName::class.simpleName}")
                        KDParameter.create(it)
                    }.toSet().also(builder::createConstructor)
                }

                is KDValueObjectType.KDValueObjectSingle -> {
                    builder.addModifiers(KModifier.VALUE)
                    builder.addAnnotation(JvmInline::class)

                    FunSpec.builder("toString")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class)
                        .addStatement("return value.toString()")
                        .build()
                        .also(builder::addFunction)

                    // value class constructor
                    builder.createConstructor(setOf(KDParameter.create("value", voType.boxedType)))
                    val valueParam = ParameterSpec.builder("value", voType.boxedType).build()
                    FunSpec.builder(simpleName.asString().replaceFirstChar { it.lowercase() })
                        .addModifiers(KModifier.INTERNAL)
                        .addParameter(valueParam)
                        .returns(superType)
                        .addStatement("return %T(%N)", className, valueParam)
                        .build()
                        .let { func ->
                            TypeSpec.companionObjectBuilder()
                                .addModifiers(KModifier.INTERNAL)
                                .addFunction(func)
                                .build()
                        }.also(builder::addType)
                    emptySet()
                }

                else -> emptySet()
            }.let { TypeSpecWrapper(className, builder, it, voType) }
        }
    }
