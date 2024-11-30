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

internal fun KDParameter.toParameterSpec() =
    ParameterSpec(name.value, typeReference.typeName)

internal fun KDParameter.toPropertySpec() =
    PropertySpec.builder(name.value, typeReference.typeName, KModifier.OVERRIDE).initializer(name.value).build()

internal fun KDParameter.toBuilderPropertySpec(replacements: Map<WrapperType, BoxedType>) =
    typeReference.let { kdReference ->
        when(kdReference) {
            is KDReference.Collection -> {
                kdReference.parameterizedTypeName.typeArguments.toMutableList().let { args ->
                    args.forEachIndexed { i, arg ->
                        replacements.getBoxedType(arg)?.also { args[i] = it.toNullable(arg.isNullable) }
                    }
                    PropertySpec.builder(name.value, kdReference.parameterizedTypeName.copy(typeArguments = args))
                        .initializer(kdReference.collectionType.initializer)
                }
            }
            is KDReference.Element ->
                (replacements.getBoxedType(kdReference.typeName) ?: kdReference.typeName)
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


internal fun TypeSpec.Builder.createConstructor(parameters: Set<KDParameter>) {
    addProperties(parameters.map { it.toPropertySpec() })
    primaryConstructor(
        FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addParameters(parameters.map { it.toParameterSpec() })
            .addStatement("validate()")
            .build()
    )
}

internal fun KDType.toTypeSpec() =
    builder.build()

internal fun KSClassDeclaration.toKDType(logger: KSPLogger, voType: KDValueObjectType) =
    asClassNameImpl().let { implClassName ->
        TypeSpec.classBuilder(implClassName).let { builder ->
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

                    val parameters = setOf(KDParameter.create("value", voType.boxedType))
                    // value class constructor
                    builder.createConstructor(parameters)

                    val valueParam = ParameterSpec.builder("value", voType.boxedType).build()
                    FunSpec.builder(simpleName.asString().replaceFirstChar { it.lowercase() })
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

                    parameters
                }

                else -> error("Illegal KDObjectValueType")
            }.let { KDType.create(implClassName, builder, it, voType) }
        }
    }

internal fun KDType.createImplBuilder(superTypeName: TypeName, replacements: Map<WrapperType, BoxedType>) {
    TypeSpec.classBuilder("Builder").also { innerBuilder ->
        FunSpec.builder("build").also { buildFunBuilder ->
            buildFunBuilder.addModifiers(KModifier.INTERNAL).returns(superTypeName)

            parameters.forEach { param ->
                val type = param.typeReference
                buildFunBuilder.takeIf { type is KDReference.Element && !type.typeName.isNullable }
                    ?.addStatement("""requireNotNull(${param.name.value}) { "Property '$superTypeName.${param.name.value}' is not set!" }""")
                param.toBuilderPropertySpec(replacements).also(innerBuilder::addProperty)
            }
            buildFunBuilder.addStatement("return ${implName.name}(")
            parameters.forEach { param ->
                //buildFunBuilder.addStatement("${}")
            }
            /*
            .addStatement("return ${wrapper.name.simpleName}(")
            .addStatement("optName = optName?.let(NameImpl::name),")
            .addStatement("count = CountImpl.count(count!!),")
            .addStatement("uri = UriImpl.uri(uri!!),")
            .addStatement("names = names.map(NameImpl::name),")
            .addStatement("indexes = indexes.map(IndexImpl::index).toSet(),")
            .addStatement("myMap = myMap.entries.associate { IndexImpl.index(it.key) to it.value?.let(NameImpl::name) },")
            .addStatement("inner!!")
             */
            buildFunBuilder.addStatement(")")

        }.also { innerBuilder.addFunction(it.build()) }
        innerBuilder.build().also(builder::addType)
    }
}
