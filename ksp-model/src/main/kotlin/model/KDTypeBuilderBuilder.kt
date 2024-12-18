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
import ru.it_arch.clean_ddd.ksp.model.KDReference.Collection.CollectionType.LIST
import ru.it_arch.clean_ddd.ksp.model.KDReference.Collection.CollectionType.MAP
import ru.it_arch.clean_ddd.ksp.model.KDReference.Collection.CollectionType.SET

public class KDTypeBuilderBuilder private constructor(
    private val holder: KDType.Model,
    private val isDsl: Boolean,
    private val logger: KDLogger
) {

    /** fun Impl.toBuilder(): Impl.Builder */
    private val toBuildersFun =
        ToBuildersFun((holder.dslBuilderClassName.takeIf { isDsl } ?: holder.builderClassName), isDsl)

    /** class Impl.<Dsl>Builder()  */
    private val classBuilder =
        (holder.dslBuilderClassName.takeIf { isDsl }?.let(TypeSpec::classBuilder)
            ?: TypeSpec.classBuilder(holder.builderClassName))

    /** fun Impl.Builder.build(): Impl */
    private val builderBuildFun = FunSpec.builder(KDType.Data.BUILDER_BUILD_METHOD_NAME)
        .returns(holder.className)

    init {
        holder.parameters.map(::toBuilderPropertySpec).also(classBuilder::addProperties)
        holder.parameters
            .filter { it.typeReference is KDReference.Element && !it.typeReference.typeName.isNullable }
            .forEach { p ->
                builderBuildFun
                    .addStatement(
                        """requireNotNull(%N) { "Property '%T.%N' is not set!" }""",
                        p.name,
                        holder.className,
                        p.name
                    )
            }

        builderBuildFun.addStatement("return %T(⇥", holder.className)
        holder.parameters.forEach { param ->
            when (param.typeReference) {
                is KDReference.Element -> holder.getKDType(param.typeReference.typeName)
                    .also { addParameterForElement(param.name, it, param.typeReference.typeName.isNullable) }
                    .let { it is KDType.Boxed && isDsl }

                is KDReference.Collection -> param.typeReference.parameterizedTypeName.typeArguments
                    .map { KDTypeWrapper(holder.getKDType(it), it.isNullable) }
                    .also { addParameterForCollection(param.name, param.typeReference.collectionType, it) }
                    .let { false }
            }
        }
        builderBuildFun.addStatement("⇤)")
    }

    public fun build(): TypeSpec =
        classBuilder.addFunction(builderBuildFun.build()).build()

    public fun buildFunToBuilder(): FunSpec =
        toBuildersFun.build()

    /**
     * 1. <Dsl>Builder.build() { return T(<name> = <name>) }
     *    DslBuilder.build(): <name> = <T>.parse(<name>!!), <name> = <name>?.let(<T>::parse),
     * 2. fun to<Dsl>Builder() { <name> = <name> }
     **/
    private fun addParameterForElement(name: MemberName, nestedType: KDType, isNullable: Boolean) {
        if (nestedType is KDType.Boxed && isDsl) {
            if (isNullable) builderBuildFun.addStatement("%N = %N?.let(%T::${nestedType.fabricMethod}),", name, name, nestedType.className)
            else builderBuildFun.addStatement("%N = %T.${nestedType.fabricMethod}(%N!!),", name, nestedType.className, name)
            toBuildersFun.element(name.simpleName, isNullable, nestedType.isParsable)
        } else {
            if (isDsl && nestedType is KDType.Data)
                createDslBuilder(name, KDTypeWrapper(nestedType, isNullable), false).also(classBuilder::addFunction)
            toBuildersFun.asIs(name.simpleName)
            builderBuildFun.addStatement("%N = %N${if (!isNullable) "!!" else ""},", name, name)
        }
    }

    private fun addParameterForCollection(
        name: MemberName,
        collectionType: KDReference.Collection.CollectionType,
        parametrized: List<KDTypeWrapper>,
    ) {
        when (collectionType) {
            LIST, SET -> parametrized.first().also { wrapper ->
                //logger.warn("wrapper: $wrapper isDsl: $isDsl")
                if (wrapper.type is KDType.Boxed && isDsl) {
                    toBuildersFun.listOrSet(name.simpleName, wrapper.isNullable, wrapper.type.isParsable, collectionType == SET)
                    StringBuilder("%N = %N.map").apply {
                        takeIf { wrapper.isNullable }?.append(" { it?.let(%T::${wrapper.type.fabricMethod}) }")
                            ?: append("(%T::${wrapper.type.fabricMethod})")
                        takeIf { collectionType == SET }?.append(".toSet()")
                        append(',')
                    }.also { builderBuildFun.addStatement(it.toString(), name, name, wrapper.type.className) }
                } else { // !Single || !isDsl
                    if (isDsl) toBuildersFun.mutableListOrSet(name.simpleName, collectionType == SET)
                    else toBuildersFun.asIs(name.simpleName)
                    if (isDsl && wrapper.type is KDType.Data)
                        createDslBuilder(name, wrapper, true).also(classBuilder::addFunction)
                    StringBuilder("%N = %N").apply {
                        if (isDsl) takeIf { collectionType == LIST }?.append(".toList()") ?: append(".toSet()")
                        append(',')
                    }.also { builderBuildFun.addStatement(it.toString(), name, name) }
                }
            }
            MAP -> {
                if (parametrized.any { it.type is KDType.Data } && isDsl)
                    createDslBuilderForMap(name, parametrized[0], parametrized[1]).also(classBuilder::addFunction)

                if (parametrized.all { it.type !is KDType.Boxed } || !isDsl) {
                    StringBuilder("%N = %N").apply {
                        if (isDsl) append(".toMap(),") else append(',')
                    }.also { builderBuildFun.addStatement(it.toString(), name, name) }
                    toBuildersFun.mutableMap(name.simpleName)
                } else { // has BOXED && isDSL
                    toBuildersFun.map(name.simpleName, parametrized)
                    // BOXED: 1 or 2. Collect Impl class names for Impl.create(BOXED)
                    parametrized.fold(mutableListOf<ClassName>()) { acc, item ->
                        if (item.type is KDType.Boxed) acc.add(item.type.className)
                        acc
                    }.let { implClassNames ->
                        StringBuilder("%N = %N.entries.associate { ").apply {
                            append(parametrized[0].toStatementTemplate(true))
                            append(" to ")
                            append(parametrized[1].toStatementTemplate(false))
                            append(" },")
                        }.toString().let { st ->
                            if (implClassNames.size > 1) builderBuildFun.addStatement(st, name, name, implClassNames[0], implClassNames[1])
                            else builderBuildFun.addStatement(st, name, name, implClassNames.first())
                        }
                    }
                }
            }
        }
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
    private fun toBuilderPropertySpec(param: KDParameter) = param.typeReference.let { ref ->
        when (ref) {
            is KDReference.Element -> holder.getKDType(ref.typeName).let { nestedType ->
                if (nestedType is KDType.Boxed && isDsl) nestedType.rawTypeName else ref.typeName
            }.let { PropertySpec.builder(param.name.simpleName, it.toNullable()).initializer("null") }

            is KDReference.Collection -> {
                val newArgs = ref.parameterizedTypeName.typeArguments.map { param ->
                    val voType = holder.getKDType(param)
                    // ValueObject.Boxed<BOXED> -> BOXED
                    if (voType is KDType.Boxed && isDsl) voType.rawTypeName.toNullable(param.isNullable) else param
                }
                ref.parameterizedTypeName.typeArguments
                    // Если есть хоть один ValueObject.Data, то нужно готовить mutableCollection для ф-ции DSL-build
                    .takeIf { args -> args.any { holder.getKDType(it) is KDType.Data } && isDsl }
                    ?.let {
                        ref.parameterizedTypeName.rawType
                            .toMutableCollection()
                            .parameterizedBy(newArgs)
                            .let { PropertySpec.builder(param.name.simpleName, it).initializer(ref.collectionType.mutableInitializer) }
                    }
                    ?: ref.parameterizedTypeName.copy(typeArguments = newArgs)
                        .let { PropertySpec.builder(param.name.simpleName, it).initializer(ref.collectionType.initializer) }
            }
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
                if (isSet) append(".toSet()")
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

        fun map(name: String, parametrized: List<KDTypeWrapper>) {
            StringBuilder("$RET.$name = $name.entries.associate { ").apply {
                append("it.key")
                boxedOrNot(parametrized[0])
                append(" to it.value")
                boxedOrNot(parametrized[1])
                append(" }")
                if (!parametrized.all { it.type is KDType.Boxed }) append(".toMutableMap()")
            }.toString().also(builder::addStatement)
        }

        private fun StringBuilder.boxedOrNot(wrapper: KDTypeWrapper) {
            if (wrapper.type is KDType.Boxed) {
                commonTypeOrNot(wrapper.isNullable, wrapper.type.isParsable)
                /*
                if (wrapper.isNullable) append('?')
                append(".${KDType.Boxed.PARAM_NAME}")

                 */
            }
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

    private data class KDTypeWrapper(
        val type: KDType,
        val isNullable: Boolean
    ) {

        // isNullable (only for ValueObjectSingle), isValueObject, keyOrValue
        // return formatted, vo.className?
        fun toStatementTemplate(isKey: Boolean) =
            ("it.key".takeIf { isKey } ?: "it.value").let { paramName ->
                if (type is KDType.Boxed) {
                    type.takeIf { isNullable }?.let { "$paramName?.let(%T::${type.fabricMethod})" }
                        ?: "%T.${type.fabricMethod}($paramName)"
                } else paramName
            }
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
        private fun createDslBuilderParameter(name: String, nestedType: KDTypeWrapper) = when (nestedType.type) {
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
         * @param kdTypeWrapper type is KDValueObject
         **/
        private fun createDslBuilder(name: MemberName, kdTypeWrapper: KDTypeWrapper, isCollection: Boolean) =
            createDslBuilderParameter("p1", kdTypeWrapper).let { blockParam ->
                FunSpec.builder(name.simpleName).apply {
                    addParameter(blockParam)
                    val dslBuilderClassName = (kdTypeWrapper.type as KDType.Data).dslBuilderClassName
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

        private fun createDslBuilderForMap(name: MemberName, keyType: KDTypeWrapper, valueType: KDTypeWrapper) =
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
