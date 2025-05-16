package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.domain.core.CompositeClassName
import ru.it_arch.clean_ddd.domain.core.KdddType
import ru.it_arch.clean_ddd.domain.ILogger
import ru.it_arch.clean_ddd.domain.Options
import ru.it_arch.clean_ddd.domain.Property
import ru.it_arch.clean_ddd.domain.core.BoxedWithCommon
import ru.it_arch.clean_ddd.domain.core.Data
import ru.it_arch.clean_ddd.domain.fullClassName
import ru.it_arch.clean_ddd.domain.templateParseBody
import ru.it_arch.clean_ddd.domain.property
import ru.it_arch.clean_ddd.domain.shortName
import ru.it_arch.clean_ddd.domain.templateBuilderBodyCheck
import ru.it_arch.clean_ddd.domain.templateBuilderFunBuildReturn
import ru.it_arch.clean_ddd.domain.templateForkBody
import ru.it_arch.clean_ddd.domain.templateToBuilderBody
import ru.it_arch.clean_ddd.ksp.model.ExtensionFile
import ru.it_arch.clean_ddd.ksp.model.TypeHolder
import ru.it_arch.kddd.KDIgnore
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

/**
 * property name -> property type
 * */
internal typealias PropertyHolders = Map<Property.Name, TypeName>

internal typealias TypeCatalog = Map<CompositeClassName.FullClassName, TypeHolder>

internal fun KSClassDeclaration.toPropertyHolders(): PropertyHolders =
    getAllProperties().associate { Property.Name(it.simpleName.asString()) to it.type.toTypeName() }

internal fun PropertyHolders.toProperties(): List<Property> =
    entries.map { entry ->
        property {
            name = entry.key
            className = entry.value.toString()
            isNullable = entry.value.isNullable
        }
    }

/**
 * */
internal typealias OutputFile = Pair<KdddType.ModelContainer, Dependencies>

context(options: Options, logger: ILogger)
@OptIn(KspExperimental::class)
internal infix fun KSFile.`to OutputFile with`(visitor: Visitor): OutputFile? =
    declarations
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.classKind == ClassKind.INTERFACE && it.getAnnotationsByType(KDIgnore::class).count() == 0 }
        .firstOrNull()
        ?.let { declaration ->
            visitor.visitKDDeclaration(declaration, null).let { kdddType ->
                if (kdddType is KdddType.ModelContainer) OutputFile(kdddType, Dependencies(false, this))
                else null
            }
        }

internal val OutputFile.fileSpecBuilder: FileSpec.Builder
    get() = FileSpec.builder(first.impl.packageName.boxed, first.impl.className.boxed).also { it.addFileComment(FILE_HEADER_STUB) }

context(typeCatalog: TypeCatalog, logger: ILogger)
/**
 *
 * */
internal fun KdddType.toTypeSpecBuilder(dslFile: ExtensionFile): TypeSpec.Builder {
    //val typeCatalog: TypeCatalog = TypeCatalog
    return typeCatalog[kddd.fullClassName]?.let { holder ->
        TypeSpec.classBuilder(impl.className.shortName).addSuperinterface(holder.classType).apply {
            when(this@toTypeSpecBuilder) {
                is KdddType.ModelContainer ->
                    if (this@toTypeSpecBuilder is Data) build(holder, dslFile)
                is KdddType.Boxed -> build(holder)
            }
        }
    } ?: error("Type ${kddd.fullClassName} not found in type catalog!")
}

internal fun List<OutputFile>.createDslFile(): ExtensionFile =
    findShortestPackageName().let { shortestPackageName ->
        FileSpec.builder(shortestPackageName.boxed, "dsl")
            .addFileComment(FILE_HEADER_STUB)
            .let { dslFileBuilder ->
                extensionFile {
                    builder = dslFileBuilder
                    packageName = shortestPackageName
                    name = "dsl"
                }
            }
    }


// Private util extensions

private const val FILE_HEADER_STUB: String = """
AUTO-GENERATED FILE. DO NOT MODIFY.
This file generated automatically by «KDDD» framework.
Author: Tepex <tepex@mail.ru>, Telegram: @Tepex
"""

private fun List<OutputFile>.findShortestPackageName(): CompositeClassName.PackageName =
    reduce { shortest, outputFile ->
        outputFile.takeIf { it.first.impl.packageName.boxed.length < shortest.first.impl.packageName.boxed.length } ?: shortest
    }.first.impl.packageName

private fun extensionFile(block: ExtensionFile.Builder.() -> Unit): ExtensionFile =
    ExtensionFile.Builder().apply(block).build()

private infix fun Property.toDataPropertySpec(typeName: TypeName): PropertySpec =
    PropertySpec.builder(name.boxed, typeName.copy(nullable = isNullable), KModifier.OVERRIDE)
        .initializer(name.boxed)
        .build()

private infix fun Property.toBuilderPropertySpec(typeName: TypeName): PropertySpec =
    PropertySpec.builder(name.boxed, typeName.copy(nullable = true))
        .mutable()
        .initializer("null")
        .build()

context(typeCatalog: TypeCatalog, builder: TypeSpec.Builder, logger: ILogger)
private fun KdddType.Boxed.build(typeHolder: TypeHolder) {
    //val builder: TypeSpec.Builder = TypeSpec.Builder
    with(builder) {
        addModifiers(KModifier.VALUE)
        addAnnotation(JvmInline::class)

        ParameterSpec.builder(KdddType.Boxed.PARAM_NAME, typeHolder.properties.values.first()).build().also { param ->
            addProperty(param.propertySpec)
            createConstructor(listOf(param)).also(builder::primaryConstructor)
            createToString(param).also(::addFunction)
            createFork(param).also(::addFunction)
            createCompanion(typeHolder.classType, param).also(::addType)
        }
    }
}

context(typeCatalog: TypeCatalog, builder: TypeSpec.Builder, logger: ILogger)
private fun Data.build(typeHolder: TypeHolder, dslFile: ExtensionFile) {
    //val builder: TypeSpec.Builder = TypeSpec.Builder
    with(builder) {
        addModifiers(KModifier.DATA)
        addAnnotation(ConsistentCopyVisibility::class)

        properties.map {
            it toDataPropertySpec
                (typeHolder.properties[it.name] ?: error("Can't find type name for property ${it.name} in ${this@build.kddd.fullClassName}"))
        }.also { propertySpecList ->
            addProperties(propertySpecList)
            propertySpecList.map { ParameterSpec(it.name, it.type) }
                .also { createConstructor(it).also(::primaryConstructor) }
            createFork(propertySpecList).also(::addFunction)
            createBuildClass(typeHolder).also(::addType)
            createToBuilderFun(typeHolder).also(dslFile.builder::addFunction)
        }

        nestedTypes.forEach { kdddType ->
            kdddType.toTypeSpecBuilder(dslFile).build().also(builder::addType)
        }
    }
}

context(logger: ILogger)
private fun Data.createBuildClass(typeHolder: TypeHolder): TypeSpec =
    ClassName.bestGuess("${impl.className.shortName}.${Data.BUILDER_CLASS_NAME}").let(TypeSpec::classBuilder).apply {
        // add properties
        properties.map {
            it toBuilderPropertySpec
                (typeHolder.properties[it.name] ?: error("Can't find type name for property ${it.name} in ${this@createBuildClass.kddd.fullClassName}"))
        }.also(::addProperties)

        // fun build(): KdddType
        FunSpec.builder(Data.BUILDER_BUILD_METHOD_NAME).apply {
            returns(typeHolder.classType)
            //add `checkNotNull(<property>)`
            templateBuilderBodyCheck { format, i ->
                addStatement(format, properties[i].name.boxed, Data.BUILDER_CLASS_NAME, properties[i].name.boxed)
            }
            // `return <MyTypeImpl>(p1 = p1, p2 = p2, ...)`
            templateBuilderFunBuildReturn(
                { addStatement(it, impl.className.shortName) },
                { format, i -> addStatement(format, properties[i].name.boxed, properties[i].name.boxed) }
            )
        }.build().also(::addFunction)
    }.build()

private val ParameterSpec.propertySpec: PropertySpec
    get() = PropertySpec.builder(name, type, KModifier.OVERRIDE).initializer(name).build()

/**
 * For All
 * */
private fun createConstructor(parameters: List<ParameterSpec>): FunSpec =
    FunSpec.constructorBuilder()
        .addModifiers(KModifier.PRIVATE)
        .addParameters(parameters)
        .addStatement("validate()")
        .build()

/**
 * ```
 * override fun toString(): String { <body> }
 * ```
 * */
private fun createToString(boxedParam: ParameterSpec): FunSpec =
    FunSpec.builder("toString").apply {
        addModifiers(KModifier.OVERRIDE)
        returns(String::class)
        addStatement(KdddType.Boxed.TEMPLATE_TO_STRING_BODY, boxedParam)
    }.build()

/**
 * ```
 * @Suppress("UNCHECKED_CAST")
 * override fun <T : ValueObject.Boxed<BOXED>> fork(<boxedParam>): T { <body> }
 * ```
 * */
private fun KdddType.Boxed.createFork(boxedParam: ParameterSpec): FunSpec =
    TypeVariableName(
        "T",
        ValueObject.Boxed::class.asTypeName().parameterizedBy(boxedParam.type)
    ).let { tvn ->
        FunSpec.builder(KdddType.Boxed.FORK_METHOD).apply {
            addTypeVariable(tvn)
            addParameter(boxedParam)
            addModifiers(KModifier.OVERRIDE)
            addUncheckedCast()
            returns(tvn)
            addStatement(KdddType.Boxed.TEMPLATE_FORK_BODY, impl.className.shortName, boxedParam, tvn)
        }.build()
    }

/**
 * ```
 * public companion object {
 *     public operator fun invoke(<boxedParam>): <kdddTypeName> { <body> }
 * }
 * ```
 * */
private fun KdddType.Boxed.createCompanion(kdddTypeName: TypeName, boxedParam: ParameterSpec): TypeSpec =
    TypeSpec.companionObjectBuilder().apply {
        FunSpec.builder("invoke").apply {
            addModifiers(KModifier.OPERATOR)
            addParameter(boxedParam)
            returns(kdddTypeName)
            addStatement(KdddType.Boxed.TEMPLATE_COMPANION_INVOKE_BODY, impl.className.shortName, boxedParam)
        }.build().let(::addFunction)

        // `public fun parse(src: String): <implClassName> { <body> }`
        if (this@createCompanion is BoxedWithCommon) {
            FunSpec.builder(BoxedWithCommon.FABRIC_PARSE_METHOD).apply {
                val srcParam = ParameterSpec.builder("src", String::class).build()
                addParameter(srcParam)
                returns(ClassName.bestGuess(impl.className.shortName))
                addStatement(templateParseBody, boxedParam.type, srcParam, impl.className.shortName)
            }.build().let(::addFunction)
        }
    }.build()

/**
 * ```
 * @Suppress("UNCHECKED_CAST")
 * override fun <T : Kddd, A : Kddd> fork(vararg args: A): T { <body> }
 * ```
 * */
private fun Data.createFork(kpProperties: List<PropertySpec>): FunSpec =
    FunSpec.builder(KdddType.Boxed.FORK_METHOD).apply {
        val typeT = TypeVariableName("T", Kddd::class)
        val typeA = TypeVariableName("A", Kddd::class)

        addTypeVariable(typeT)
        addTypeVariable(typeA)
        addParameter(ParameterSpec.builder("args", typeA, KModifier.VARARG).build())
        addModifiers(KModifier.OVERRIDE)
        addUncheckedCast()
        returns(typeT)
        templateForkBody(
            { addStatement(it) },
            { format, i -> addStatement(format, kpProperties[i].name, kpProperties[i].type) }
        )
    }.build()

private fun Data.createToBuilderFun(typeHolder: TypeHolder): FunSpec =
    ClassName.bestGuess("${impl.fullClassName.boxed}.${Data.BUILDER_CLASS_NAME}").let { builderClass ->
        FunSpec.builder("toBuilder").apply {
            receiver(typeHolder.classType)
            returns(builderClass)
            templateToBuilderBody { addStatement(it, builderClass) }
        }.build()
    }

/**
 * ```
 * @Suppress("UNCHECKED_CAST")
 * ```
 * */
private fun FunSpec.Builder.addUncheckedCast(): FunSpec.Builder =
    addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("\"UNCHECKED_CAST\"").build())
