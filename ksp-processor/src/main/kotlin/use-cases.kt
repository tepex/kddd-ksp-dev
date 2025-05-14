package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.domain.CompositeClassName
import ru.it_arch.clean_ddd.domain.core.KdddType
import ru.it_arch.clean_ddd.domain.Context
import ru.it_arch.clean_ddd.domain.ILogger
import ru.it_arch.clean_ddd.domain.Options
import ru.it_arch.clean_ddd.domain.Property
import ru.it_arch.clean_ddd.domain.core.BoxedWithCommon
import ru.it_arch.clean_ddd.domain.core.BoxedWithPrimitive
import ru.it_arch.clean_ddd.domain.core.Data
import ru.it_arch.clean_ddd.domain.fullClassName
import ru.it_arch.clean_ddd.domain.templateParseBody
import ru.it_arch.clean_ddd.domain.property
import ru.it_arch.clean_ddd.domain.templateBuilderBodyCheck
import ru.it_arch.clean_ddd.domain.templateForkBody
import ru.it_arch.clean_ddd.domain.toKDddType
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

context(_: Context, _: Options)
/**
 * ValueObject.* -> KdddType.*
 */
internal fun KSTypeReference.toKdddType(): KdddType =
    toString().substringBefore('<').toKDddType()

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
            logger.log("process $declaration")
            //basePackageName ?: run { basePackageName = declaration toImplementationPackage options.subpackage }
            visitor.visitKDDeclaration(declaration, null).let { kdddType ->
                if (kdddType is KdddType.ModelContainer) OutputFile(kdddType, Dependencies(false, this))
                else null
            }
        }

internal fun List<OutputFile>.findShortestPackageName(): CompositeClassName.PackageName =
    reduce { acc, outputFile ->
        outputFile.takeIf { it.first.impl.packageName.boxed.length < acc.first.impl.packageName.boxed.length } ?: acc
    }.first.impl.packageName

internal val OutputFile.fileSpecBuilder: FileSpec.Builder
    get() = FileSpec.builder(first.impl.packageName.boxed, first.impl.className.boxed).also { it.addFileComment(FILE_HEADER_STUB) }


// Private util extensions

private const val FILE_HEADER_STUB: String = """
AUTO-GENERATED FILE. DO NOT MODIFY.
This file generated automatically by «KDDD» framework.
Author: Tepex <tepex@mail.ru>, Telegram: @Tepex
"""

private typealias KotlinPoetProperty = Pair<PropertySpec, MemberName>

private fun Property.toDataProperty(typeName: TypeName, implClassName: ClassName): KotlinPoetProperty =
    PropertySpec
        .builder(name.boxed, typeName.copy(nullable = isNullable), KModifier.OVERRIDE)
            .initializer(name.boxed)
            .build() to implClassName.member(name.boxed)

private fun Property.toBuilderProperty(typeName: TypeName, implClassName: ClassName): KotlinPoetProperty =
    PropertySpec
        .builder(name.boxed, typeName.copy(nullable = true))
        .mutable()
        .initializer("null")
        .build() to implClassName.member(name.boxed)


context(typeCatalog: TypeCatalog, logger: ILogger)
/**
 *
 * */
internal val KdddType.typeSpecBuilder: TypeSpec.Builder
    get() = ClassName.bestGuess(impl.fullClassName.boxed).let { implClassName ->
        //val typeCatalog: TypeCatalog = TypeCatalog
        typeCatalog[kddd.fullClassName]?.let { holder ->
            TypeSpec.classBuilder(implClassName).addSuperinterface(holder.classType).apply {
                when (this@typeSpecBuilder) {
                    is KdddType.ModelContainer ->
                        if (this@typeSpecBuilder is Data) build(holder, implClassName)
                    is KdddType.Boxed -> build(holder.classType, implClassName)
                }
            }
        } ?: error("Type ${kddd.fullClassName} not found in type catalog!")
    }

context(builder: TypeSpec.Builder)
private fun KdddType.Boxed.build(kdddTypeName: TypeName, implClassName: ClassName) {
    //val builder: TypeSpec.Builder = TypeSpec.Builder
    with(builder) {
        addModifiers(KModifier.VALUE)
        addAnnotation(JvmInline::class)

        createBoxedParam().also { param ->
            addProperty(param.propertySpec)
            createConstructor(listOf(param))
            createToString(param).also(::addFunction)
            createFork(implClassName, param).also(::addFunction)
            createCompanion(kdddTypeName, implClassName, param).also(::addType)
        }
    }
}

context(builder: TypeSpec.Builder, logger: ILogger)
private fun Data.build(typeHolder: TypeHolder, implClassName: ClassName) {
    //val builder: TypeSpec.Builder = TypeSpec.Builder
    with(builder) {
        addModifiers(KModifier.DATA)
        addAnnotation(ConsistentCopyVisibility::class)

        properties.map { it.toDataProperty(
            typeHolder.properties[it.name] ?: error("Can't find type name for property ${it.name} in ${this@build.kddd.fullClassName}"),
            implClassName
        ) }.also { kp ->
            kp.map { it.first }.also(::addProperties)
            kp.map { ParameterSpec(it.first.name, it.first.type) }
                .also { createConstructor(it).also(::primaryConstructor) }
            createFork(kp).also(::addFunction)
            logger.log("createBuildClass for $implClassName")
            createBuildClass(typeHolder, implClassName).also(::addType)
        }
    }
}

context(logger: ILogger)
private fun Data.createBuildClass(typeHolder: TypeHolder, implClassName: ClassName): TypeSpec =
    implClassName.nestedClass(Data.BUILDER_CLASS_NAME).let(TypeSpec::classBuilder).apply {
        // add properties
        properties.map { it.toBuilderProperty(
            typeHolder.properties[it.name] ?: error("Can't find type name for property ${it.name} in ${this@createBuildClass.kddd.fullClassName}"),
            implClassName
        ) }.also { kp -> kp.map { it.first }.also(::addProperties) }

        // fun build(): KdddType
        FunSpec.builder(Data.BUILDER_BUILD_METHOD_NAME).apply {
            returns(typeHolder.classType)
            //add `checkNotNull(<property>)`
            templateBuilderBodyCheck { format, i ->
                addStatement(format, properties[i].name.boxed, implClassName, properties[i].name.boxed)
            }
        }.build().also(::addFunction)
    }.build()

private fun KdddType.Boxed.createBoxedParam(): ParameterSpec = when(this) {
    is BoxedWithCommon -> ClassName.bestGuess(boxed.boxed)
    is BoxedWithPrimitive -> when (boxed) {
        BoxedWithPrimitive.PrimitiveClassName.STRING -> STRING
        BoxedWithPrimitive.PrimitiveClassName.BOOLEAN -> BOOLEAN
        BoxedWithPrimitive.PrimitiveClassName.BYTE -> BYTE
        BoxedWithPrimitive.PrimitiveClassName.CHAR -> CHAR
        BoxedWithPrimitive.PrimitiveClassName.FLOAT -> FLOAT
        BoxedWithPrimitive.PrimitiveClassName.DOUBLE -> DOUBLE
        BoxedWithPrimitive.PrimitiveClassName.INT -> INT
        BoxedWithPrimitive.PrimitiveClassName.LONG -> LONG
        BoxedWithPrimitive.PrimitiveClassName.SHORT -> SHORT
    }
}.let { ParameterSpec.builder(KdddType.Boxed.PARAM_NAME, it) }.build()

private val ParameterSpec.propertySpec: PropertySpec
    get() = PropertySpec.builder(name, type, KModifier.OVERRIDE).initializer(name).build()

/**
 *
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
private fun createFork(implClassName: ClassName, boxedParam: ParameterSpec): FunSpec =
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
            addStatement(KdddType.Boxed.TEMPLATE_FORK_BODY, implClassName, boxedParam, tvn)
        }.build()
    }

/**
 * ```
 * public companion object {
 *     public operator fun invoke(<boxedParam>): <kdddTypeName> { <body> }
 * }
 * ```
 * */
private fun KdddType.Boxed.createCompanion(kdddTypeName: TypeName, implClassName: ClassName, boxedParam: ParameterSpec): TypeSpec =
    TypeSpec.companionObjectBuilder().apply {
        FunSpec.builder("invoke").apply {
            addModifiers(KModifier.OPERATOR)
            addParameter(boxedParam)
            returns(kdddTypeName)
            addStatement(KdddType.Boxed.TEMPLATE_COMPANION_INVOKE_BODY, implClassName, boxedParam)
        }.build().let(::addFunction)

        // `public fun parse(src: String): <implClassName> { <body> }`
        if (this@createCompanion is BoxedWithCommon) {
            FunSpec.builder(BoxedWithCommon.FABRIC_PARSE_METHOD).apply {
                val srcParam = ParameterSpec.builder("src", String::class).build()
                addParameter(srcParam)
                returns(implClassName)
                addStatement(templateParseBody, boxedParam.type, srcParam, implClassName)
            }.build()
        }
    }.build()

/**
 * ```
 * @Suppress("UNCHECKED_CAST")
 * override fun <T : Kddd, A : Kddd> fork(vararg args: A): T { <body> }
 * ```
 * */
private fun Data.createFork(kpProperties: List<KotlinPoetProperty>): FunSpec =
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
            { format, i -> addStatement(format, kpProperties[i].second, kpProperties[i].first.type) }
        )
    }.build()

/**
 * ```
 * @Suppress("UNCHECKED_CAST")
 * ```
 * */
private fun FunSpec.Builder.addUncheckedCast(): FunSpec.Builder =
    addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("\"UNCHECKED_CAST\"").build())
