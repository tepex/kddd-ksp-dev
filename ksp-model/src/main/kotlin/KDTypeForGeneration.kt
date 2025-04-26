package ru.it_arch.clean_ddd.ksp_model

import com.squareup.kotlinpoet.AnnotationSpec
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
import ru.it_arch.clean_ddd.ksp_model.model.KDProperty
import ru.it_arch.clean_ddd.ksp_model.model.KDType
import ru.it_arch.clean_ddd.ksp_model.model.KDTypeContext
import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

/**
 * Делегат интерфейса [KDType.Generatable].
 *
 * @property context
 * @param boxedType
 * @param isEntity
 * */
internal class KDTypeForGeneration(
    private val context: KDTypeContext,
    boxedType: TypeName? = null,
    isEntity: Boolean = false
) : KDType.Generatable {

    override val implName = context.implName
    override val fullClassName = context.fullClassName
    override val builder = TypeSpec.classBuilder(implName).addSuperinterface(context.name)
    override val properties: List<KDProperty>
    override val name: TypeName = context.name

    private val _nestedTypes = mutableSetOf<KDType>()
    override val nestedTypes: Set<KDType>
        get() = _nestedTypes.toSet()

    override val hasDsl = getAnnotation<KDGeneratable>()?.dsl != false
    override val hasJson = getAnnotation<KDGeneratable>()?.json == true

    /*
    var isParsable: Boolean = false
        private set*/

    init {
        //context.logger.log("className: $classNameRef, package: ${context.packageName}, impl package: ${context.options.getImplementationPackage(context.packageName.boxed)}")
        properties = boxedType?.let {
            boxedType(boxedType)
            listOf(KDProperty(implName.member(KDType.Boxed.PARAM_NAME), boxedType))
        } ?: run {
            // not Boxed
            if (!isEntity) {
                builder.addModifiers(KModifier.DATA)
                builder.addAnnotation(ConsistentCopyVisibility::class)
                createForkFun(context.properties)
            }

            if (hasJson) {
                AnnotationSpec.builder(Serializable::class).apply {
                    addMember("with = %L", "${implName.simpleName}.Companion::class")
                }.build().also(builder::addAnnotation)
            }
            context.properties
        }

        // Генерация конструктора
        context.properties.map { param ->
            PropertySpec
                .builder(param.name, param.type, KModifier.OVERRIDE)
                .initializer(param.name)
                .build()
        }.also(builder::addProperties)

        FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addParameters(context.properties.map { ParameterSpec(it.name, it.type) })
            .addStatement("validate()")
            .build()
            .also(builder::primaryConstructor)
    }

    private fun boxedType(boxedType: TypeName) {
        builder.addModifiers(KModifier.VALUE)
        builder.addAnnotation(JvmInline::class)

        val boxedParam = ParameterSpec.builder(KDType.Boxed.PARAM_NAME, boxedType).build()
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
            FunSpec.builder(KDType.Boxed.CREATE_METHOD).apply {
                addTypeVariable(tvn)
                addParameter(boxedParam)
                addModifiers(KModifier.OVERRIDE)
                addUncheckedCast()
                returns(tvn)
                addStatement("return %T(%N) as %T", implName, boxedParam, tvn)
            }.build().also(builder::addFunction)
        }

        /* ValueObject.Boxed companion object */
        TypeSpec.companionObjectBuilder().apply {
            FunSpec.builder("invoke").apply {
                addModifiers(KModifier.OPERATOR)
                addParameter(boxedParam)
                returns(context.name)
                addStatement("return %T(%N)", implName, boxedParam)
            }.build().let(::addFunction)
            this@KDTypeForGeneration.getAnnotation<KDParsable>()
                ?.also {
                    createParseFun(boxedType, it).let(::addFunction)
                    //isParsable = true
                }

        }.build().also(builder::addType)
    }

    private fun createForkFun(properties: List<KDProperty>) {
        FunSpec.builder(KDType.Boxed.CREATE_METHOD).apply {
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
                addStatement("%N = args[$i] as %T", property.member, property.type)
            }
            addStatement("⇤}.build() as T")
            addStatement("return ret")
        }.build().also(builder::addFunction)
    }

    private fun createParseFun(boxedType: TypeName, parsable: KDParsable): FunSpec =
        FunSpec.builder(KDType.Boxed.FABRIC_PARSE_METHOD).apply {
            val srcParam = ParameterSpec.builder("src", String::class).build()
            addParameter(srcParam)
            returns(implName)
            addStatement("return %T${parsable.deserialization.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""}(%N).let(::%T)", boxedType, srcParam, implName)
        }.build()

    inline fun <reified T : Annotation> getAnnotation(): T? =
        context.annotations.filterIsInstance<T>().firstOrNull()

    override fun addNestedType(type: KDType) {
        _nestedTypes += type
    }

    override fun getKDType(name: TypeName): KDTypeSearchResult = name.toNullable(false).let { key ->
        if (key == KDType.Abstraction.TYPENAME) /*KDType.Abstraction */error("${context.name}: WIP. Abstraction not supported yet.")
        else {
            nestedTypes.find { it.name.toNullable(false) == key }?.let { it to true }
                ?: context.typeCatalog.find { it.name.toNullable(false) == key }?.let { it to false }
                    ?.also { context.logger.log("found: ${it.first}") }
                ?: run {
                    error("Can't find implementation for $key in $implName")
                }
        }
    }
}
