package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import ru.it_arch.clean_ddd.ksp.model.KDReference.Collection

public class KDTypeBuilderBuilder private constructor(
    private val holder: KDType.Model,
    private val isDsl: Boolean,
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
        //holder.propertyHolders.map(::toBuilderPropertySpec).also(innerBuilder::addProperties)
        holder.propertyHolders.forEach { property ->
            property.typeReference.let { ref ->
                when (ref) {
                    is KDReference.Element -> {
                        // Builder().return
                        property.typeReference.typeName.toKDReference(holder)
                            .also { funSpecStatement.addParameterForElement(property.name, it as KDReference.Element) }

                        // return new PropertySpec
                        holder.getKDType(ref.typeName).let { nestedType ->
                            if (nestedType is KDType.Boxed && isDsl) nestedType.rawTypeName else ref.typeName
                        }.let { PropertySpec.builder(property.name.simpleName, it.toNullable()).initializer("null") }
                    }

                    is KDReference.Parameterized -> funSpecStatement.addParameterForCollection(property.name, ref as Collection)

                }.mutable().build().also(innerBuilder::addProperty)
            }

            // Check nulls statements
            if (property.typeReference  is KDReference.Element && !property.typeReference.typeName.isNullable )
                builderFunBuild.addStatement(
                    """requireNotNull(%N) { "Property '%T.%N' is not set!" }""", property.name, holder.className, property.name)
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
    private fun FunSpecStatement.addParameterForElement(name: MemberName, ref: KDReference.Element) {
        +Chunk("%N = ", name)
        if (ref.kdType is KDType.Boxed && isDsl) {
            val boxedType = (ref.kdType as KDType.Boxed)
            if (ref.typeName.isNullable) +Chunk("%N?.let(::${boxedType.dslBuilderFunName}),", name)
            else +Chunk("${boxedType.dslBuilderFunName}(%N!!),", name)
            toBuildersHolder.element(name.simpleName, ref.typeName.isNullable, boxedType.isParsable)
        } else {
            toBuildersHolder.asIs(name.simpleName)
            +Chunk("%N${if (!ref.typeName.isNullable) "!!" else ""},", name)
        }
    }

    private fun FunSpecStatement.addParameterForCollection(name: MemberName, collection: Collection) =
        if (isDsl) traverseParameterizedTypes(Collection.create(collection.parameterizedTypeName)).let { substituted ->
            // return from Builder.build() { return T(...) }
            +Chunk("%N = %N${substituted.fromDslMapper}", name, name)
            endStatement()

            toBuildersHolder.fromDsl(name.simpleName, substituted.toDslMapper)

            substituted.parameterizedTypeName
                .let { PropertySpec.builder(name.simpleName, it).initializer(collection.collectionType.mutableInitializer) }
        }
        // return new PropertySpec
        else {
            +Chunk("%N = %N", name, name)
            endStatement()
            // toBuilder()
            toBuildersHolder.asIs(name.simpleName)
            PropertySpec.builder(name.simpleName, collection.parameterizedTypeName).initializer(collection.collectionType.initializer)
        }

    // ValueObject.Boxed<BOXED> -> BOXED for DSL
    // https://discuss.kotlinlang.org/t/3-tailrec-questions/3981 #3
    private tailrec fun traverseParameterizedTypes(node: KDReference.Parameterized): KDReference.Parameterized =
        node.substituteOrNull() ?: traverseParameterizedTypes(node.apply {
            substituteArgs { arg ->
                @Suppress("NON_TAIL_RECURSIVE_CALL")
                arg.toKDReference(holder).let { if (it is Collection) traverseParameterizedTypes(it) else it }
            }
        })

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

        fun toSet() {
            +Chunk(".toSet()")
        }

        fun toList() {
            +Chunk(".toList()")
        }

        fun toMap() {
            +Chunk(".toMap()")
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

        public fun create(holder: KDType.Model, isDsl: Boolean, logger: KDLogger): KDTypeBuilderBuilder =
            KDTypeBuilderBuilder(holder, isDsl, logger)

        private fun createDslInnerBuilder(innerType: KDType.Generatable): FunSpec =
            FunSpec.builder(innerType.dslBuilderFunName).apply {
                if (innerType is KDType.Boxed) {
                    ParameterSpec.builder("value", innerType.rawTypeName).build().also { param ->
                        addParameter(param)
                        addStatement("return %T.${innerType.fabricMethod}(%N)", innerType.className, param)
                    }
                } else if (innerType is KDType.Model) {
                    ParameterSpec.builder(
                        "block",
                        LambdaTypeName.get(
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

        /** BOXED or Enum or lambda */
        private fun createDslBuilderParameter(name: String, nestedType: KDReference.Element) = when (nestedType.kdType) {
            is KDType.Sealed ->
                ParameterSpec.builder(name, nestedType.kdType.sourceTypeName).build()
            is KDType.Boxed ->
                (nestedType.kdType as KDType.Boxed).rawTypeName.let { type -> type.takeIf { nestedType.typeName.isNullable }?.toNullable() ?: type }
                    .let { ParameterSpec.builder(name, it).build() }

            is KDType.Model -> ParameterSpec.builder(
                name,
                LambdaTypeName.get(
                    receiver = (nestedType.kdType as KDType.Model).dslBuilderClassName,
                    returnType = Unit::class.asTypeName()
                )
            ).build()

            else -> error("Impossible state")
        }

        /**
         * DSL builder for param: ValueObject, param: List<ValueObject.Boxed>
         * `fun <t>(block: <T>Impl.DslBuilder.() -> Unit) { ... }`
         *
         * @param nullableHolder type is KDValueObject
         **/
        /*
        private fun createDslBuilderFunction(name: MemberName, ref: KDReference.Element, isCollection: Boolean) =
            createDslBuilderParameter("p1", ref).let { blockParam ->
                FunSpec.builder(name.simpleName).apply {
                    addParameter(blockParam)
                    val dslBuilderClassName = (ref.kdType as KDType.Data).dslBuilderClassName
                    if (isCollection)
                        addStatement("${KDType.Data.APPLY_BUILDER}.also(%N::add)", dslBuilderClassName, blockParam, name)
                    else
                        addStatement("%N = ${KDType.Data.APPLY_BUILDER}", name, dslBuilderClassName, blockParam)
                }.build()
            }
*/
    }
}
