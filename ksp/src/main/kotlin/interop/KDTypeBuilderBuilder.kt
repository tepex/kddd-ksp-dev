package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import ru.it_arch.clean_ddd.ksp.interop.KDReference.Collection.CollectionType.*
import ru.it_arch.ddd.ValueObject

internal class KDTypeBuilderBuilder(
    private val holder: KDType.KDValueObject,
    private val isDsl: Boolean,
    private val logger: KSPLogger
) {
    /** fun Impl.toBuilder(): Impl.Builder */
    private val toBuilderFun: ToBuilderFun? = if (!isDsl) ToBuilderFun(holder) else null

    /** class Impl.<Dsl>Builder() : ValueObject.IBuilder<Impl> */
    private val classBuilder =
        (holder.dslBuilderClassName.takeIf { isDsl }?.let(TypeSpec::classBuilder)
            ?: TypeSpec.classBuilder(holder.builderClassName))
            .also { cb ->
                ValueObject.IBuilder::class.asTypeName().parameterizedBy(holder.className).also(cb::addSuperinterface)
            }

    /** fun Impl.Builder.build(): Impl */
    private val builderBuildFun = FunSpec.builder(KDType.KDValueObject.BUILDER_BUILD_METHOD_NAME)
        .addModifiers(KModifier.OVERRIDE)
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
                is KDReference.Element -> holder.getNestedType(param.typeReference.typeName)
                    .also { addParameterForElement(param.name, it, param.typeReference.typeName.isNullable) }

                is KDReference.Collection -> param.typeReference.parameterizedTypeName.typeArguments
                    .map { KDTypeWrapper(holder.getNestedType(it), it.isNullable) }
                    .also { addParameterForCollection(param.name, param.typeReference.collectionType, it, logger) }
            }
            toBuilderFun?.addStatement(param.name.simpleName)
        }
        builderBuildFun.addStatement("⇤)")
    }

    fun build() =
        classBuilder.addFunction(builderBuildFun.build()).build()

    fun buildFunToBuilder() =
        toBuilderFun?.build()

    private fun addParameterForElement(name: MemberName, nestedType: KDType, isNullable: Boolean) {
        if (nestedType is KDType.KDValueObjectSingle && isDsl) {
            if (isNullable) builderBuildFun.addStatement("%N = %N?.let(%T::create),", name, name, nestedType.className)
            else builderBuildFun.addStatement("%N = %T.create(%N!!),", name, nestedType.className, name)
            //toBuilderFun?.element(name.simpleName, isNullable)
        } else {
            if (isDsl && nestedType is KDType.KDValueObject)
                createDslBuilder(name, KDTypeWrapper(nestedType, isNullable), false).also(classBuilder::addFunction)
            //toBuilderFun?.valueObject(name.simpleName)
            builderBuildFun.addStatement("%N = %N${if (!isNullable) "!!" else ""},", name, name)
        }
    }

    private fun addParameterForCollection(
        name: MemberName,
        collectionType: KDReference.Collection.CollectionType,
        parametrized: List<KDTypeWrapper>,
        logger: KSPLogger
    ) {
        when (collectionType) {
            LIST, SET -> parametrized.first().also { wrapper ->
                //logger.warn("wrapper: $wrapper isDsl: $isDsl")
                if (wrapper.type is KDType.KDValueObjectSingle && isDsl) {
                    //toBuilderFun?.listOrSet(name.simpleName, wrapper.isNullable, collectionType == SET)
                    StringBuilder("%N = %N.map").apply {
                        takeIf { wrapper.isNullable }?.append(" { it?.let(%T::create) }") ?: append("(%T::create)")
                        takeIf { collectionType == SET }?.append(".toSet()")
                        append(',')
                    }.also { builderBuildFun.addStatement(it.toString(), name, name, wrapper.type.className) }
                } else { // !Single || !isDsl
                    if (isDsl && wrapper.type is KDType.KDValueObject)
                        createDslBuilder(name, wrapper, true).also(classBuilder::addFunction)
                    StringBuilder("%N = %N").apply {
                        if (isDsl) takeIf { collectionType == LIST }?.append(".toList()") ?: append(".toSet()")
                        append(',')
                    }.also { builderBuildFun.addStatement(it.toString(), name, name) }
                }
            }
            MAP -> {
                if (parametrized.any { it.type is KDType.KDValueObject } && isDsl)
                    createDslBuilderForMap(name, parametrized[0], parametrized[1]).also(classBuilder::addFunction)

                if (parametrized.all { it.type !is KDType.KDValueObjectSingle } || !isDsl) {
                    StringBuilder("%N = %N").apply {
                        if (isDsl) append(".toMap(),") else append(',')
                    }.also { builderBuildFun.addStatement(it.toString(), name, name) }
                } else { // has BOXED && isDSL
                    // BOXED: 1 or 2. Collect Impl class names for Impl.create(BOXED)
                    parametrized.fold(mutableListOf<ClassName>()) { acc, item ->
                        if (item.type is KDType.KDValueObjectSingle) acc.add(item.type.className)
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
            is KDReference.Element -> holder.getNestedType(ref.typeName).let { innerType ->
                if (innerType is KDType.KDValueObjectSingle && isDsl) innerType.boxedType else ref.typeName
            }.let { PropertySpec.builder(param.name.simpleName, it.toNullable()).initializer("null") }

            is KDReference.Collection -> {
                val newArgs = ref.parameterizedTypeName.typeArguments.map { typeName ->
                    val voType = holder.getNestedType(typeName)
                    if (voType is KDType.KDValueObjectSingle && isDsl) // ValueObjectSingle<BOXED> -> BOXED
                        voType.boxedType.toNullable(typeName.isNullable)
                    else typeName
                }
                ref.parameterizedTypeName.typeArguments
                    // Если есть хоть один ValueObject, то нужно готовить mutableCollection для ф-ции DSL-build
                    .takeIf { args -> args.any { holder.getNestedType(it) is KDType.KDValueObject } && isDsl }
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

    private companion object {
        fun ClassName.toMutableCollection() = when (this) {
            com.squareup.kotlinpoet.LIST -> MUTABLE_LIST
            com.squareup.kotlinpoet.SET -> MUTABLE_SET
            com.squareup.kotlinpoet.MAP -> MUTABLE_MAP
            else -> error("Unsupported collection for mutable: $this")
        }

        /** BOXED or Enum or lambda */
        fun createDslBuilderParameter(name: String, nestedType: KDTypeWrapper) = when (nestedType.type) {
            is KDType.KDValueObjectBase ->
                ParameterSpec.builder(name, nestedType.type.typeName).build()
            is KDType.KDValueObjectSingle ->
                (nestedType.type.boxedType.takeIf { nestedType.isNullable }?.toNullable()
                    ?: nestedType.type.boxedType).let { ParameterSpec.builder(name, it).build() }
            is KDType.KDValueObject -> ParameterSpec.builder(
                name,
                LambdaTypeName.get(
                    receiver = nestedType.type.dslBuilderClassName,
                    returnType = Unit::class.asTypeName()
                )
            ).build()
        }

        /**
         * DSL builder for param: ValueObject, param: List<ValueObject>
         * `fun <t>(block: <T>Impl.DslBuilder.() -> Unit) { ... }`
         *
         * @param kdTypeWrapper type is KDValueObject
         **/
        fun createDslBuilder(name: MemberName, kdTypeWrapper: KDTypeWrapper, isCollection: Boolean) =
            createDslBuilderParameter("p1", kdTypeWrapper).let { blockParam ->
                FunSpec.builder(name.simpleName).apply {
                    addParameter(blockParam)
                    val dslBuilderClassName = (kdTypeWrapper.type as KDType.KDValueObject).dslBuilderClassName
                    if (isCollection)
                        addStatement(
                            "${KDType.KDValueObject.APPLY_BUILDER}.also(%N::add)",
                            dslBuilderClassName,
                            blockParam,
                            name
                        )
                    else
                        addStatement(
                            "%N = ${KDType.KDValueObject.APPLY_BUILDER}",
                            name,
                            dslBuilderClassName,
                            blockParam
                        )
                }.build()
            }

        fun createDslBuilderForMap(name: MemberName, keyType: KDTypeWrapper, valueType: KDTypeWrapper) =
            FunSpec.builder(name.simpleName).apply {
                val p1 = createDslBuilderParameter("p1", keyType)
                val p2 = createDslBuilderParameter("p2", valueType)
                addParameter(p1)
                addParameter(p2)
                val templateArgs = mutableListOf<Any>(name)
                StringBuilder("%N[").apply {
                    if (keyType.type is KDType.KDValueObject) {
                        append(KDType.KDValueObject.APPLY_BUILDER)
                        templateArgs += keyType.type.dslBuilderClassName
                    } else append("%N")
                    templateArgs += p1
                    append("] = ")
                    if (valueType.type is KDType.KDValueObject) {
                        append(KDType.KDValueObject.APPLY_BUILDER)
                        templateArgs += valueType.type.dslBuilderClassName
                    } else append("%N")
                    templateArgs += p2
                }.toString().also { addStatement(it, *templateArgs.toTypedArray()) }
            }.build()
    }

    private class ToBuilderFun(kdType: KDType.KDValueObject) {
        private val returnType =
            TypeVariableName("B", ValueObject.IBuilder::class.asTypeName().parameterizedBy(STAR))

        private val builder = FunSpec.builder("toBuilder")
            .addModifiers(KModifier.OVERRIDE)
            .addUncheckedCast()
            .addTypeVariable(returnType)
            .returns(returnType)
            .addStatement("val $RET = %T()", kdType.builderClassName)

        fun addStatement(name: String) {
            builder.addStatement("$RET.$name = $name")
        }

        fun build(): FunSpec =
            builder.addStatement("return $RET as %T", returnType).build()

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
                paramName.takeIf { type is KDType.KDValueObjectSingle }
                    ?.let { it.takeIf { isNullable }?.let { "$paramName?.let(%T::create)" } ?: "%T.create($paramName)" }
                    ?: paramName
            }
    }
}
