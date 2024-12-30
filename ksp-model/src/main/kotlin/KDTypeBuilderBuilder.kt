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
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import ru.it_arch.clean_ddd.ksp.model.DSLType.Collection

public class KDTypeBuilderBuilder private constructor(
    private val holder: KDType.Model,
    private val isDsl: Boolean,
    private val useContextReceivers: KDOptions.UseContextReceivers,
    private val logger: KDLogger
) {

    /** fun Impl.toBuilder(): Impl.Builder */
    private val toBuildersHolder =
        ToBuildersFun((holder.dslBuilderClassName.takeIf { isDsl } ?: holder.builderClassName), isDsl)

    /** class Impl.<Dsl>Builder()  */
    private val innerBuilder =
        (holder.dslBuilderClassName.takeIf { isDsl }?.let(TypeSpec::classBuilder)
            ?: TypeSpec.classBuilder(holder.builderClassName))

    /** fun Impl.Builder.build(): Impl */
    private val builderFunBuild = FunSpec.builder(KDType.Data.BUILDER_BUILD_METHOD_NAME)
        .returns(holder.className)

    init {
        val funSpecStatement = FunSpecStatement(Chunk("return %T(", holder.className))
        holder.propertyHolders.forEach { property ->
            if (property.typeName is ParameterizedTypeName) {
                funSpecStatement.addParameterForCollection(property.name, property.typeName)
            } else {
                funSpecStatement.addParameterForElement(property)

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
                    if (nestedType.first is KDType.Boxed && isDsl) (nestedType.first as KDType.Boxed).rawTypeName else property.typeName
                }.let {
                    if (!isDsl && !property.typeName.isNullable) PropertySpec.builder(property.name.simpleName, it).addModifiers(KModifier.LATEINIT)
                    else PropertySpec.builder(property.name.simpleName, it.toNullable()).initializer("null")
                }
            }.mutable().build().also(innerBuilder::addProperty)
        }

        if (isDsl) {
            //logger.log("Processing: ${holder.className.simpleName}")
            holder.nestedTypes.values.filterIsInstance<KDType.Generatable>()
                .forEach { createDslInnerBuilder(it).also(innerBuilder::addFunction) }
        }

        funSpecStatement.final()
        funSpecStatement.add(builderFunBuild)
    }

    public fun build(): TypeSpec =
        innerBuilder.addFunction(builderFunBuild.build()).build()

    public fun buildFunToBuilder(): FunSpec =
        toBuildersHolder.build()

    /**
     * 2. fun to<Dsl>Builder() { <name> = <name> }
     **/
    private fun FunSpecStatement.addParameterForElement(property: KDPropertyHolder) {
        +Chunk("%N = ", property.name)
        val element =
            holder.getKDType(property.typeName).let { DSLType.Element.create(it, property.typeName) }
        if (element.kdType is KDType.Boxed && isDsl) {
            if (element.typeName.isNullable) +Chunk("%N?.let(::${element.kdType.dslBuilderFunName(element.isInner)}),", property.name)
            else +Chunk("${element.kdType.dslBuilderFunName(element.isInner)}(%N!!),", property.name)
            toBuildersHolder.element(property.name.simpleName, element.typeName.isNullable, element.kdType.isParsable)
        } else {
            toBuildersHolder.asIs(property.name.simpleName)
            +Chunk("%N${if (!element.typeName.isNullable && isDsl) "!!" else ""},", property.name)
        }
    }

    private fun FunSpecStatement.addParameterForCollection(name: MemberName, typeName: ParameterizedTypeName): PropertySpec.Builder {
        val collectionType = typeName.toCollectionType()
        val testFlag = name.simpleName == "nestedMaps"
        return if (isDsl) {
            traverseParameterizedTypes(Collection.create(typeName, logger, testFlag), testFlag).let { substituted ->
                // return from Builder.build() { return T(...) }
                +Chunk("%N = %N${substituted.fromDslMapper}", name, name)
                endStatement()

                toBuildersHolder.fromDsl(name.simpleName, substituted.toDslMapper)

                substituted.parameterizedTypeName
                    .let { PropertySpec.builder(name.simpleName, it).initializer(collectionType.initializer(true)) }
            }
            // return new PropertySpec
        } else {
            +Chunk("%N = %N", name, name)
            endStatement()
            // toBuilder()
            toBuildersHolder.asIs(name.simpleName)
            PropertySpec.builder(name.simpleName, typeName).initializer(collectionType.initializer(false))
        }
    }

    @OptIn(ExperimentalKotlinPoetApi::class)
    private fun createDslInnerBuilder(innerType: KDType.Generatable): FunSpec =
        FunSpec.builder(innerType.dslBuilderFunName(true)).apply {
            if (innerType is KDType.Boxed) {
                ParameterSpec.builder("value", innerType.rawTypeName).build().also { param ->
                    addParameter(param)
                    addStatement("return %T.${innerType.fabricMethod}(%N)", innerType.className, param)
                }
            } else if (innerType is KDType.Model) {
                ParameterSpec.builder(
                    "block",
                    if (useContextReceivers.boxed) LambdaTypeName.get(
                        contextReceivers = listOf(innerType.dslBuilderClassName),
                        returnType = Unit::class.asTypeName()
                    ) else LambdaTypeName.get(
                        receiver = innerType.dslBuilderClassName,
                        returnType = Unit::class.asTypeName()
                    )
                ).build().also { param ->
                    addParameter(param)
                    // return InnerImpl.DslBuilder().apply(block).build()
                    addStatement("return ${KDType.Data.APPLY_BUILDER}", innerType.dslBuilderClassName, param)
                }
            }
            returns(innerType.sourceTypeName)
        }.build()

    // ValueObject.Boxed<BOXED> -> BOXED for DSL
    // https://discuss.kotlinlang.org/t/3-tailrec-questions/3981 #3
    private tailrec fun traverseParameterizedTypes(node: DSLType.Parameterized, testFlag: Boolean): DSLType.Parameterized {
        val ret = node.substituteOrNull()
        return if (ret != null) ret
        else {
            traverseParameterizedTypes(node.apply {
                substituteArgs { arg ->
                    @Suppress("NON_TAIL_RECURSIVE_CALL")
                    if (arg is ParameterizedTypeName) traverseParameterizedTypes(
                        Collection.create(arg, logger, testFlag),
                        testFlag
                    )
                    else holder.getKDType(arg).let { DSLType.Element.create(it, arg) }
                }
            }, testFlag)
        }
    }

    private class ToBuildersFun(builderTypeName: ClassName, isDsl: Boolean) {
        private val builder = FunSpec.builder("toDslBuilder".takeIf { isDsl } ?: "toBuilder")
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
        context(KDOptions)
        public fun create(holder: KDType.Model, isDsl: Boolean, logger: KDLogger): KDTypeBuilderBuilder =
            KDTypeBuilderBuilder(holder, isDsl, useContextReceivers, logger)
    }
}
