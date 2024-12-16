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
    helper: KDTypeHelper,
    boxedType: TypeName? = null,
    isEntity: Boolean = false
) : Generatable {
    override val className = helper.toBeGenerated
    override val builder = TypeSpec.classBuilder(className).addSuperinterface(helper.typeName)
    override val parameters: List<KDParameter>

    private val nestedTypes = mutableMapOf<TypeName, KDType>()

    init {
        parameters = boxedType?.let {
            builder.addModifiers(KModifier.VALUE)
            builder.addAnnotation(JvmInline::class)

            listOf(
                KDParameter.create(className.member(Boxed.PARAM_NAME), boxedType)
            ).apply {
                /* value class constructor */
                createConstructor(this)

                val boxedParam = ParameterSpec.builder(Boxed.PARAM_NAME, boxedType).build()
                FunSpec.builder("toString")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(String::class)
                    .addStatement("return %N.toString()", boxedParam)
                    .build()
                    .also(builder::addFunction)

                /*  override fun <T : ValueObjectSingle<String>> copy(value: String): T = NameImpl(value) as T */

                TypeVariableName(
                    "T",
                    ValueObject.Boxed::class.asTypeName().parameterizedBy(boxedType)
                ).also { tvn ->
                    FunSpec.builder(Boxed.CREATE_METHOD)
                        .addTypeVariable(tvn)
                        .addParameter(boxedParam)
                        .addModifiers(KModifier.OVERRIDE)
                        .addUncheckedCast()
                        .returns(tvn)
                        .addStatement("return %T(%N) as %T", className, boxedParam, tvn)
                        .build()
                        .also(builder::addFunction)
                }

                /* ValueObjectSingle companion object */

                FunSpec.builder(Boxed.FABRIC_CREATE_METHOD)
                    .addParameter(boxedParam)
                    .returns(className)
                    .addStatement("return %T(%N)", className, boxedParam)
                    .build()
                    .let { TypeSpec.companionObjectBuilder().addFunction(it).build() }
                    .also(builder::addType)
            }
        } ?: run {
            // not Boxed
            builder.addAnnotation(ConsistentCopyVisibility::class)
            if (!isEntity) builder.addModifiers(KModifier.DATA)
            helper.properties
                .map(KDParameter::create)
                .also(::createConstructor)
        }
    }

    override fun addNestedType(key: TypeName, type: KDType) {
        nestedTypes[key.toNullable(false)] = type
        if (type is Generatable) builder.addType(type.builder.build())
    }

    override fun getNestedType(typeName: TypeName) =
        nestedTypes[typeName.toNullable(false)] ?: error("Can't find implementation for $typeName in $className")

    private fun createConstructor(parameters: List<KDParameter>) {
        parameters.map { param ->
            PropertySpec
                .builder(param.name.simpleName, param.typeReference.typeName, KModifier.OVERRIDE)
                .initializer(param.name.simpleName)
                .build()
        }.also(builder::addProperties)

        FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addParameters(parameters.map { ParameterSpec(it.name.simpleName, it.typeReference.typeName) })
            .addStatement("validate()")
            .build()
            .also(builder::primaryConstructor)
    }
}
