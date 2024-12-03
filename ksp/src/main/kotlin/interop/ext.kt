package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.ksp.interop.KDReference.Collection.CollectionType.*

internal fun KSClassDeclaration.toClassNameImpl(): ClassName =
    ClassName.bestGuess("${simpleName.asString()}Impl")

internal fun TypeName.toNullable(nullable: Boolean = true) =
    if (isNullable != nullable) copy(nullable = nullable) else this

internal fun KSClassDeclaration.toKDType(voType: KDValueObjectType): KDType {
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
            builder.addModifiers(KModifier.INTERNAL)
            builder.addSuperinterface(superType)

            when (voType) {
                KDValueObjectType.KDValueObject -> {
                    builder
                        .addModifiers(KModifier.DATA)
                        .addAnnotation(ConsistentCopyVisibility::class)

                    getAllProperties()
                        .map { KDParameter.create(implClassName.member(it.simpleName.asString()), it) }
                        .toList().also(builder::createConstructor)
                }

                is KDValueObjectType.KDValueObjectSingle -> {
                    builder.addModifiers(KModifier.VALUE)
                    builder.addAnnotation(JvmInline::class)

                    listOf(
                        KDParameter.create(
                            implClassName.member(KDValueObjectType.KDValueObjectSingle.PARAM_NAME),
                            voType.boxedType
                        )
                    ).apply {
                        /* value class constructor */
                        builder.createConstructor(this)

                        val valueParam = ParameterSpec.builder(
                            KDValueObjectType.KDValueObjectSingle.PARAM_NAME,
                            voType.boxedType
                        ).build()

                        /* ValueObjectSingle companion object */

                        FunSpec.builder(KDValueObjectType.KDValueObjectSingle.FABRIC_CREATE_METHOD)
                            .addModifiers(KModifier.INTERNAL)
                            .addParameter(valueParam)
                            .returns(superType)
                            .addStatement("return %T(%N)", implClassName, valueParam)
                            .build()
                            .let { func ->
                                TypeSpec.companionObjectBuilder()
                                    .addModifiers(KModifier.INTERNAL)
                                    .addFunction(func)
                                    .build()
                            }.also(builder::addType)

                        FunSpec.builder("toString")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(String::class)
                            .addStatement("return %N.toString()", valueParam)
                            .build()
                            .also(builder::addFunction)
                    }
                }

                else -> error("Illegal KDObjectValueType")
            }.let { KDType.create(implClassName, builder, it, voType) }
        }
    }
}

internal fun KDType.createImplBuilder(superTypeName: TypeName, logger: KSPLogger) {
    TypeSpec.classBuilder(KDType.BUILDER_CLASS_NAME).also { innerBuilderForBuilder ->
        FunSpec.builder(KDType.BUILDER_BUILD_METHOD).also { buildFunBuilder ->
            buildFunBuilder.addModifiers(KModifier.INTERNAL).returns(superTypeName)

            parameters.forEach { param ->
                buildFunBuilder.takeIf { param.typeReference is KDReference.Element && !param.typeReference.typeName.isNullable }
                    ?.addStatement("""requireNotNull(%N) { "Property '%T.%N' is not set!" }""", param.name, superTypeName, param.name)
                param.toBuilderPropertySpec(this).also(innerBuilderForBuilder::addProperty)
            }
            buildFunBuilder.addStatement("return %T(", className)
            parameters.forEach { param ->
                when(val ref = param.typeReference) {
                    is KDReference.Element -> {
                        val innerType = getNestedType(ref.typeName)
                        val receiver = ClassName.bestGuess("${innerType.className.simpleName}.Builder")

                        if (innerType.valueObjectType.isValueObject) {
                            buildFunBuilder.addStatement("\t%N = %N!!,", param.name, param.name)

                            // DSL builder: `fun <t>(block: <T>.Builder.() -> Unit) {}`

                            val blockParam = ParameterSpec.builder(
                                "block",
                                LambdaTypeName.get(receiver = receiver, returnType = Unit::class.asTypeName())
                            ).build()
                            FunSpec.builder(param.name.simpleName)
                                .addParameter(blockParam)
                                .addStatement("%N = %T().apply(%N).build()", param.name, receiver, blockParam)
                                .build()
                                .also(innerBuilderForBuilder::addFunction)
                        } else {
                            if (ref.typeName.isNullable)
                                buildFunBuilder.addStatement("\t%N = %N?.let(%T::create),", param.name, param.name, innerType.className)
                            else
                                buildFunBuilder.addStatement("\t%N = %T.create(%N!!),", param.name, innerType.className, param.name)
                        }
                    }
                    is KDReference.Collection -> {
                        data class Helper(val str: String)
                        val parametrizedKdTypes = ref.parameterizedTypeName.typeArguments
                            .map { getNestedType(it) to it.isNullable}

                        when(ref.collectionType) {
                            LIST, SET -> {
                                val (parametrizedKdType, isNullable) = parametrizedKdTypes.first()
                                if (parametrizedKdType.valueObjectType.isValueObject) {
                                    StringBuilder("\t%N = %N").apply {
                                        if (ref.collectionType == LIST) append(".toList()") else append(".toSet()")
                                    }.toString().also { buildFunBuilder.addStatement(it, param.name, param.name) }

                                    // DSL builder for collection
                                    val receiver = ClassName.bestGuess("${parametrizedKdType.className.simpleName}.${KDType.BUILDER_CLASS_NAME}")

                                    val blockParam = ParameterSpec.builder(
                                        "block",
                                        LambdaTypeName.get(receiver = receiver, returnType = Unit::class.asTypeName())
                                    ).build()
                                    FunSpec.builder(param.name.simpleName)
                                        .addParameter(blockParam)
                                        .addStatement("%T().apply(%N).build().also(%N::add)", receiver, blockParam, param.name)
                                        .build()
                                        .also(innerBuilderForBuilder::addFunction)

                                } else StringBuilder("\t%N = %N.map").apply {
                                    takeIf { isNullable }?.append(" { it?.let(%T::create) }")
                                        ?: append("(%T::create)")
                                    takeIf { ref.collectionType == SET }?.append(".toSet()")
                                    append(',')
                                }.toString().also { st ->
                                    buildFunBuilder.addStatement(
                                        st,
                                        param.name,
                                        param.name,
                                        parametrizedKdType.className
                                    )
                                }
                            }
                            MAP -> {

                                // TODO: when parametrized type is ValueObject

                                StringBuilder("\t%N = %N.entries.associate { ").apply {
                                    takeIf { parametrizedKdTypes[0].second }?.append("it.key?.let(%T::create) to ")
                                        ?: append("%T.create(it.key) to ")
                                    takeIf { parametrizedKdTypes[1].second }?.append("it.value?.let(%T::create)")
                                        ?: append("%T.create(it.value)")
                                    append(" },")
                                }.toString().also { st ->
                                    buildFunBuilder.addStatement(
                                        st,
                                        param.name,
                                        param.name,
                                        parametrizedKdTypes[0].first.className,
                                        parametrizedKdTypes[1].first.className
                                    )
                                }
                            }
                        }
                    }
                }
            }
            buildFunBuilder.addStatement(")")
        }.also { innerBuilderForBuilder.addFunction(it.build()) }
        innerBuilderForBuilder.build().also(builder::addType)
    }
}

private fun KDParameter.toBuilderPropertySpec(holder: KDType) =
    typeReference.let { ref ->
        when (ref) {
            is KDReference.Collection -> {
                ref.parameterizedTypeName.typeArguments.toMutableList().let { args ->
                    var convertToMutable = false
                    args.forEachIndexed { i, arg ->
                        holder.getNestedType(arg).also { nestedType ->
                            when(nestedType.valueObjectType) {
                                /* Collection<ValueObjectSingle<S>> -> TypeName[S] */
                                is KDValueObjectType.KDValueObjectSingle ->
                                    args[i] = nestedType.valueObjectType.boxedType.toNullable(arg.isNullable)

                                /* Collection<ValueObject> -> MutableCollection<ValueObject> */
                                KDValueObjectType.KDValueObject -> convertToMutable = true
                            }
                        }
                    }

                    if (convertToMutable) ref.parameterizedTypeName.rawType.toMutableCollection().parameterizedBy(args)
                        .let { PropertySpec.builder(name.simpleName, it) }
                        .initializer(ref.collectionType.mutableInitializer)
                    else ref.parameterizedTypeName.copy(typeArguments = args)
                        .let { PropertySpec.builder(name.simpleName, it) }
                        .initializer(ref.collectionType.initializer)
                }
            }

            is KDReference.Element ->
                holder.getNestedType(ref.typeName).let { innerType ->
                    /* ValueObjectSingle<S> -> TypeName[S] */
                    if (innerType.valueObjectType is KDValueObjectType.KDValueObjectSingle)
                        innerType.valueObjectType.boxedType
                    /* ValueObject -> TypeName[this] */
                    else ref.typeName
                }.let { PropertySpec.builder(name.simpleName, it.toNullable()).mutable().initializer("null") }
        }.build()
    }

private fun ClassName.toMutableCollection() = when(this) {
    com.squareup.kotlinpoet.LIST -> MUTABLE_LIST
    com.squareup.kotlinpoet.SET -> MUTABLE_SET
    com.squareup.kotlinpoet.MAP -> MUTABLE_MAP
    else -> error("Unsupported collection for mutable: $this")
}
