package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName

internal fun IKDParameter.toParameterSpec() = ParameterSpec(
    name,
    if (this is KDParameter && isNullable) type.copy(nullable = true) else type
)

internal fun IKDParameter.toPropertySpec() = PropertySpec.builder(
    name,
    if (this is KDParameter && isNullable) type.copy(nullable = true) else type,
    KModifier.OVERRIDE
).initializer(name).build()


internal fun KSClassDeclaration.asClassNameImplString(): String =
    "${simpleName.asString()}Impl"

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

internal fun KSPropertyDeclaration.toConstructorParameter() =
    KDParameter.create(simpleName.asString(), type.toTypeName(), type.resolve().isMarkedNullable)

internal fun KSClassDeclaration.toTypeSpecBuilder(voType: KDValueObjectType) =
    TypeSpec.classBuilder(asClassNameImplString()).let { builder ->
        val superType = asType(emptyList()).toTypeName()
        builder.addModifiers(KModifier.INTERNAL)
        builder.addSuperinterface(superType)

        when (voType) {
            KDValueObjectType.KDValueObject -> {
                builder.addModifiers(KModifier.DATA)
                getAllProperties().map { it.toConstructorParameter() }.toSet().also(builder::createConstructor)
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
                builder.createConstructor(setOf(KDParameterBase("value", voType.boxedType)))
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
                    }.also(builder::addType)
                //voType.boxedType
                emptySet()
            }
            else -> emptySet()
        }.let { TypeSpecWrapper(builder, it) }
    }
