package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import ru.it_arch.clean_ddd.ksp.model.KDReference.Collection
import ru.it_arch.clean_ddd.ksp.model.KDReference.Collection.CollectionType.LIST as KDLIST
import ru.it_arch.clean_ddd.ksp.model.KDReference.Collection.CollectionType.MAP as KDMAP
import ru.it_arch.clean_ddd.ksp.model.KDReference.Collection.CollectionType.SET as KDSET

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
                        NullableHolder.create(holder, property.typeReference.typeName)
                            .also { funSpecStatement.addParameterForElement(property.name, it) }

                        // return new PropertySpec
                        holder.getKDType(ref.typeName).let { nestedType ->
                            if (nestedType is KDType.Boxed && isDsl) nestedType.rawTypeName else ref.typeName
                        }.let { PropertySpec.builder(property.name.simpleName, it.toNullable()).initializer("null") }
                    }

                    is Collection -> {
                        if (isDsl) {
                            // Builder().return
                            // funSpecStatement.addCollectionParameter(param.name, param.typeReference.collectionType, it)
                            /*
                            if (holder.className.simpleName == "AATestCollectionsImpl")
                                funSpecStatement.processCollectionParameter(property)*/

                            //logger.log("processing property: $property")
                            substituteForDsl(Collection.create(ref.parameterizedTypeName)).let { collection ->
                                val ret = collection.parameterizedTypeName
                                    .let { PropertySpec.builder(property.name.simpleName, it).initializer(ref.collectionType.mutableInitializer) }

                                // !!!
                                // return from Builder.build() { return T(...) }
                                funSpecStatement.processCollectionParameter(property, collection)

                                ret
                            }
                            // return new PropertySpec
                        } else PropertySpec.builder(property.name.simpleName, ref.parameterizedTypeName)
                            .initializer(ref.collectionType.initializer)
                    }
                }.mutable().build().also(innerBuilder::addProperty)
            }

            // Check nulls statements
            if (property.typeReference  is KDReference.Element && !property.typeReference.typeName.isNullable )
                builderFunBuild.addStatement(
                    """requireNotNull(%N) { "Property '%T.%N' is not set!" }""", property.name, holder.className, property.name)
        }

        if (isDsl) {
            logger.log("Processing: ${holder.className.simpleName}")
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
     * 1. <Dsl>Builder.build() { return T(<name> = <name>) }
     *    DslBuilder.build(): <name> = <T>.parse(<name>!!), <name> = <name>?.let(<T>::parse),
     * 2. fun to<Dsl>Builder() { <name> = <name> }
     **/
    private fun FunSpecStatement.addParameterForElement(name: MemberName, nullableHolder: NullableHolder) {
        +Chunk("%N = ", name)
        if (nullableHolder.type is KDType.Boxed && isDsl) {
            if (nullableHolder.isNullable) +Chunk("%N?.let(::${nullableHolder.type.dslBuilderFunName}),", name)
            else +Chunk("${nullableHolder.type.dslBuilderFunName}(%N!!),", name)
            toBuildersHolder.element(name.simpleName, nullableHolder.isNullable, nullableHolder.type.isParsable)
        } else {
            if (isDsl && nullableHolder.type is KDType.Data)
                createDslBuilderFunction(name, nullableHolder, false).also(innerBuilder::addFunction)
            toBuildersHolder.asIs(name.simpleName)
            +Chunk("%N${if (!nullableHolder.isNullable) "!!" else ""},", name)
        }
    }

    private fun FunSpecStatement.processCollectionParameterOld(param: KDPropertyHolder) {
        +Chunk("%N = %N", param.name, param.name)
        // recursive
        // create collection:  .map { or .entries.associate {
        logger.log("For: ${param.typeReference.typeName}")
        logger.log("${param.name.simpleName} = ${param.name.simpleName}")
        recursive((param.typeReference as Collection).parameterizedTypeName)
        // close collection: }
        logger.log("")
        endStatement()
    }

    private fun FunSpecStatement.processCollectionParameter(property: KDPropertyHolder, collection: Collection) {
        logger.log("For: ${property.name.simpleName}: ${property.typeReference.typeName}")
        logger.log("    return: ${property.name.simpleName}${collection.unDslMapper}")


        +Chunk("%N = %N", property.name, property.name)
        endStatement()
    }


    // return constructor(...)
    // toBuilder() {
    // ret.<name> = <name>
    // ret.list = list.map { it?.boxed }.toMutableList()
    // }

    // recursive to immutable
    // nestedList = nestedList.map { it.map(NameImpl::create) }
    private fun FunSpecStatement.addCollectionParameter(
        name: MemberName,
        collectionType: Collection.CollectionType,
        _parametrized: List<TypeName>,
    ) {

        +Chunk("%N = %N", name, name)
        when (collectionType) {
            KDLIST, KDSET -> {
                val nullableHolder = NullableHolder.create(holder, _parametrized.first())

                if (isDsl) {
                    if (nullableHolder.type is KDType.Boxed) {
                        +Chunk(".map")
                        // recursive
                        toBuildersHolder
                            .listOrSet(name.simpleName, nullableHolder.isNullable, nullableHolder.type.isParsable,collectionType == KDSET)

                        // recursive
                        if (nullableHolder.isNullable)
                            +Chunk(" { it?.let(%T::${nullableHolder.type.fabricMethod}) }", nullableHolder.type.className)
                        else +Chunk("(%T::${nullableHolder.type.fabricMethod})", nullableHolder.type.className)

                    } else if (nullableHolder.type is KDType.Model) {
                        toBuildersHolder.mutableListOrSet(name.simpleName, collectionType == KDSET)
                        createDslBuilderFunction(name, nullableHolder, true).also(innerBuilder::addFunction)
                    } else toBuildersHolder.asIs(name.simpleName)

                    if (collectionType == KDLIST) toList() else toSet()

                } else { // !Boxed
                    toBuildersHolder.asIs(name.simpleName) // !isDsl
                }
            }
            KDMAP -> {
                val parametrized = _parametrized.map { NullableHolder.create(holder, it) }
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

    private fun FunSpecStatement.recursive(node: ParameterizedTypeName) {
        val args = node.typeArguments
        // finish recursion
        if (args.all { it !is ParameterizedTypeName }) {
            if (node.rawType == MAP) {
                processMapArgs(args)
            } else {
                processListOrSetArg(args.first()).also {
                    logger.log("$it${node.toImmutable()}")
                    +Chunk(node.toImmutable())
                }
            }
        } else {
            logger.log("recursion for args: $args")
        }
    }

    private fun FunSpecStatement.processMapArgs(args: List<TypeName>) {
        logger.log(".entries.associate { convert MAP args }")
        /*
        +Chunk(".entries.associate { ")
        +parametrized[0].toStatementTemplate("it.key")
        +Chunk(" to ")
        +parametrized[1].toStatementTemplate("it.value")
        +Chunk(" }")*/

    }

    // ValueObject.Boxed or ValueObject.Data
    private fun FunSpecStatement.processListOrSetArg(arg: TypeName): String {
        return holder.getKDType(arg).let { kdType ->
            if (kdType is KDType.Boxed) {
                if (arg.isNullable) {
                    +Chunk(".map { it?.let(::${kdType.dslBuilderFunName}) }")
                    ".map { it?.let(::${kdType.dslBuilderFunName}) }"
                }
                else {
                    +Chunk(".map(::${kdType.dslBuilderFunName})")
                    ".map(::${kdType.dslBuilderFunName})"
                }
            } else if (kdType is KDType.Model) {
                // TODO: generate fabric method only for 0-level
                // createDslBuilderFunction(name, nullableHolder, true).also(innerBuilder::addFunction)
                ""
            } else ""
        }
    }

    private fun ParameterizedTypeName.toImmutable() = when(rawType) {
        MAP -> ".toMap()"
        LIST -> ".toList()"
        SET -> ".toSet()"
        else -> error("Unsupported collection type: $rawType")
    }

    // ret.list = list.map { it?.boxed }.toMutableList()
    // src:
    // var nestedList:              MutableSet< MutableList<String>                    > = mutableSetOf()
    //                              Set       < List<Name>                             >
    // var nestedList:              MutableSet< MutableList<String?>                   > = mutableSetOf()
    //                              Set       < List<Name?>                            >
    // var nestedList:              MutableSet< MutableList<Inner>                     > = mutableSetOf()
    //                              Set       < List<Inner>                            >
    // var nestedList:              MutableSet< MutableList<Inner?>                    > = mutableSetOf()
    //                              Set       < List<Inner?>                           >

    // toBuilder: as is

    // toDslBuilder:
    // ret.nestedList = nestedList  .map      { it.map { it.boxed }                  .toMutable()   }.toMutable()
    // ret.nestedList = nestedList  .map      { it.map { it?.boxed }                 .toMutable()   }.toMutable() // null
    // ret.nestedList = nestedList  .map      { it                                   .toMutable()   }.toMutable()
    // ret.nestedList = nestedList  .map      { it                                   .toMutable()   }.toMutable() // null

    // DslBuilder.return:
    // nestedList = nestedList      .map      { it.map(::name)                       .toImmutable()  }.toImmutable()
    // nestedList = nestedList      .map      { it.map { it?.let(::name) }           .toImmutable()  }.toImmutable() // null
    // nestedList = nestedList      .map      { it                                   .toImmutable()  }.toImmutable()
    // nestedList = nestedList      .map      { it                                   .toImmutable()  }.toImmutable() // null


    // src: nestedMap: MutableMap<String?, MutableList<String>> = mutableMapOf()
    //val qqq = nestedMap.entries.associate { it.key?.let(NameImpl::create) to it.value.map { NameImpl.create(it) }.toList() }
    /*
    public val nestedMaps: Map<Map<Name, Inner?>, List<List<Inner?>>>
    nestedMaps = nestedMaps.entries.associate {
            it.key.entries.associate {
              NameImpl.create(it.key) as Name to it.value
            } to it.value.map { it.toList() }.toList()
          }

    public var nestedMaps1: MutableMap< MutableMap<String, AATestCollections.Inner>, String                                     > = mutableMapOf()
    public var nestedMaps2: MutableMap< String,                                      MutableMap<String, AATestCollections.Inner>> = mutableMapOf()

    val p1: Map<Map<AATestCollections.Name, AATestCollections.Inner>, AATestCollections.Name> =
        nestedMaps1.entries.associate { it.key.entries.associate { NameImpl.create(it.key) as AATestCollections.Name to it.value } to NameImpl.create(it.value) }

    val p2: Map<AATestCollections.Name, Map<AATestCollections.Name, AATestCollections.Inner>> =
        nestedMaps2.entries.associate { NameImpl.create(it.key)                                                                    to it.value.entries.associate { NameImpl.create(it.key) to it.value } }
*/






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
        if (param.name.simpleName == "nestedList" && isDsl) logger.log("===============")
        when (ref) {
            is KDReference.Element -> holder.getKDType(ref.typeName).let { nestedType ->
                if (nestedType is KDType.Boxed && isDsl) nestedType.rawTypeName else ref.typeName
            }.let { PropertySpec.builder(param.name.simpleName, it.toNullable()).initializer("null") }

            is Collection ->
                if (isDsl) {
                    substituteForDsl(Collection.create(ref.parameterizedTypeName)).let { collection ->
                        logger.log("    return: ${param.name}.${collection.unDslMapper}")
                        collection.parameterizedTypeName
                            .let { PropertySpec.builder(param.name.simpleName, it).initializer(ref.collectionType.mutableInitializer) }
                    }
                }
                else PropertySpec.builder(param.name.simpleName, ref.parameterizedTypeName).initializer(ref.collectionType.initializer)
        }.mutable().build()
    }

    // ValueObject.Boxed<BOXED> -> BOXED for DSL
    // https://discuss.kotlinlang.org/t/3-tailrec-questions/3981 #3
    private tailrec fun substituteForDsl(collection: Collection): Collection {
        return if (collection.isSubstituted) collection.terminate()
        else substituteForDsl(collection.apply {
            substitute { arg ->
                @Suppress("NON_TAIL_RECURSIVE_CALL")
                arg.toKDReference(holder).let { if (it is Collection) substituteForDsl(it) else it }
            }
        })
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

    private data class NullableHolder private constructor(
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

        companion object {
            //nestedList: Set<List<AATestCollections.Inner>> = emptySet()
            fun create(holder: KDType.Model, typeName: TypeName): NullableHolder {
                val kdType = holder.getKDType(typeName)
                return NullableHolder(kdType, typeName.isNullable)
            }
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
                        addStatement("${KDType.Data.APPLY_BUILDER}.also(%N::add)", dslBuilderClassName, blockParam, name)
                    else
                        addStatement("%N = ${KDType.Data.APPLY_BUILDER}", name, dslBuilderClassName, blockParam)
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
