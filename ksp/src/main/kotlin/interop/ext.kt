package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName

internal fun KSClassDeclaration.toClassNameImpl(): ClassName =
    ClassName.bestGuess("${simpleName.asString()}Impl")

internal fun KSClassDeclaration.toKDType(logger: KSPLogger, voType: KDValueObjectType): KDType {
    fun TypeSpec.Builder.createConstructor(parameters: List<KDParameter>) {
        parameters.map { param ->
            PropertySpec
                .builder(param.name.simpleName, param.typeReference.typeName,KModifier.OVERRIDE)
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

                    getAllProperties().map {
                        val typeName = it.type.toTypeName()
                        val memb = implClassName.member(it.simpleName.asString())
                        logger.warn(">>> to KDParameter: `${it.simpleName.asString()}` [$memb]: $typeName ${typeName::class.simpleName}")
                        KDParameter.create(memb, it)
                    }.toList().also(builder::createConstructor)
                }

                is KDValueObjectType.KDValueObjectSingle -> {
                    builder.addModifiers(KModifier.VALUE)
                    builder.addAnnotation(JvmInline::class)

                    listOf(KDParameter.create(implClassName.member("value"), voType.boxedType)).apply {
                        // value class constructor
                        builder.createConstructor(this)

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
                    }
                }

                else -> error("Illegal KDObjectValueType")
            }.let { KDType.create(implClassName, builder, it, voType) }
        }
    }
}

internal fun KDType.createImplBuilder(superTypeName: TypeName) {
    TypeSpec.classBuilder("Builder").also { innerBuilder ->
        FunSpec.builder("build").also { buildFunBuilder ->
            buildFunBuilder.addModifiers(KModifier.INTERNAL).returns(superTypeName)

            parameters.forEach { param ->
                val type = param.typeReference
                buildFunBuilder.takeIf { type is KDReference.Element && !type.typeName.isNullable }
                    ?.addStatement("""requireNotNull(%N) { "Property '%T.%N' is not set!" }""", param.name, superTypeName, param.name)
                param.toBuilderPropertySpec(innerTypes).also(innerBuilder::addProperty)
            }
            //buildFunBuilder.addStatement("return ${className.simpleName}(")
            buildFunBuilder.addStatement("return %T(", className)
            parameters.forEach { param ->
                // name = NameImpl.name(name!!),
                //buildFunBuilder.addStatement("${}")
            }
            /*
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

private fun KDParameter.toBuilderPropertySpec(replacements: Map<TypeName, KDType>): PropertySpec {
    fun TypeName.toNullable(nullable: Boolean = true) =
        if (isNullable != nullable) copy(nullable = nullable) else this

    fun getBoxedTypeOrNull(typeName: TypeName): TypeName? =
        replacements[typeName.toNullable(false)]?.getBoxedTypeOrNull()

    return typeReference.let { kdReference ->
        when (kdReference) {
            is KDReference.Collection -> {
                kdReference.parameterizedTypeName.typeArguments.toMutableList().let { args ->
                    args.forEachIndexed { i, arg ->
                        getBoxedTypeOrNull(arg)?.also { args[i] = it.toNullable(arg.isNullable) }
                    }
                    PropertySpec.builder(name.simpleName, kdReference.parameterizedTypeName.copy(typeArguments = args))
                        .initializer(kdReference.collectionType.initializer)
                }
            }

            is KDReference.Element ->
                (getBoxedTypeOrNull(kdReference.typeName) ?: kdReference.typeName)
                    .let { PropertySpec.builder(name.simpleName, it.toNullable()).initializer("null") }
        }.mutable().build()
    }
}
