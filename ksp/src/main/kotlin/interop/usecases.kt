package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
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
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.ksp.interop.KDReference.Collection.CollectionType.*
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
                        //.addAnnotation(ConsistentCopyVisibility::class)
                        .addAnnotation(ExposedCopyVisibility::class)

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
                                FunSpec.builder("copy")
                                    .addTypeVariable(tvn)
                                    .addParameter(valueParam)
                                    .addModifiers(KModifier.OVERRIDE)
                                    .addAnnotation(
                                        AnnotationSpec.builder(Suppress::class).addMember("\"UNCHECKED_CAST\"").build()
                                    )
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

internal fun KDType.createImplBuilder(holderTypeName: TypeName, logger: KSPLogger) {

    /** BOXED or holderTypeName */
    fun createDslBuilderParameter(name: String, nestedType: KDType) = when(nestedType.valueObjectType) {
        is KDValueObjectType.KDValueObjectSingle ->
            ParameterSpec.builder(name, nestedType.valueObjectType.boxedType).build()
        else -> ParameterSpec.builder(
            name,
            LambdaTypeName.get(receiver = nestedType.builderClassName, returnType = Unit::class.asTypeName())
        ).build()
    }

    /** DSL builder: `fun <t>(block: <T>Impl.Builder.() -> Unit) { ... }` */
    fun createDslBuilder(param: KDParameter, nestedType: KDType, isCollection: Boolean) =
        createDslBuilderParameter("p1", nestedType).let { blockParam ->
            FunSpec.builder(param.name.simpleName).apply {
                addParameter(blockParam)
                if (isCollection)
                    addStatement("%T().apply(%N).${KDType.BUILDER_BUILD_METHOD}().also(%N::add)", nestedType.builderClassName, blockParam, param.name)
                else
                    addStatement("%N = %T().apply(%N).${KDType.BUILDER_BUILD_METHOD}()", param.name, nestedType.builderClassName, blockParam)
            }.build()
        }

    /** DSL builder: `fun <t>(boxed: BOXED, block: <T>Impl.Builder.() -> Unit) { ... }`*/
    /*
public fun myMap(name: String, block: InnerStructImpl.Builder.() -> Unit) {
    myMap[NameImpl.create(name)] = InnerStructImpl.Builder().apply(block).build()
}*/

    fun createDslBuilder1(param: KDParameter, nestedType1: KDType, nestedType2: KDType) =
        FunSpec.builder(param.name.simpleName).apply {
            val p1 = createDslBuilderParameter("p1", nestedType1)
            val p2 = createDslBuilderParameter("p2", nestedType2)
            addParameter(p1)
            addParameter(p2)
        }.build()


    /*
    val toBuilderReturnCodeBlock = CodeBlock.builder().add("return %T().also {", kdBuilderClass)

    toBuilderReturnCodeBlock.add("}")
    toBuilderReturnCodeBlock.add(toBuilderReturnCodeBlock.build())*/

    val toBuilderFunBuilder = FunSpec.builder("toBuilder")
        .returns(builderClassName)
        .addStatement("val ret = %T()", builderClassName)
        //.addStatement("return %T().also {}", kdBuilderClass)
        .addStatement("ret.name = name.value")

    TypeSpec.classBuilder(builderClassName).also { builderForKDBuilderClass ->
        FunSpec.builder(KDType.BUILDER_BUILD_METHOD).also { builderForKDBuildFun ->
            builderForKDBuildFun.returns(holderTypeName)

            parameters.forEach { param ->
                builderForKDBuildFun.takeIf { param.typeReference is KDReference.Element && !param.typeReference.typeName.isNullable }
                    ?.addStatement("""requireNotNull(%N) { "Property '%T.%N' is not set!" }""", param.name, holderTypeName, param.name)
                param.toBuilderPropertySpec(this).also(builderForKDBuilderClass::addProperty)
            }

            builderForKDBuildFun.addStatement("return %T(", className)
            parameters.forEach { param ->
                when(param.typeReference) {
                    is KDReference.Element -> {
                        getNestedType(param.typeReference.typeName).also { nestedType ->
                            if (nestedType.valueObjectType.isValueObject) {
                                createDslBuilder(param, nestedType, false).also(builderForKDBuilderClass::addFunction)
                                builderForKDBuildFun.addStatement(
                                    "\t%N = %N${if (!param.typeReference.typeName.isNullable) "!!" else ""},",
                                    param.name,
                                    param.name
                                )
                            } else {
                                if (param.typeReference.typeName.isNullable)
                                    builderForKDBuildFun.addStatement("\t%N = %N?.let(%T::create),", param.name, param.name, nestedType.className)
                                else
                                    builderForKDBuildFun.addStatement("\t%N = %T.create(%N!!),", param.name, nestedType.className, param.name)
                            }
                        }
                    }
                    is KDReference.Collection -> {
                        val parametrizedKdTypes = param.typeReference.parameterizedTypeName.typeArguments
                            .map { getNestedType(it) to it.isNullable }

                        when(param.typeReference.collectionType) {
                            LIST, SET -> parametrizedKdTypes.first().also { (kdType, isNullable) ->
                                StringBuilder("\t%N = %N").apply {
                                    takeIf { param.typeReference.collectionType == LIST }?.append(".toList(),") ?: append(".toSet(),")
                                }.toString().takeIf { kdType.valueObjectType.isValueObject }?.also { str ->
                                    builderForKDBuildFun.addStatement(str, param.name, param.name)
                                    createDslBuilder(param, kdType,true).also(builderForKDBuilderClass::addFunction)
                                } ?: StringBuilder("\t%N = %N.map").apply {
                                    takeIf { isNullable }?.append(" { it?.let(%T::create) }") ?: append("(%T::create)")
                                    takeIf { param.typeReference.collectionType == SET }?.append(".toSet()")
                                    append(',')
                                }.toString()
                                    .also { builderForKDBuildFun.addStatement(it, param.name, param.name, kdType.className) }
                            }

                            MAP -> {
                                // TODO: when parametrized type is ValueObject

                                parametrizedKdTypes.find { it.first.valueObjectType.isValueObject }?.also {
                                    StringBuilder("\t%N = %N.toMap(),").apply {
                                        createDslBuilder1(param, parametrizedKdTypes[0].first, parametrizedKdTypes[1].first)
                                            .also(builderForKDBuilderClass::addFunction)
                                    }.toString()
                                        .also { builderForKDBuildFun.addStatement(it, param.name, param.name) }
                                } ?: StringBuilder("\t%N = %N.entries.associate { ").apply {
                                    takeIf { parametrizedKdTypes[0].second }?.append("it.key?.let(%T::create) to ")
                                        ?: append("%T.create(it.key) to ")
                                    takeIf { parametrizedKdTypes[1].second }?.append("it.value?.let(%T::create)")
                                        ?: append("%T.create(it.value)")
                                    append(" },")
                                }.toString()
                                    .also { builderForKDBuildFun.addStatement(it, param.name, param.name, parametrizedKdTypes[0].first.className, parametrizedKdTypes[1].first.className) }
                            }
                        }
                    }
                }
            }
            builderForKDBuildFun.addStatement(")")
        }.also { builderForKDBuilderClass.addFunction(it.build()) }

        builderForKDBuilderClass.build().also(builder::addType)

        toBuilderFunBuilder.addStatement("return ret")
        builder.addFunction(toBuilderFunBuilder.build())
    }
}

/**
 * KDType{ValueObjectSingle<BOXED>} -> BOXED
 * KDType{ValueObject} -> ValueObject
 *
 * List<KDType{ValueObjectSingle<BOXED>}>, Set<KDType{ValueObjectSingle<BOXED>}> -> List<BOXED>, Set<BOXED>
 * Map<KDType{ValueObjectSingle<BOXED1>}, KDType{ValueObjectSingle<BOXED2>}> -> Map<BOXED1, BOXED2>
 *
 * List<KDType{ValueObject}>, Set<KDType{ValueObject}>        -> MutableList<ValueObject>, MutableSet<ValueObject>
 * Map<KDType{ValueObject}, KDType{ValueObject}>              -> MutableMap<ValueObject, ValueObject>
 * Map<KDType{ValueObject}, KDType{ValueObjectSingle<BOXED>}> -> MutableMap<ValueObject, ValueObjectSingle<BOXED>>
 * Map<KDType{ValueObjectSingle<BOXED>, KDType{ValueObject}>  -> MutableMap<ValueObjectSingle<BOXED>, ValueObject>
 * */
private fun KDParameter.toBuilderPropertySpec(holder: KDType) = typeReference.let { ref ->
    when(ref) {
        is KDReference.Element -> holder.getNestedType(ref.typeName).let { innerType ->
            if (innerType.valueObjectType is KDValueObjectType.KDValueObjectSingle) innerType.valueObjectType.boxedType
            else ref.typeName
        }.let { PropertySpec.builder(name.simpleName, it.toNullable()).mutable().initializer("null") }

        is KDReference.Collection -> ref.parameterizedTypeName.typeArguments
            .takeIf { args -> args.any { holder.getNestedType(it).valueObjectType.isValueObject } }?.let { args ->
                // ValueObject -> ValueObject
                ref.parameterizedTypeName.rawType
                    .toMutableCollection()
                    .parameterizedBy(args)
                    .let { PropertySpec.builder(name.simpleName, it).initializer(ref.collectionType.mutableInitializer) }
            } ?: run {
                ref.parameterizedTypeName.typeArguments.map { typeName ->
                    when(val voType = holder.getNestedType(typeName).valueObjectType) {
                        // ValueObjectSingle<BOXED> -> BOXED
                        is KDValueObjectType.KDValueObjectSingle -> voType.boxedType.toNullable(typeName.isNullable)
                        else -> typeName
                    }
                }.let { args ->
                    ref.parameterizedTypeName.copy(typeArguments = args)
                        .let { PropertySpec.builder(name.simpleName, it).mutable().initializer(ref.collectionType.initializer) }
                }
            }
    }.build()
}

private fun ClassName.toMutableCollection() = when(this) {
    com.squareup.kotlinpoet.LIST -> MUTABLE_LIST
    com.squareup.kotlinpoet.SET -> MUTABLE_SET
    com.squareup.kotlinpoet.MAP -> MUTABLE_MAP
    else -> error("Unsupported collection for mutable: $this")
}
