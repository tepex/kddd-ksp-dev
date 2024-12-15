package ru.it_arch.clean_ddd.ksp.model

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

public sealed interface KDType {
    public class Sealed private constructor(public val typeName: TypeName) : KDType {
        public companion object {
            public fun create(typeName: TypeName): Sealed =
                Sealed(typeName)
        }
    }

    public interface Model : Generatable {
        public val builderClassName: ClassName
        public val dslBuilderClassName: ClassName
    }

    public interface Generatable : KDType {
        public val className: ClassName
        public val builder: TypeSpec.Builder
        public val parameters: List<KDParameter>

        public fun addNestedType(key: TypeName, type: KDType)
        public fun getNestedType(typeName: TypeName): KDType
    }

    public class Data private constructor(
        private val forGeneration: ForGeneration
    ) : Generatable by forGeneration, Model {

        override val builderClassName: ClassName = ClassName.bestGuess("${className.simpleName}.$BUILDER_CLASS_NAME")
        override val dslBuilderClassName: ClassName = ClassName.bestGuess("${className.simpleName}.$DSL_BUILDER_CLASS_NAME")

        public companion object {
            public const val BUILDER_CLASS_NAME: String = "Builder"
            public const val DSL_BUILDER_CLASS_NAME: String = "DslBuilder"
            public const val BUILDER_BUILD_METHOD_NAME: String = "build"
            public const val APPLY_BUILDER: String = "%T().apply(%N).$BUILDER_BUILD_METHOD_NAME()"

            public fun create(helper: KDTypeHelper, isEntity: Boolean): Data =
                Data(ForGeneration(helper, null, isEntity))
        }
    }

    public class IEntity private constructor(private val data: Data) : Model by data {
        public companion object {
            public const val ID_NAME: String = "id"

            public fun create(helper: KDTypeHelper): IEntity =
                Data.create(helper, true).let(KDType::IEntity)
        }
    }

    public class Boxed private constructor(
        private val forGeneration: ForGeneration,
        public val boxedType: TypeName,
    ) : Generatable by forGeneration, KDType {

        override fun toString(): String =
            "KDType.Boxed<$boxedType>"

        public companion object {
            public const val PARAM_NAME: String = "boxed"
            public const val FABRIC_CREATE_METHOD: String = "create"
            public const val CREATE_METHOD: String = "copy"

            public fun create(helper: KDTypeHelper, superInterfaceName: TypeName): Boxed {
                require(superInterfaceName is ParameterizedTypeName && superInterfaceName.typeArguments.size == 1) {
                    "Class name `$superInterfaceName` expected type parameter"
                }
                val boxed = superInterfaceName.typeArguments.first()
                return Boxed(ForGeneration(helper, boxed), boxed)
            }
        }
    }

    private class ForGeneration(helper: KDTypeHelper, boxedType: TypeName? = null, isEntity: Boolean = false) : Generatable {
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
}
