package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ANY
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
import ru.it_arch.clean_ddd.ksp.model.KDType.Value
import ru.it_arch.k3dm.Fts
import ru.it_arch.k3dm.Parsable
import ru.it_arch.k3dm.ValueObject
import ru.it_arch.k3dm.Generatable as AGeneratable

internal class KDTypeForGeneration(
    private val context: KDTypeContext,
    val annotations: List<Annotation>,
    boxedType: TypeName? = null,
    isEntity: Boolean = false
) : KDType.Generatable {

    override val className = annotations.filterIsInstance<AGeneratable>().firstOrNull()?.implementationName
        ?.takeIf { it.isNotBlank() }?.let(ClassName::bestGuess) ?: context.toBeGenerated
    // TODO: add case for inner types
    override val classNameRef: String = className.simpleName
    override val builder = TypeSpec.classBuilder(className).addSuperinterface(context.typeName)
    override val propertyHolders: List<KDProperty>
    override val kDddTypeName: TypeName = context.typeName

    private val _nestedTypes = mutableMapOf<TypeName, KDType>()
    override val nestedTypes: Map<TypeName, KDType>
        get() = _nestedTypes.toMap()

    override val hasDsl = annotations.filterIsInstance<AGeneratable>().firstOrNull()?.dsl != false
    override val hasJson = annotations.filterIsInstance<AGeneratable>().firstOrNull()?.json == true

    var isParsable: Boolean = false
        private set

    init {
        propertyHolders = boxedType?.let {
            boxedType(boxedType)
            listOf(KDProperty(className.member(Value.PARAM_NAME), boxedType))
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

        val valueParam = ParameterSpec.builder(Value.PARAM_NAME, boxedType).build()
        FunSpec.builder("toString").apply {
            addModifiers(KModifier.OVERRIDE)
            returns(String::class)
            addStatement("return %N.toString()", valueParam)
        }.build().also(builder::addFunction)

        // override fun <T : ValueObject.Value<Int>> apply(boxed: Int): T = CoordinateImpl(boxed) as T
        TypeVariableName(
            "T",
            ValueObject.Value::class.asTypeName().parameterizedBy(boxedType)
        ).also { tvn ->
            FunSpec.builder(Value.APPLY_METHOD).apply {
                addTypeVariable(tvn)
                addParameter(valueParam)
                addModifiers(KModifier.OVERRIDE)
                addUncheckedCast()
                returns(tvn)
                addStatement("return %T(%N) as %T", className, valueParam, tvn)
            }.build().also(builder::addFunction)
        }


        /* ValueObject.Value companion object */
        TypeSpec.companionObjectBuilder().apply {
            FunSpec.builder("invoke").apply {
                addModifiers(KModifier.OPERATOR)
                addParameter(valueParam)
                returns(context.typeName)
                addStatement("return %T(%N)", className, valueParam)
            }.build().let(::addFunction)
            this@KDTypeForGeneration.annotations.filterIsInstance<Parsable>().firstOrNull()
                ?.also {
                    createParseFun(boxedType, it).let(::addFunction)
                    isParsable = true
                }

        }.build().also(builder::addType)
    }

    private fun createForkFun(properties: List<KDProperty>) {
        FunSpec.builder(KDType.Data.FORK_METHOD).apply {
            //val typeT = TypeVariableName("T", Fts::class)
            val typeT = TypeVariableName("T", ValueObject.Data::class)
            //val typeA = TypeVariableName("A", Any::class).toNullable()
            val typeA = ANY.toNullable()

            addTypeVariable(typeT)
            //addTypeVariable(typeA)
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

    private fun createParseFun(boxedType: TypeName, parsable: Parsable): FunSpec =
        FunSpec.builder(Value.FABRIC_PARSE_METHOD).apply {
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
