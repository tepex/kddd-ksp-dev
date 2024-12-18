package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import ru.it_arch.kddd.ValueObject
import java.io.File
import java.net.URI
import java.util.UUID

public sealed interface KDType {
    public val sourceTypeName: TypeName

    public class Sealed private constructor(
        override val sourceTypeName: TypeName
    ) : KDType {
        public companion object {
            public fun create(typeName: TypeName): Sealed =
                Sealed(typeName)
        }
    }

    public data object Abstraction : KDType {
        override val sourceTypeName: TypeName = ValueObject::class.asTypeName()
        val TYPENAME: TypeName = ValueObject::class.asTypeName()

        override fun toString(): String = "Abstraction"
    }

    public interface Model : Generatable {
        public val builderClassName: ClassName
        public val dslBuilderClassName: ClassName
    }

    public interface Generatable : KDType {
        public val className: ClassName
        public val builder: TypeSpec.Builder
        public val parameters: List<KDParameter>
        public val nestedTypes: Map<TypeName, KDType>

        public fun addNestedType(type: KDType)
        public fun getKDType(typeName: TypeName): KDType
    }

    public class Data private constructor(
        private val forGeneration: KDTypeForGeneration
    ) : Generatable by forGeneration, Model {

        override val builderClassName: ClassName = ClassName.bestGuess("${className.simpleName}.$BUILDER_CLASS_NAME")
        override val dslBuilderClassName: ClassName = ClassName.bestGuess("${className.simpleName}.$DSL_BUILDER_CLASS_NAME")

        override fun toString(): String =
            "Data($className)"

        public companion object {
            public const val BUILDER_CLASS_NAME: String = "Builder"
            public const val DSL_BUILDER_CLASS_NAME: String = "DslBuilder"
            public const val BUILDER_BUILD_METHOD_NAME: String = "build"
            public const val APPLY_BUILDER: String = "%T().apply(%N).$BUILDER_BUILD_METHOD_NAME()"

            public fun create(helper: KDTypeHelper, isEntity: Boolean): Data =
                Data(KDTypeForGeneration(helper, null, isEntity))
        }
    }

    public class IEntity private constructor(private val data: Data) : Model by data {
        public fun generateBaseContract() {
            val paramId = parameters.find { it.name.simpleName == ID_NAME }
                ?: error("ID parameter not found for Entity $className")

            FunSpec.builder("hashCode").apply {
                addModifiers(KModifier.OVERRIDE)
                addStatement("return %N.hashCode()", paramId.name)
                returns(Int::class)
            }.build().also(builder::addFunction)

            val paramOther = ParameterSpec.builder("other", ANY.toNullable()).build()
            FunSpec.builder("equals").apply {
                addModifiers(KModifier.OVERRIDE)
                addParameter(paramOther)
                addStatement("if (this === other) return true")
                addStatement("if (%N !is %T) return false", paramOther, className)
                addStatement("if (%N != %N.%N) return false", paramId.name, paramOther, paramId.name)
                addStatement("return true")
                returns(Boolean::class)
            }.build().also(builder::addFunction)

            // override fun toString()

            parameters.filter { it.name.simpleName != ID_NAME }
                .fold(mutableListOf<Pair<String, MemberName>>()) { acc, param -> acc.apply { add("%N: $%N" to param.name) } }
                .let { it.joinToString { pair -> pair.first } to it.fold(mutableListOf(paramId.name)) { acc, pair ->
                    acc.apply {
                        add(pair.second)
                        add(pair.second)
                    }
                } }.also { pair ->
                    FunSpec.builder("toString").apply {
                        addModifiers(KModifier.OVERRIDE)
                        returns(String::class)
                        addStatement("return \"[ID: $%N, ${pair.first}]\"", *pair.second.toTypedArray())
                    }.build().also(builder::addFunction)
                }
        }

        public companion object {
            public const val ID_NAME: String = "id"

            public fun create(helper: KDTypeHelper): IEntity =
                Data.create(helper, true).let(KDType::IEntity)
        }
    }

    public class Boxed private constructor(
        private val forGeneration: KDTypeForGeneration,
        private val boxedType: TypeName,
    ) : Generatable by forGeneration, KDType {

        override fun toString(): String =
            "KDType.Boxed<$boxedType>"

        public val isParsable: Boolean =
            PARSABLES.containsKey(boxedType)

        public val fabricMethod: String =
            FABRIC_PARSE_METHOD.takeIf { isParsable } ?: FABRIC_CREATE_METHOD

        public val rawTypeName: TypeName =
            boxedType.takeUnless { isParsable } ?: String::class.asTypeName()

        public companion object {
            public const val PARAM_NAME: String = "boxed"
            public const val FABRIC_CREATE_METHOD: String = "create"
            public const val FABRIC_PARSE_METHOD: String = "parse"
            public const val CREATE_METHOD: String = "copy"

            public val PARSABLES: Map<ClassName, String> = mapOf(
                URI::class.asTypeName() to ".create",
                UUID::class.asTypeName() to ".fromString",
                File::class.asTypeName() to ""
            )

            public fun create(helper: KDTypeHelper, superInterfaceName: TypeName): Boxed = run {
                require(superInterfaceName is ParameterizedTypeName && superInterfaceName.typeArguments.size == 1) {
                    "Class name `$superInterfaceName` expected type parameter"
                }
                superInterfaceName.typeArguments.first().let { Boxed(KDTypeForGeneration(helper, it), it) }
            }
        }
    }
}
