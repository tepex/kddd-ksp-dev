package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
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
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.ddd.ValueObjectSingle

internal sealed interface KDType {
    class KDValueObjectBase private constructor(val typeName: TypeName) : KDType {
        companion object {
            const val CLASSNAME = "ru.it_arch.ddd.ValueObjectBase"

            fun create(typeName: TypeName) =
                KDValueObjectBase(typeName)
        }
    }

    class KDValueObject private constructor(
        declaration: KSClassDeclaration
    ) : Impl(declaration), KDType {

        val builderClassName = ClassName.bestGuess("${className.simpleName}.$BUILDER_CLASS_NAME")
        val dslBuilderClassName = ClassName.bestGuess("${className.simpleName}.$DSL_BUILDER_CLASS_NAME")

        companion object {
            const val CLASSNAME = "ru.it_arch.ddd.ValueObject"
            const val BUILDER_CLASS_NAME = "Builder"
            const val DSL_BUILDER_CLASS_NAME = "DslBuilder"
            const val BUILDER_BUILD_METHOD_NAME = "build"
            const val APPLY_BUILDER = "%T().apply(%N).$BUILDER_BUILD_METHOD_NAME()"

            fun create(declaration: KSClassDeclaration) = KDValueObject(declaration)
        }
    }

    class KDValueObjectSingle private constructor(
        declaration: KSClassDeclaration,
        val boxedType: TypeName,
    ) : Impl(declaration, boxedType), KDType {

        override fun toString(): String =
            "KDValueObjectSingle<$boxedType>"

        companion object {
            const val CLASSNAME = "ru.it_arch.ddd.ValueObjectSingle"
            const val PARAM_NAME = "value"
            const val FABRIC_CREATE_METHOD = "create"
            const val CREATE_METHOD = "copy"

            fun create(declaration: KSClassDeclaration, superInterfaceName: TypeName): KDValueObjectSingle {
                require(superInterfaceName is ParameterizedTypeName && superInterfaceName.typeArguments.size == 1) {
                    "Class name `$superInterfaceName` expected type parameter"
                }
                return KDValueObjectSingle(declaration, superInterfaceName.typeArguments.first())
            }
        }
    }

    open class Impl(declaration: KSClassDeclaration, boxedType: TypeName? = null) {
        val className = declaration.toClassNameImpl()
        val builder = TypeSpec.classBuilder(className)
            .addSuperinterface(declaration.asType(emptyList()).toTypeName())
        val parameters: List<KDParameter>

        private val nestedTypes = mutableMapOf<TypeName, KDType>()

        init {
            parameters = boxedType?.let {
                //builder.addModifiers(KModifier.PRIVATE)
                builder.addModifiers(KModifier.VALUE)
                builder.addAnnotation(JvmInline::class)

                listOf(
                    KDParameter.create(className.member(KDValueObjectSingle.PARAM_NAME), boxedType)
                ).apply {
                    /* value class constructor */
                    createConstructor(this)

                    val valueParam = ParameterSpec.builder(KDValueObjectSingle.PARAM_NAME, boxedType).build()
                    FunSpec.builder("toString")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class)
                        .addStatement("return %N.toString()", valueParam)
                        .build()
                        .also(builder::addFunction)

                    /*  override fun <T : ValueObjectSingle<String>> copy(value: String): T = NameImpl(value) as T */

                    TypeVariableName(
                        "T",
                        ValueObjectSingle::class.asTypeName().parameterizedBy(boxedType)
                    ).also { tvn ->
                        FunSpec.builder(KDValueObjectSingle.CREATE_METHOD)
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

                    FunSpec.builder(KDValueObjectSingle.FABRIC_CREATE_METHOD)
                        .addParameter(valueParam)
                        .returns(className)
                        .addStatement("return %T(%N)", className, valueParam)
                        .build()
                        .let { TypeSpec.companionObjectBuilder().addFunction(it).build() }
                        .also(builder::addType)
                }
            } ?: run {
                builder
                    .addModifiers(KModifier.DATA)
                    .addAnnotation(ConsistentCopyVisibility::class)
                declaration.getAllProperties()
                    .map { KDParameter.create(className.member(it.simpleName.asString()), it) }
                    .toList().also(::createConstructor)
            }
        }

        fun addNestedType(key: TypeName, type: KDType) {
            nestedTypes[key.toNullable(false)] = type
            if (type is Impl) builder.addType(type.builder.build())
        }

        fun getNestedType(typeName: TypeName) =
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

    companion object {
        fun create(declaration: KSClassDeclaration, logger: KSPLogger): KDType? {
            declaration.superTypes.forEach { parent ->
                val fullName = parent.resolve().declaration.let { "${it.packageName.asString()}.${it.simpleName.asString()}" }
                when(fullName) {
                    KDValueObjectBase.CLASSNAME -> KDValueObjectBase.create(declaration.asType(emptyList()).toTypeName())
                    KDValueObject.CLASSNAME -> KDValueObject.create(declaration)
                    KDValueObjectSingle.CLASSNAME -> {
                        runCatching { KDValueObjectSingle.create(declaration, parent.toTypeName()) }.getOrElse {
                            logger.warn(it.message ?: "Cant parse parent type $parent")
                            null
                        }
                    }
                    else -> null
                }?.also { return it }
            }
            return null
        }
    }
}
