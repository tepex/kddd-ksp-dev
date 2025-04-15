package ru.it_arch.clean_ddd.ksp.model

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
import kotlinx.serialization.Serializable
import ru.it_arch.clean_ddd.ksp.model.KDType.Boxed
import ru.it_arch.clean_ddd.ksp.model.KDType.Generatable
import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

internal class KDTypeForGeneration(
    private val context: KDTypeContext,
    val annotations: List<Annotation>,
    boxedType: TypeName? = null,
    isEntity: Boolean = false
) : Generatable {

    override val className = annotations.filterIsInstance<KDGeneratable>().firstOrNull()?.implementationName
        ?.takeIf { it.isNotBlank() }?.let(ClassName::bestGuess) ?: context.toBeGenerated
    // TODO: add case for inner types
    override val classNameRef: String = className.simpleName
    override val builder = TypeSpec.classBuilder(className).addSuperinterface(context.typeName)
    override val propertyHolders: List<KDProperty>
    override val kDddTypeName: TypeName = context.typeName

    private val _nestedTypes = mutableMapOf<TypeName, KDType>()
    override val nestedTypes: Map<TypeName, KDType>
        get() = _nestedTypes.toMap()

    override val hasDsl = annotations.filterIsInstance<KDGeneratable>().firstOrNull()?.dsl != false
    override val hasJson = annotations.filterIsInstance<KDGeneratable>().firstOrNull()?.json == true

    var isParsable: Boolean = false
        private set

    init {
        propertyHolders = boxedType?.let {
            boxedType(boxedType)
            listOf(KDProperty(className.member(Boxed.PARAM_NAME), boxedType))
        } ?: run {
            // not Boxed
            if (!isEntity) {
                builder.addModifiers(KModifier.DATA)
                builder.addAnnotation(ConsistentCopyVisibility::class)
                createForkFun(context.properties)
            }

            if (hasJson) {
                AnnotationSpec.builder(Serializable::class).apply {
                    addMember("with = %L", "${className.simpleName}.Companion::class")
                }.build().also(builder::addAnnotation)
            }

            context.properties
        }
        createConstructor(context.properties)
    }

    private fun boxedType(boxedType: TypeName) {
        builder.addModifiers(KModifier.VALUE)
        builder.addAnnotation(JvmInline::class)

        val boxedParam = ParameterSpec.builder(Boxed.PARAM_NAME, boxedType).build()
        FunSpec.builder("toString").apply {
            addModifiers(KModifier.OVERRIDE)
            returns(String::class)
            addStatement("return %N.toString()", boxedParam)
        }.build().also(builder::addFunction)

        // override fun <T : ValueObject.Boxed<Int>> fork(boxed: Int): T = CoordinateImpl(boxed) as T
        TypeVariableName(
            "T",
            ValueObject.Boxed::class.asTypeName().parameterizedBy(boxedType)
        ).also { tvn ->
            FunSpec.builder(Boxed.CREATE_METHOD).apply {
                addTypeVariable(tvn)
                addParameter(boxedParam)
                addModifiers(KModifier.OVERRIDE)
                addUncheckedCast()
                returns(tvn)
                addStatement("return %T(%N) as %T", className, boxedParam, tvn)
            }.build().also(builder::addFunction)
        }


        /* ValueObject.Boxed companion object */
        TypeSpec.companionObjectBuilder().apply {
            FunSpec.builder("invoke").apply {
                addModifiers(KModifier.OPERATOR)
                addParameter(boxedParam)
                returns(context.typeName)
                addStatement("return %T(%N)", className, boxedParam)
            }.build().let(::addFunction)
            this@KDTypeForGeneration.annotations.filterIsInstance<KDParsable>().firstOrNull()
                ?.also {
                    createParseFun(boxedType, it).let(::addFunction)
                    isParsable = true
                }

        }.build().also(builder::addType)
    }

    private fun createForkFun(properties: List<KDProperty>) {
        FunSpec.builder(Boxed.CREATE_METHOD).apply {
            val typeT = TypeVariableName("T", Kddd::class)
            val typeA = TypeVariableName("A", Kddd::class)

            addTypeVariable(typeT)
            addTypeVariable(typeA)
            addParameter(ParameterSpec.builder("args", typeA, KModifier.VARARG).build())
            addModifiers(KModifier.OVERRIDE)
            addUncheckedCast()
            returns(typeT)
            addStatement("val ret = ${KDType.Data.BUILDER_CLASS_NAME}().apply {⇥\n")
            properties.forEachIndexed { i, property ->
                addStatement("%N = args[$i] as %T", property.name, property.typeName)
            }
            addStatement("⇤}.build() as T")
            addStatement("return ret")
        }.build().also(builder::addFunction)
    }

    private fun createParseFun(boxedType: TypeName, parsable: KDParsable): FunSpec =
        FunSpec.builder(Boxed.FABRIC_PARSE_METHOD).apply {
            val srcParam = ParameterSpec.builder("src", String::class).build()
            addParameter(srcParam)
            returns(className)
            addStatement("return %T${parsable.deserialization.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""}(%N).let(::%T)", boxedType, srcParam, className)
        }.build()

    override fun addNestedType(type: KDType) {
        _nestedTypes[type.kDddTypeName.toNullable(false)] = type
    }

    override fun getKDType(typeName: TypeName) = typeName.toNullable(false).let { key ->

//        if (className.simpleName == "CollectionsImpl" /* && typeName.toString() == "ru.it_arch.clean_ddd.domain.demo.CommonTypes"*/)
//            context.logger.log("type: $typeName global: ${context.globalKDTypes.keys.map { it.toString().substringAfterLast(".demo.") }}")
//            context.logger.log("type: $typeName global: ")

        if (key == KDType.Abstraction.TYPENAME) /*KDType.Abstraction */error("${context.typeName}: WIP. Abstraction not supported yet.")
        else _nestedTypes[key]?.let { it to true }
            ?: context.globalKDTypes[key]?.let { it to false }
                ?.also { context.logger.log("found: ${it.first}") }
            ?: run {
                // to KDType.List, KDType.Set, KDType.Map
                error("Can't find implementation for $key in $className")
            }
    }

    private fun createConstructor(parameters: List<KDProperty>) {
        parameters.map { param ->
            PropertySpec
                .builder(param.name.simpleName, param.typeName, KModifier.OVERRIDE)
                .initializer(param.name.simpleName)
                .build()
        }.also(builder::addProperties)

        FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addParameters(parameters.map { ParameterSpec(it.name.simpleName, it.typeName) })
            .addStatement("validate()")
            .build()
            .also(builder::primaryConstructor)
    }
}
