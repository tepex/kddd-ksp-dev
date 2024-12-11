package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.ddd.ValueObjectSingle

internal fun KSClassDeclaration.toClassNameImpl(): ClassName =
    ClassName.bestGuess("${simpleName.asString()}Impl")

internal fun TypeName.toNullable(nullable: Boolean = true) =
    if (isNullable != nullable) copy(nullable = nullable) else this

internal fun KSClassDeclaration.toKDType(voType: KDValueObjectType, logger: KSPLogger): KDType {
    fun TypeSpec.Builder.createConstructor(parameters: List<KDParameter>) {
        parameters.map { param ->
            PropertySpec
                .builder(param.name.simpleName, param.typeReference.typeName, KModifier.OVERRIDE)
                .initializer(param.name.simpleName)
                .build()
        }.also(::addProperties)

        FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addParameters(parameters.map { ParameterSpec(it.name.simpleName, it.typeReference.typeName) })
            .addStatement("validate()")
            .build()
            .also(::primaryConstructor)
    }

    return toClassNameImpl().let { implClassName ->
        TypeSpec.classBuilder(implClassName).let { builder ->
            val superType = asType(emptyList()).toTypeName()
            builder.addSuperinterface(superType)

            when (voType) {
                KDValueObjectType.KDValueObject -> {
                    builder
                        .addModifiers(KModifier.DATA)
                        .addAnnotation(ConsistentCopyVisibility::class)
                        //.addAnnotation(ExposedCopyVisibility::class)

                    getAllProperties()
                        .map { KDParameter.create(implClassName.member(it.simpleName.asString()), it) }
                        .toList().also(builder::createConstructor)

                    /* fun toBuilder() */
                }

                is KDValueObjectType.KDValueObjectSingle -> {
                    //builder.addModifiers(KModifier.PRIVATE)
                    builder.addModifiers(KModifier.VALUE)
                    builder.addAnnotation(JvmInline::class)

                    listOf(
                        KDParameter.create(implClassName.member(KDValueObjectType.KDValueObjectSingle.PARAM_NAME), voType.boxedType)
                    ).apply {
                        /* value class constructor */
                        builder.createConstructor(this)

                        val valueParam = ParameterSpec.builder(KDValueObjectType.KDValueObjectSingle.PARAM_NAME, voType.boxedType).build()
                        FunSpec.builder("toString")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(String::class)
                            .addStatement("return %N.toString()", valueParam)
                            .build()
                            .also(builder::addFunction)

                        /*  override fun <T : ValueObjectSingle<String>> copy(value: String): T = NameImpl(value) as T */

                        TypeVariableName("T", ValueObjectSingle::class.asTypeName().parameterizedBy(voType.boxedType))
                            .also { tvn ->
                                FunSpec.builder(KDValueObjectType.KDValueObjectSingle.CREATE_METHOD)
                                    .addTypeVariable(tvn)
                                    .addParameter(valueParam)
                                    .addModifiers(KModifier.OVERRIDE)
                                    .addUncheckedCast()
                                    .returns(tvn)
                                    .addStatement("return %T(%N) as %T", implClassName, valueParam, tvn)
                                    .build()
                                    .also(builder::addFunction)
                            }

                        /* ValueObjectSingle companion object */

                        FunSpec.builder(KDValueObjectType.KDValueObjectSingle.FABRIC_CREATE_METHOD)
                            .addParameter(valueParam)
                            .returns(implClassName)
                            .addStatement("return %T(%N)", implClassName, valueParam)
                            .build()
                            .let { TypeSpec.companionObjectBuilder().addFunction(it).build() }
                            .also(builder::addType)
                    }
                }

                else -> error("Illegal KDObjectValueType")
            }.let { KDType.create(implClassName, builder, it, voType) }
        }
    }
}

internal fun FunSpec.Builder.addUncheckedCast(): FunSpec.Builder =
    addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("\"UNCHECKED_CAST\"").build())
