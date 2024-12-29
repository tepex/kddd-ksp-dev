package ru.it_arch.clean_ddd.ksp.model

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
import ru.it_arch.clean_ddd.ksp.model.KDType.Boxed
import ru.it_arch.clean_ddd.ksp.model.KDType.Generatable
import ru.it_arch.kddd.ValueObject

internal class KDTypeForGeneration(
    private val context: KDTypeContext,
    boxedType: TypeName? = null,
    isEntity: Boolean = false
) : Generatable {
    override val className = context.toBeGenerated
    override val builder = TypeSpec.classBuilder(className).addSuperinterface(context.typeName)
    override val propertyHolders: List<KDPropertyHolder>
    override val sourceTypeName: TypeName = context.typeName
    //override val packageName: String = helper.packageName

    private val _nestedTypes = mutableMapOf<TypeName, KDType>()
    override val nestedTypes: Map<TypeName, KDType>
        get() = _nestedTypes.toMap()

    override fun dslBuilderFunName(isInner: Boolean): String =
        context.typeName.toString().substringAfterLast('.').let { name ->
            val impl = with(context.options) {
                context.typeName.toString().substringAfterLast("${context.packageName}.").substringBefore(".$name")
                    .implementationName
            }
            ("$impl.${KDType.Data.DSL_BUILDER_CLASS_NAME}().".takeUnless { isInner } ?: "")
                .let { prefix -> "$prefix${name.replaceFirstChar { it.lowercaseChar() }}" }
        }

    init {
        propertyHolders = boxedType?.let {
            boxedType(boxedType)
            listOf(KDPropertyHolder.create(className.member(Boxed.PARAM_NAME), boxedType)).also(::createConstructor)
        } ?: run {
            // not Boxed
            if (!isEntity) {
                builder.addModifiers(KModifier.DATA)
                builder.addAnnotation(ConsistentCopyVisibility::class)
            }
            context.properties
                .map(KDPropertyHolder::create)
                .also(::createConstructor)
        }
        //propertyHolders.filter { it in  }
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

        /*  override fun <T : ValueObjectSingle<String>> copy(value: String): T = NameImpl(value) as T */
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

        /* ValueObjectSingle companion object */
        TypeSpec.companionObjectBuilder().apply {
            FunSpec.builder(Boxed.FABRIC_CREATE_METHOD).apply {
                addParameter(boxedParam)
                returns(className)
                addStatement("return %T(%N)", className, boxedParam)
            }.build().let(::addFunction)
            createParseFun(boxedType)?.let(::addFunction)
        }.build().also(builder::addType)
    }

    private fun createParseFun(boxedType: TypeName): FunSpec? =
        Boxed.PARSABLES[boxedType]?.let { creator ->
            FunSpec.builder(Boxed.FABRIC_PARSE_METHOD).apply {
                val srcParam = ParameterSpec.builder("src", String::class).build()
                addParameter(srcParam)
                returns(className)
                addStatement("return %T$creator(%N).let(::${Boxed.FABRIC_CREATE_METHOD})", boxedType, srcParam)
            }.build()
        }

    override fun addNestedType(type: KDType) {
        _nestedTypes[type.sourceTypeName.toNullable(false)] = type
    }

    override fun getKDType(typeName: TypeName) = typeName.toNullable(false).let { key ->
        if (key == KDType.Abstraction.TYPENAME) /*KDType.Abstraction */error("${context.typeName}: WIP. Abstraction not supported yet.")
        else _nestedTypes[key]?.let { it to true } ?: context.globalKDTypes[key]?.let { it to false } ?: run {
            // to KDType.List, KDType.Set, KDType.Map
            error("Can't find implementation for $key in $className")
        }
    }

    private fun createConstructor(parameters: List<KDPropertyHolder>) {
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
