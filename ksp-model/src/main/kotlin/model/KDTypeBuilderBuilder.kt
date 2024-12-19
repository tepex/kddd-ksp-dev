package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import ru.it_arch.clean_ddd.ksp.model.KDPropertyHolder.KDReference.Collection.CollectionType.LIST
import ru.it_arch.clean_ddd.ksp.model.KDPropertyHolder.KDReference.Collection.CollectionType.MAP
import ru.it_arch.clean_ddd.ksp.model.KDPropertyHolder.KDReference.Collection.CollectionType.SET

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
        holder.propertyHolders.map(::toBuilderPropertySpec).also(innerBuilder::addProperties)

        // Check nulls statements
        holder.propertyHolders
            .filter { it.typeReference is KDPropertyHolder.KDReference.Element && !it.typeReference.typeName.isNullable }
            .forEach { p ->
                builderFunBuild
                    .addStatement(
                        """requireNotNull(%N) { "Property '%T.%N' is not set!" }""",
                        p.name,
                        holder.className,
                        p.name
                    )
            }

        val funSpecStatement = FunSpecStatement(Chunk("return %T(", holder.className))
        holder.propertyHolders.forEach { param ->
            when (param.typeReference) {
                is KDPropertyHolder.KDReference.Element -> holder.getKDType(param.typeReference.typeName)
                    .also { funSpecStatement.addParameterForElement(param.name, it, param.typeReference.typeName.isNullable) }

                is KDPropertyHolder.KDReference.Collection -> param.typeReference.parameterizedTypeName.typeArguments
                     // recursive
                    .map { NullableHolder(holder.getKDType(it), it.isNullable) }


                    .also { funSpecStatement.addParameterForCollection(param.name, param.typeReference.collectionType, it) }
            }
        }
        funSpecStatement.final()
        funSpecStatement.add(builderFunBuild)
    }

    public fun build(): TypeSpec =
        innerBuilder.addFunction(builderFunBuild.build()).build()

    public fun buildFunToBuilder(): FunSpec =
        toBuildersHolder.build()

    /**
     * 1. <Dsl>Builder.build() { return T(<name> = <name>) }
     *    DslBuilder.build(): <name> = <T>.parse(<name>!!), <name> = <name>?.let(<T>::parse),
     * 2. fun to<Dsl>Builder() { <name> = <name> }
     **/
    private fun FunSpecStatement.addParameterForElement(name: MemberName, nestedType: KDType, isNullable: Boolean) {
        +Chunk("%N = ", name)
        if (nestedType is KDType.Boxed && isDsl) {
            if (isNullable) +Chunk("%N?.let(%T::${nestedType.fabricMethod}),", name, nestedType.className)
            else +Chunk("%T.${nestedType.fabricMethod}(%N!!),", nestedType.className, name)
            toBuildersHolder.element(name.simpleName, isNullable, nestedType.isParsable)
        } else {
            if (isDsl && nestedType is KDType.Data)
                createDslBuilderFunction(name, NullableHolder(nestedType, isNullable), false).also(innerBuilder::addFunction)
            toBuildersHolder.asIs(name.simpleName)
            +Chunk("%N${if (!isNullable) "!!" else ""},", name)
        }
    }

    // return constructor(...)
    // recursive to immutable
    private fun FunSpecStatement.addParameterForCollection(
        name: MemberName,
        collectionType: KDPropertyHolder.KDReference.Collection.CollectionType,
        parametrized: List<NullableHolder>,
    ) {
        +Chunk("%N = %N", name, name)
        when (collectionType) {
            LIST, SET -> parametrized.first().also { wrapper ->
                if (wrapper.type is KDType.Boxed && isDsl) {
                    toBuildersHolder.listOrSet(name.simpleName, wrapper.isNullable, wrapper.type.isParsable,collectionType == SET)
                    +Chunk(".map")
                    if (wrapper.isNullable ) +Chunk(" { it?.let(%T::${wrapper.type.fabricMethod}) }", wrapper.type.className)
                    else +Chunk("(%T::${wrapper.type.fabricMethod})", wrapper.type.className)
                    if (collectionType == SET) toSet()
                } else { // !Boxed || !isDsl
                    if (isDsl) {
                        if (wrapper.type is KDType.Model) {
                            toBuildersHolder.mutableListOrSet(name.simpleName, collectionType == SET)
                            createDslBuilderFunction(name, wrapper, true).also(innerBuilder::addFunction)
                        }
                        else toBuildersHolder.asIs(name.simpleName)
                        if (collectionType == LIST) toList() else toSet()
                    } else toBuildersHolder.asIs(name.simpleName)
                }
            }
            MAP -> {
                if (parametrized.any { it.type is KDType.Data } && isDsl)
                    createDslBuilderForMap(name, parametrized[0], parametrized[1]).also(innerBuilder::addFunction)

                if (parametrized.all { it.type !is KDType.Boxed } || !isDsl) {
                    if (isDsl) {
                        toMap()
                        toBuildersHolder.mutableMap(name.simpleName)
                    }
                } else { // has BOXED && isDSL
                    toBuildersHolder.map(name.simpleName, parametrized)
                    //logger.log("${name.simpleName}, parametrized: $parametrized")
                    +Chunk(".entries.associate { ")
                    +parametrized[0].toStatementTemplate("it.key")
                    +Chunk(" to ")
                    +parametrized[1].toStatementTemplate("it.value")
                    +Chunk(" }")
                }
            }
        }
        endStatement()
    }

    /**
     * KDType{ValueObjectSingle<BOXED>} -> BOXED
     * KDType{ValueObject} -> ValueObject
     *
     * List<KDType{ValueObjectSingle<BOXED>}>, Set<KDType{ValueObjectSingle<BOXED>}> -> List<BOXED>, Set<BOXED>
     * Map<KDType{ValueObjectSingle<BOXED1>}, KDType{ValueObjectSingle<BOXED2>}> -> Map<BOXED1, BOXED2>
     *
     * List<KDType{ValueObject}>, Set<KDType{ValueObject}>        -> MutableList<ValueObject>, MutableSet<ValueObject>
     * Map<KDType{ValueObject}, KDType{ValueObject}>              -> MutableMap<ValueObject, ValueObject>
     * !!!
     * Map<KDType{ValueObject}, KDType{ValueObjectSingle<BOXED>}> -> MutableMap<ValueObject, ValueObjectSingle<BOXED>>
     * Map<KDType{ValueObjectSingle<BOXED>, KDType{ValueObject}>  -> MutableMap<ValueObjectSingle<BOXED>, ValueObject>
     * */
    private fun toBuilderPropertySpec(param: KDPropertyHolder) = param.typeReference.let { ref ->
        when (ref) {
            is KDPropertyHolder.KDReference.Element -> holder.getKDType(ref.typeName).let { nestedType ->
                if (nestedType is KDType.Boxed && isDsl) nestedType.rawTypeName else ref.typeName
            }.let { PropertySpec.builder(param.name.simpleName, it.toNullable()).initializer("null") }

            is KDPropertyHolder.KDReference.Collection -> if (isDsl) {
                // ValueObject.Boxed<BOXED> -> BOXED for DSL
                val newArgs = ref.parameterizedTypeName.typeArguments.map { paramTypeName ->

                        // recursion search and replace

                    holder.getKDType(paramTypeName)
                        .let { if (it is KDType.Boxed) it.rawTypeName.toNullable(paramTypeName.isNullable) else paramTypeName }
                }

                // recursive to mutable

                ref.parameterizedTypeName.rawType
                    .toMutableCollection()
                    .parameterizedBy(newArgs)
                    .let { PropertySpec.builder(param.name.simpleName, it).initializer(ref.collectionType.mutableInitializer) }

            } else //  no dsl: as is
                PropertySpec.builder(param.name.simpleName, ref.parameterizedTypeName).initializer(ref.collectionType.initializer)
        }.mutable().build()
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

        fun listOrSet(name: String, isNullable: Boolean, isCommonType: Boolean, isSet: Boolean) {
            StringBuilder("$RET.$name = $name.map { it").apply {
                commonTypeOrNot(isNullable, isCommonType)
                append(" }")
                if (isSet) append(".toMutableSet()") else append(".toMutableList()")
            }.toString().also(builder::addStatement)
        }

        fun mutableListOrSet(name: String, isSet: Boolean) {
            StringBuilder("$RET.$name = $name.").apply {
                if (isSet) append("toMutableSet()") else append("toMutableList()")
            }.toString().also(builder::addStatement)
        }

        fun mutableMap(name: String) {
            builder.addStatement("$RET.$name = $name.toMutableMap()")
        }

        fun map(name: String, parametrized: List<NullableHolder>) {
            StringBuilder("$RET.$name = $name.entries.associate { ").apply {
                append("it.key")
                boxedOrNot(parametrized[0])
                append(" to it.value")
                boxedOrNot(parametrized[1])
                append(" }")
                /*if (!parametrized.all { it.type is KDType.Boxed })*/ append(".toMutableMap()")
            }.toString().also(builder::addStatement)
        }

        private fun StringBuilder.boxedOrNot(wrapper: NullableHolder) {
            if (wrapper.type is KDType.Boxed) commonTypeOrNot(wrapper.isNullable, wrapper.type.isParsable)
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

    private data class NullableHolder(
        // from property or collection parameter
        val type: KDType,
        val isNullable: Boolean
    ) {
        // isNullable (only for ValueObjectSingle), isValueObject, keyOrValue
        // return formatted, vo.className?
        fun toStatementTemplate(paramName: String) =
            if (type is KDType.Boxed)
                type.takeIf { isNullable }?.let { Chunk("$paramName?.let(%T::${type.fabricMethod})", type.className) }
                    ?: Chunk("%T.${type.fabricMethod}($paramName)", type.className)
            else Chunk(paramName)
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

        private fun ClassName.toMutableCollection() = when (this) {
            com.squareup.kotlinpoet.LIST -> MUTABLE_LIST
            com.squareup.kotlinpoet.SET -> MUTABLE_SET
            com.squareup.kotlinpoet.MAP -> MUTABLE_MAP
            else -> error("Unsupported collection for mutable: $this")
        }

        /** BOXED or Enum or lambda */
        private fun createDslBuilderParameter(name: String, nestedType: NullableHolder) = when (nestedType.type) {
            is KDType.Sealed ->
                ParameterSpec.builder(name, nestedType.type.sourceTypeName).build()
            is KDType.Boxed ->
                nestedType.type.rawTypeName.let { type -> type.takeIf { nestedType.isNullable }?.toNullable() ?: type }
                    .let { ParameterSpec.builder(name, it).build() }

            is KDType.Model -> ParameterSpec.builder(
                name,
                LambdaTypeName.get(
                    receiver = nestedType.type.dslBuilderClassName,
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
        private fun createDslBuilderFunction(name: MemberName, nullableHolder: NullableHolder, isCollection: Boolean) =
            createDslBuilderParameter("p1", nullableHolder).let { blockParam ->
                FunSpec.builder(name.simpleName).apply {
                    addParameter(blockParam)
                    val dslBuilderClassName = (nullableHolder.type as KDType.Data).dslBuilderClassName
                    if (isCollection)
                        addStatement(
                            "${KDType.Data.APPLY_BUILDER}.also(%N::add)",
                            dslBuilderClassName,
                            blockParam,
                            name
                        )
                    else
                        addStatement(
                            "%N = ${KDType.Data.APPLY_BUILDER}",
                            name,
                            dslBuilderClassName,
                            blockParam
                        )
                }.build()
            }

        private fun createDslBuilderForMap(name: MemberName, keyType: NullableHolder, valueType: NullableHolder) =
            FunSpec.builder(name.simpleName).apply {
                val p1 = createDslBuilderParameter("p1", keyType)
                val p2 = createDslBuilderParameter("p2", valueType)
                addParameter(p1)
                addParameter(p2)
                val templateArgs = mutableListOf<Any>(name)
                StringBuilder("%N[").apply {
                    if (keyType.type is KDType.Data) {
                        append(KDType.Data.APPLY_BUILDER)
                        templateArgs += keyType.type.dslBuilderClassName
                    } else append("%N")
                    templateArgs += p1
                    append("] = ")
                    if (valueType.type is KDType.Data) {
                        append(KDType.Data.APPLY_BUILDER)
                        templateArgs += valueType.type.dslBuilderClassName
                    } else append("%N")
                    templateArgs += p2
                }.toString().also { addStatement(it, *templateArgs.toTypedArray()) }
            }.build()
    }
}
