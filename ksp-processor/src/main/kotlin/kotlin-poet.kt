package ru.it_arch.clean_ddd.ksp

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import ru.it_arch.clean_ddd.domain.model.Property
import ru.it_arch.clean_ddd.domain.model.kddd.BoxedWithCommon
import ru.it_arch.clean_ddd.domain.model.kddd.Data
import ru.it_arch.clean_ddd.domain.model.kddd.KdddType
import ru.it_arch.clean_ddd.domain.fullClassName
import ru.it_arch.clean_ddd.domain.`get initializer for DSL Builder or canonical Builder`
import ru.it_arch.clean_ddd.domain.isCollectionType
import ru.it_arch.clean_ddd.domain.model.Context
import ru.it_arch.clean_ddd.domain.model.ILogger
import ru.it_arch.clean_ddd.domain.shortName
import ru.it_arch.clean_ddd.domain.templateBuilderBodyCheck
import ru.it_arch.clean_ddd.domain.templateBuilderFunBuildReturn
import ru.it_arch.clean_ddd.domain.templateForkBody
import ru.it_arch.clean_ddd.domain.templateParseBody
import ru.it_arch.clean_ddd.domain.templateToBuilderBody
import ru.it_arch.clean_ddd.ksp.model.ExtensionFile
import ru.it_arch.clean_ddd.ksp.model.TypeHolder
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

// === Kotlin Poet builders ===

internal val OutputFile.fileSpecBuilder: FileSpec.Builder
    get() = FileSpec.builder(first.impl.packageName.boxed, first.impl.className.boxed).also { it.addFileComment(FILE_HEADER_STUB) }

context(typeCatalog: TypeCatalog, builder: TypeSpec.Builder, logger: ILogger)
internal fun KdddType.Boxed.build(typeHolder: TypeHolder) {
    //val builder: TypeSpec.Builder = TypeSpec.Builder
    with(builder) {
        addModifiers(KModifier.VALUE)
        addAnnotation(JvmInline::class)

        ParameterSpec.builder(KdddType.Boxed.PARAM_NAME, typeHolder.propertyTypes.values.first()).build().also { param ->
            PropertySpec.builder(param.name, param.type, KModifier.OVERRIDE).initializer(param.name).build()
                .also(::addProperty)
            createConstructor(listOf(param)).also(builder::primaryConstructor)
            createToString(param).also(::addFunction)
            createFork(param).also(::addFunction)
            createCompanion(typeHolder.classType, param).also(::addType)
        }
    }
}

context(typeCatalog: TypeCatalog, builder: TypeSpec.Builder, logger: ILogger)
/**
 * Формирование класса `MyTypeImpl : MyType`.
 *
 * Создается:
 * 1. Конструктор класса с параметрами и `init { validate() }`.
 * 2. Функция `fork`.
 * 3. Внутренний класс `Builder` и его парная функция `MyType.toBuilder(): MyTypeImpl.Builder`.
 * 4. Внутренний класс `DslBuilder` и его парная функция `MyType.toDslBuilder(): MyTypeImpl.DslBuilder` если есть опция `hasDsl`.
 * 5. Внутренние классы.
 *
 * @param typeHolder [TypeHolder] [Kddd]-интерфейса.
 * @param dslFile файл для функций-расширений `toBuilder()` и `toDslBuilder`.
 * @receiver [Data] класс, для которого формируется имплементация.
 *
 * */
internal fun Data.build(typeHolder: TypeHolder, dslFile: ExtensionFile) {
    //val builder: TypeSpec.Builder = TypeSpec.Builder
    with(builder) {
        addModifiers(KModifier.DATA)
        addAnnotation(ConsistentCopyVisibility::class)

        createBuildClass(typeHolder).also(::addType)
        createToBuilderFun(typeHolder).also(dslFile.builder::addFunction)

        typeHolder.propertyTypes.entries.map { entry ->
            PropertySpec.builder(entry.key.boxed, entry.value, KModifier.OVERRIDE)
                .initializer(entry.key.boxed)
                .build()
        }.also { propertySpecList ->
            addProperties(propertySpecList)
            propertySpecList.map { ParameterSpec(it.name, it.type) }
                .also { createConstructor(it).also(::primaryConstructor) }
            createFork(propertySpecList).also(::addFunction)
            if (hasDsl) {


                createDslBuildClass(typeHolder, builder, dslFile)



                //createDslBuildClass(typeHolder).also(::addType)
                //createToDslBuilderFun(typeHolder).also(dslFile.builder::addFunction)
            }
        }

        nestedTypes.forEach { kdddType ->
            kdddType.toTypeSpecBuilder(dslFile).build().also(builder::addType)
        }
    }
}

internal val TypeName.collectionType: Property.CollectionType?
    get() = if (this is ParameterizedTypeName) when(rawType) {
        SET -> Property.CollectionType.SET
        LIST -> Property.CollectionType.LIST
        MAP -> Property.CollectionType.MAP
        else -> null
    } else null


// === Private util extensions ===

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

/**
 * ```
 * @Suppress("UNCHECKED_CAST")
 * ```
 * */
private fun FunSpec.Builder.addUncheckedCast(): FunSpec.Builder =
    addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("\"UNCHECKED_CAST\"").build())



internal const val FILE_HEADER_STUB: String = """
AUTO-GENERATED FILE. DO NOT MODIFY.
This file generated automatically by «KDDD» framework.
Author: Tepex <tepex@mail.ru>, Telegram: @Tepex
"""


private fun `preserve imports for Android Studio, not used`(context: Context, logger: ILogger) {}
