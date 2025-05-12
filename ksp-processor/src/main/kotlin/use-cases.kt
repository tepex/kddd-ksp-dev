package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
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
import ru.it_arch.clean_ddd.domain.Options
import ru.it_arch.clean_ddd.domain.Property
import ru.it_arch.clean_ddd.domain.core.BoxedWithCommon
import ru.it_arch.clean_ddd.domain.core.BoxedWithPrimitive
import ru.it_arch.clean_ddd.domain.core.Data
import ru.it_arch.clean_ddd.domain.fullClassName
import ru.it_arch.clean_ddd.domain.templateParseBody
import ru.it_arch.clean_ddd.domain.property
import ru.it_arch.clean_ddd.domain.templateForkBody
import ru.it_arch.clean_ddd.domain.toKDddType
import ru.it_arch.kddd.KDIgnore
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

internal fun KSPropertyDeclaration.toProperty(): Property = type.toTypeName().let { type ->
    property {
        name = simpleName.asString()
        className = type.toString()
        isNullable = type.isNullable
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

internal typealias TypeCatalog = Map<CompositeClassName.FullClassName, TypeName>

context(options: Options)
@OptIn(KspExperimental::class)
internal infix fun KSFile.`to OutputFile with`(visitor: Visitor): OutputFile? =
    declarations
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.classKind == ClassKind.INTERFACE && it.getAnnotationsByType(KDIgnore::class).count() == 0 }
        .firstOrNull()
        ?.let { declaration ->
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

private const val FILE_HEADER_STUB: String = """
AUTO-GENERATED FILE. DO NOT MODIFY.
This file generated automatically by «KDDD» framework.
Author: Tepex <tepex@mail.ru>, Telegram: @Tepex
"""

private val KdddType.Boxed.boxedTypeName: TypeName get() = when(this) {
    is BoxedWithCommon -> ClassName.bestGuess(boxed.boxed)
    is BoxedWithPrimitive -> when(boxed) {
        BoxedWithPrimitive.PrimitiveClassName.STRING  -> STRING
        BoxedWithPrimitive.PrimitiveClassName.BOOLEAN -> BOOLEAN
        BoxedWithPrimitive.PrimitiveClassName.BYTE    -> BYTE
        BoxedWithPrimitive.PrimitiveClassName.CHAR    -> CHAR
        BoxedWithPrimitive.PrimitiveClassName.FLOAT   -> FLOAT
        BoxedWithPrimitive.PrimitiveClassName.DOUBLE  -> DOUBLE
        BoxedWithPrimitive.PrimitiveClassName.INT     -> INT
        BoxedWithPrimitive.PrimitiveClassName.LONG    -> LONG
        BoxedWithPrimitive.PrimitiveClassName.SHORT   -> SHORT
    }
}

private val KdddType.Boxed.boxedParam: ParameterSpec
    get() = ParameterSpec.builder(KdddType.Boxed.PARAM_NAME, boxedTypeName).build()

private typealias KotlinPoetProperty = Triple<TypeName, ParameterSpec, MemberName>


private fun Property.toKotlinPoetProperty(typeCatalog: TypeCatalog, implClassName: ClassName): KotlinPoetProperty =
    typeCatalog[className]
        ?.let { KotlinPoetProperty(it, ParameterSpec(name.boxed, it), implClassName.member(name.boxed)) }
        ?: error("Can't find type name for property $this")

context(typeCatalog: TypeCatalog)
internal val KdddType.typeSpecBuilder: TypeSpec.Builder get() {
    //val typeCatalog: TypeCatalog = TypeCatalog
    val implClassName = ClassName.bestGuess(impl.fullClassName.boxed)

    return typeCatalog[kddd.fullClassName]?.let { kdddTypeName ->
        TypeSpec.classBuilder(implClassName).addSuperinterface(kdddTypeName).apply {

            when(this@typeSpecBuilder) {
                is KdddType.ModelContainer -> {
                    if (this@typeSpecBuilder is Data) {


                        val kpProperties = properties.map { it.toKotlinPoetProperty(typeCatalog, implClassName) }


                        addModifiers(KModifier.DATA)
                        addAnnotation(ConsistentCopyVisibility::class)
                        createFork(kpProperties).also(::addFunction)
                    }
                }
                is KdddType.Boxed -> {
                    addModifiers(KModifier.VALUE)
                    addAnnotation(JvmInline::class)

                    createToString().also(::addFunction)
                    createFork(implClassName).also(::addFunction)
                    createCompanion(kdddTypeName, implClassName).also(::addType)
                }
            }
        }
    } ?: error("Type ${kddd.fullClassName} not found in type catalog!")
}

/**
 * ```
 * override fun toString(): String { <body> }
 * ```
 * */
private fun KdddType.Boxed.createToString(): FunSpec =
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
private fun KdddType.Boxed.createFork(implClassName: ClassName): FunSpec =
    TypeVariableName(
        "T",
        ValueObject.Boxed::class.asTypeName().parameterizedBy(boxedTypeName)
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
private fun KdddType.Boxed.createCompanion(kdddTypeName: TypeName, implClassName: ClassName): TypeSpec =
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
                addStatement(templateParseBody, boxedTypeName, srcParam, implClassName)
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
            { format, i -> addStatement(format, kpProperties[i].third, kpProperties[i].first) }
        )
    }.build()

/**
 * ```
 * @Suppress("UNCHECKED_CAST")
 * ```
 * */
private fun FunSpec.Builder.addUncheckedCast(): FunSpec.Builder =
    addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("\"UNCHECKED_CAST\"").build())
