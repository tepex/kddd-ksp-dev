package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

/** Для генерации классов билдеров — обычного или DSL  */
public class KDTypeBuilderBuilder private constructor(
    private val options: KDOptions,
    private val logger: KDLogger,
    private val holder: KDType.Model,
    private val isDsl: Boolean
) {

    /** fun MyType.toBuilder(): MyTypeImpl.Builder */
    private val toBuilderHolder =
        ToBuilderFunHolder(holder.kDddTypeName, (holder.dslBuilderClassName.takeIf { isDsl } ?: holder.builderClassName), isDsl)

    /** class Impl.<Dsl>Builder()  */
    private val innerBuilder =
        (holder.dslBuilderClassName.takeIf { isDsl }?.let(TypeSpec::classBuilder)
            ?: TypeSpec.classBuilder(holder.builderClassName))

    /** fun MyTypeImpl.Builder.build(): MyType */
    private val builderFunBuild = FunSpec.builder(KDType.Data.BUILDER_BUILD_METHOD_NAME)
        //.returns(holder.kDddTypeName)
        .returns(holder.className)

    init {
        val funSpecStatement = FunSpecStatement(Chunk("return %T(", holder.className))
        holder.propertyHolders.forEach { property ->
            if (property.typeName is ParameterizedTypeName) { // Collection
                funSpecStatement.addParameterForCollection(property.name, property.typeName)
            } else {
                // Check nulls statements
                if (!property.typeName.isNullable) {
                    builderFunBuild.addStatement(
                        """${if (isDsl) "requireNotNull(%N)" else "require(::%N.isInitialized)"} { "Property '%T.%N' is not set!" }""",
                        property.name,
                        holder.className,
                        property.name
                    )
                }

                // return new PropertySpec
                holder.getKDType(property.typeName).let { nestedType ->
                    val kdType = nestedType.first

                    funSpecStatement.addParameterForElement(property)

                    if (kdType is KDType.Model && isDsl) createDslBuilder(property.name.simpleName, kdType).also(innerBuilder::addFunction)

                    if (kdType is KDType.Boxed && isDsl) (nestedType.first as KDType.Boxed).rawTypeName else property.typeName
                }.let {
                    if (!isDsl && !property.typeName.isNullable) PropertySpec.builder(property.name.simpleName, it).addModifiers(KModifier.LATEINIT)
                    else PropertySpec.builder(property.name.simpleName, it.toNullable()).initializer("null")
                }
            }.mutable().build().also(innerBuilder::addProperty)
        }

        funSpecStatement.final()
        funSpecStatement.add(builderFunBuild)
    }

    public fun build(): TypeSpec =
        innerBuilder.addFunction(builderFunBuild.build()).build()

    public fun buildFunToBuilder(): FunSpec =
        toBuilderHolder.build()

    /**
     * 2. fun to<Dsl>Builder() { <name> = <name> }
     **/
    private fun FunSpecStatement.addParameterForElement(property: KDProperty) {
        +Chunk("%N = ", property.name)
        val element =
            holder.getKDType(property.typeName).let { DSLType.Element(it, property.typeName.isNullable) }
        if (element.kdType is KDType.Boxed && isDsl) {
            // Builder.build return statement
            val parse = if (element.kdType.isParsable && element.kdType.isUseStringInDsl) ".${KDType.Boxed.FABRIC_PARSE_METHOD}" else ""
            if (element.typeName.isNullable) +Chunk("%N?.let { %T$parse(it) }, ", property.name, element.kdType.className)
            else +Chunk("%T$parse(%N!!), ", element.kdType.className, property.name)
            //logger.log("$property isParsable: ${element.kdType.isParsable} bozedType: ${element.kdType.boxedType} ")
            toBuilderHolder.element(property.name.simpleName, element.typeName.isNullable, element.kdType.isParsable && element.kdType.isUseStringInDsl)
        } else {
            toBuilderHolder.asIs(property.name.simpleName)
            +Chunk("%N${if (!element.typeName.isNullable && isDsl) "!!" else ""},", property.name)
        }
    }

    private fun FunSpecStatement.addParameterForCollection(name: MemberName, typeName: ParameterizedTypeName): PropertySpec.Builder {
        val collectionType = typeName.toCollectionType()
        return if (isDsl) {
            traverseParameterizedTypes(DSLType.Collection(typeName, logger)).let { substituted ->
                // return from Builder.build() { return T(...) }
                +Chunk("%N = %N${substituted.fromDslMapper}", name, name)
                endStatement()

                toBuilderHolder.fromDsl(name.simpleName, substituted.toDslMapper)

                substituted.parameterizedTypeName
                    .let { PropertySpec.builder(name.simpleName, it).initializer(collectionType.initializer(true)) }
            }
            // return new PropertySpec
        } else {
            +Chunk("%N = %N", name, name)
            endStatement()
            // toBuilder()
            toBuilderHolder.asIs(name.simpleName)
            PropertySpec.builder(name.simpleName, typeName).initializer(collectionType.initializer(false))
        }
    }

    @kotlin.OptIn(ExperimentalKotlinPoetApi::class)
    private fun createDslBuilder(name: String, type: KDType.Model): FunSpec =
        FunSpec.builder(createDslBuilderFunName(name, true)).apply {
            ParameterSpec.builder(
                "block",
                if (options.isUseContextParameters) LambdaTypeName.get(
                    contextReceivers = listOf(type.dslBuilderClassName),
                    returnType = Unit::class.asTypeName()
                ) else LambdaTypeName.get(
                    receiver = type.dslBuilderClassName,
                    returnType = Unit::class.asTypeName()
                )
            ).build().also { param ->
                addParameter(param)
                // return InnerImpl.DslBuilder().apply(block).build()
                addStatement("return ${KDType.Data.APPLY_BUILDER}", type.dslBuilderClassName, param)
            }
            returns(type.kDddTypeName)
        }.build()

    // ValueObject.Boxed<BOXED> -> BOXED for DSL
    // https://discuss.kotlinlang.org/t/3-tailrec-questions/3981 #3
    private tailrec fun traverseParameterizedTypes(node: DSLType.Collection): DSLType.Collection {
        val ret = node.substituteOrNull()
        return if (ret != null) ret
        else {
            traverseParameterizedTypes(node.apply {
                substituteArgs { arg ->
                    @Suppress("NON_TAIL_RECURSIVE_CALL")
                    if (arg is ParameterizedTypeName) traverseParameterizedTypes(DSLType.Collection(arg, logger))
                    else holder.getKDType(arg).let { DSLType.Element(it, arg.isNullable) }
                }
            })
        }
    }

    private fun createDslBuilderFunName(name: String, isInner: Boolean): String {
        return name
        /* TODO: for foreign reference
        ("$implClassName.${KDType.Data.DSL_BUILDER_CLASS_NAME}().".takeUnless { isInner } ?: "")
            .let { prefix -> "$prefix${name.replaceFirstChar { it.lowercaseChar() }}" }

        val ret = context.typeName.toString().substringAfterLast('.').let { name ->
            context.logger.log("For className: $className  properties: $propertyHolders")
            val implClassName = context.typeName.toString()
                .substringAfterLast("${context.packageName}.")
                .substringBefore(".$name")
                .let(context.options::toImplementationName)

            ("$implClassName.${KDType.Data.DSL_BUILDER_CLASS_NAME}().".takeUnless { isInner } ?: "")
                .let { prefix -> "$prefix${name.replaceFirstChar { it.lowercaseChar() }}" }
        }

        //context.logger.log("name: $ret")
        return ret*/
    }

    /**
     * Вспомогательный класс-holder для генерации `fun MyType.toBuilder(): MyTypeImpl.Builder` или `fun MyType.toDslBuilder(): MyTypeImpl.DslBuilder`
     *
     * @param receiverTypeName имя исходной KDDD-модели
     * @param builderTypeName тип билдера
     * @param isDsl вид билдера: false — обычный, true — DSL
     * */
    private class ToBuilderFunHolder(receiverTypeName: TypeName, builderTypeName: ClassName, isDsl: Boolean) {
        private val builder =
            FunSpec.builder("toDslBuilder".takeIf { isDsl } ?: "toBuilder")
                .receiver(receiverTypeName)
                .returns(builderTypeName)
                .addStatement("val $RET = %T()", builderTypeName)

        // toDslBuilder: ret.<name> = <name>.boxed.toString(), ret.<name> = <name>?.boxed?.toString()
        fun element(name: String, isNullable: Boolean, isCommonType: Boolean) {
            StringBuilder("$RET.$name = $name").apply {
                commonTypeOrNot(isNullable, isCommonType)
            }.toString().also(builder::addStatement)
        }

        fun asIs(name: String) {
            builder.addStatement("$RET.$name = $name")
        }

        fun fromDsl(name: String, str: String) {
            builder.addStatement("$RET.$name = $name$str")
        }

        private fun StringBuilder.commonTypeOrNot(isNullable: Boolean, isCommonType: Boolean) {
            if (isNullable) append('?')
            append(".${KDType.Boxed.PARAM_NAME}")
            if (isCommonType) {
                if (isNullable) append('?')
                append(".toString()")
            }
        }

        fun build(): FunSpec =
            builder.addStatement("return $RET").build()

        private companion object {
            const val RET = "ret"
        }
    }

    private class FunSpecStatement(_init: Chunk?) {
        private val chunks = mutableListOf<Chunk>()
        init {
            _init?.also(chunks::add)
        }

        operator fun Chunk.unaryPlus() {
            chunks += this
        }

        operator fun plusAssign(other: FunSpecStatement) {
            chunks += other.chunks
        }

        fun endStatement() {
            +Chunk(",♢")
        }

        fun final() {
            +Chunk(")")
        }

        fun add(funBuilder: FunSpec.Builder) {
            funBuilder.addStatement(
                chunks.fold(StringBuilder()) { acc, chunk -> acc.apply { append(chunk.str) } }.toString(),
                *chunks.fold(mutableListOf<Any>()) { acc, chunk -> acc.apply { addAll(chunk.args) } }.toTypedArray()
            )
        }
    }

    private class Chunk(val str: String, vararg params: Any) {
        val args = params.toList()
    }

    public companion object {
        context(options: KDOptions, logger: KDLogger)
        public operator fun invoke(holder: KDType.Model, isDsl: Boolean): KDTypeBuilderBuilder =
            KDTypeBuilderBuilder(options, logger, holder, isDsl)
    }
}
