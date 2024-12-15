package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import ru.it_arch.kddd.ValueObject

internal sealed interface KDType {
    class Sealed private constructor(val typeName: TypeName) : KDType {
        companion object {
            fun create(typeName: TypeName) =
                Sealed(typeName)
        }
    }

    interface Model : HasImpl {
        val builderClassName: ClassName
        val dslBuilderClassName: ClassName
    }

    interface HasImpl : KDType {
        val className: ClassName
        val builder: TypeSpec.Builder
        val parameters: List<KDParameter>

        fun addNestedType(key: TypeName, type: KDType)
        fun getNestedType(typeName: TypeName): KDType
    }

    class Data private constructor(
        private val impl: Impl
    ) : HasImpl by impl, Model {

        override val builderClassName = ClassName.bestGuess("${className.simpleName}.$BUILDER_CLASS_NAME")
        override val dslBuilderClassName = ClassName.bestGuess("${className.simpleName}.$DSL_BUILDER_CLASS_NAME")

        companion object {
            const val BUILDER_CLASS_NAME = "Builder"
            const val DSL_BUILDER_CLASS_NAME = "DslBuilder"
            const val BUILDER_BUILD_METHOD_NAME = "build"
            const val APPLY_BUILDER = "%T().apply(%N).$BUILDER_BUILD_METHOD_NAME()"


            fun create(helper: KDTypeHelper, isEntity: Boolean) =
                Data(Impl(helper, null, isEntity))
        }
    }

    class IEntity private constructor(private val data: Data) : Model by data {
        companion object {
            fun create(helper: KDTypeHelper) =
                Data.create(helper, true).let(KDType::IEntity)
        }
    }

    class Boxed private constructor(
        private val impl: Impl,
        val boxedType: TypeName,
    ) : HasImpl by impl, KDType {

        override fun toString(): String =
            "KDType.Boxed<$boxedType>"

        companion object {
            const val PARAM_NAME = "boxed"
            const val FABRIC_CREATE_METHOD = "create"
            const val CREATE_METHOD = "copy"

            fun create(helper: KDTypeHelper, superInterfaceName: TypeName): Boxed {
                require(superInterfaceName is ParameterizedTypeName && superInterfaceName.typeArguments.size == 1) {
                    "Class name `$superInterfaceName` expected type parameter"
                }
                val boxed = superInterfaceName.typeArguments.first()
                return Boxed(Impl(helper, boxed), boxed)
            }
        }
    }

    private class Impl(helper: KDTypeHelper, boxedType: TypeName? = null, isEntity: Boolean = false) : HasImpl {
        override val className = helper.implClassName
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

                    val valueParam = ParameterSpec.builder(Boxed.PARAM_NAME, boxedType).build()
                    FunSpec.builder("toString")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class)
                        .addStatement("return %N.toString()", valueParam)
                        .build()
                        .also(builder::addFunction)

                    /*  override fun <T : ValueObjectSingle<String>> copy(value: String): T = NameImpl(value) as T */

                    TypeVariableName(
                        "T",
                        ValueObject.Boxed::class.asTypeName().parameterizedBy(boxedType)
                    ).also { tvn ->
                        FunSpec.builder(Boxed.CREATE_METHOD)
                            .addTypeVariable(tvn)
                            .addParameter(valueParam)
                            .addModifiers(KModifier.OVERRIDE)
                            .addUncheckedCast()
                            .returns(tvn)
                            .addStatement("return %T(%N) as %T", className, valueParam, tvn)
                            .build()
                            .also(builder::addFunction)
                    }

                    /* ValueObjectSingle companion object */

                    FunSpec.builder(Boxed.FABRIC_CREATE_METHOD)
                        .addParameter(valueParam)
                        .returns(className)
                        .addStatement("return %T(%N)", className, valueParam)
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
            if (type is HasImpl) builder.addType(type.builder.build())
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
}
