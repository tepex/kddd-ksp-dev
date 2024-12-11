package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.AnnotationSpec
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
import ru.it_arch.ddd.ValueObjectSingle

internal class KDTypeBuilderBuilder(
    private val holder: KDType,
    private val isDsl: Boolean,
    private val logger: KSPLogger
) {
    /** fun Impl.toBuilder(): Impl.Builder */
    private val toBuilderFun: ToBuilderFun? = if (!isDsl) ToBuilderFun(holder) else null

    /** class Impl.[Dsl]Builder() : ValueObject.IBuilder<Impl> */
    private val classBuilder =
        (holder.dslBuilderClassName.takeIf { isDsl }?.let(TypeSpec::classBuilder)
            ?: TypeSpec.classBuilder(holder.builderClassName))
            .also { cb ->
                ValueObject.IBuilder::class.asTypeName().parameterizedBy(holder.className).also(cb::addSuperinterface)
            }

    /** fun Impl.Builder.build(): Impl */
    private val builderBuildFun = FunSpec.builder(KDType.BUILDER_BUILD_METHOD_NAME)
        .addModifiers(KModifier.OVERRIDE)
        .returns(holder.className)

    init {
        //helper.toBuilderFun.add("name")

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
                    .also { addParameterForCollection(param.name, param.typeReference.collectionType, it) }
            }
        }
        builderBuildFun.addStatement("⇤)")
    }

    fun build() =
        classBuilder.addFunction(builderBuildFun.build()).build()

    fun buildFunToBuilder() =
        toBuilderFun?.build()

    private fun addParameterForElement(name: MemberName, nestedType: KDType, isNullable: Boolean) {
        if (nestedType.valueObjectType.isValueObject || !isDsl) {
            if (isDsl) createDslBuilder(name, KDTypeWrapper(nestedType, isNullable), false).also(classBuilder::addFunction)
            toBuilderFun?.valueObject(name.simpleName)
            builderBuildFun.addStatement("%N = %N${if (!isNullable) "!!" else ""},", name, name)
        } else { // single or dsl
            if (isNullable) builderBuildFun.addStatement("%N = %N?.let(%T::create),", name, name, nestedType.className)
            else builderBuildFun.addStatement("%N = %T.create(%N!!),", name, nestedType.className, name)
            toBuilderFun?.element(name.simpleName, isNullable)
        }
    }

    private fun addParameterForCollection(
        name: MemberName,
        collectionType: KDReference.Collection.CollectionType,
        parametrized: List<KDTypeWrapper>
    ) {

        when (collectionType) {
            LIST, SET -> parametrized.first().also { wrapper ->
                StringBuilder("%N = %N").apply {
                    takeIf { collectionType == LIST }?.append(".toList(),") ?: append(".toSet(),")
                }.toString().takeIf { wrapper.type.valueObjectType.isValueObject || !isDsl }?.also { str ->
                    builderBuildFun.addStatement(str, name, name)
                    if (isDsl) createDslBuilder(name, wrapper, true).also(classBuilder::addFunction)
                    toBuilderFun?.mutableListOrSet(name.simpleName, collectionType == SET)
                } ?: StringBuilder("%N = %N.map").apply {
                    takeIf { wrapper.isNullable }?.append(" { it?.let(%T::create) }") ?: append("(%T::create)")
                    takeIf { collectionType == SET }?.append(".toSet()")
                    append(',')
                }.toString()
                    .also { builderBuildFun.addStatement(it, name, name, wrapper.type.className) }
                    .also { toBuilderFun?.listOrSet(name.simpleName, wrapper.isNullable, collectionType == SET) }
            }
            MAP -> {
                if (parametrized.any { it.type.valueObjectType.isValueObject } && isDsl)
                    createDslBuilderForMap(name, parametrized[0], parametrized[1]).also(classBuilder::addFunction)

                if (parametrized.all { it.type.valueObjectType.isValueObject } || !isDsl) {
                    builderBuildFun.addStatement("%N = %N.toMap(),", name, name)
                    toBuilderFun?.mutableMap(name.simpleName)
                } else { // has BOXED && isDSL
                    // 1 or 2
                    parametrized.fold(mutableListOf<ClassName>()) { acc, item ->
                        item.takeUnless { it.type.valueObjectType.isValueObject }?.also { acc.add(it.type.className) }
                        acc
                    }.let { classNames ->
                        StringBuilder("%N = %N.entries.associate { ").apply {
                            append(parametrized[0].toStatement(true))
                            append(" to ")
                            append(parametrized[1].toStatement(false))
                            append(" },")
                        }.toString().let { st ->
                            if (classNames.size > 1) builderBuildFun.addStatement(st, name, name, classNames[0], classNames[1])
                            else builderBuildFun.addStatement(st, name, name, classNames.first())
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
                if (innerType.valueObjectType is KDValueObjectType.KDValueObjectSingle && isDsl) innerType.valueObjectType.boxedType
                else ref.typeName
            }.let { PropertySpec.builder(param.name.simpleName, it.toNullable()).initializer("null") }

            is KDReference.Collection -> {
                val newArgs = ref.parameterizedTypeName.typeArguments.map { typeName ->
                    val voType = holder.getNestedType(typeName).valueObjectType
                    if (voType is KDValueObjectType.KDValueObjectSingle && isDsl) // ValueObjectSingle<BOXED> -> BOXED
                        voType.boxedType.toNullable(typeName.isNullable)
                    else typeName
                }
                ref.parameterizedTypeName.typeArguments
                    .takeIf { args -> args.any { holder.getNestedType(it).valueObjectType.isValueObject } }
                    ?.let {
                        // ValueObject -> ValueObject
                        ref.parameterizedTypeName.rawType
                            .toMutableCollection()
                            .parameterizedBy(newArgs)
                            .let {
                                PropertySpec.builder(param.name.simpleName, it)
                                    .initializer(ref.collectionType.mutableInitializer)
                            }
                    }
                    ?: ref.parameterizedTypeName.copy(typeArguments = newArgs)
                        .let {
                            PropertySpec.builder(param.name.simpleName, it).initializer(ref.collectionType.initializer)
                        }
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

        /** BOXED or holderTypeName !!! Check Nullable !!! */
        fun createDslBuilderParameter(name: String, nestedType: KDTypeWrapper) =
            when (nestedType.type.valueObjectType) {
                is KDValueObjectType.KDValueObjectSingle ->
                    (nestedType.type.valueObjectType.boxedType.takeIf { nestedType.isNullable }?.toNullable()
                        ?: nestedType.type.valueObjectType.boxedType).let { ParameterSpec.builder(name, it).build() }

                else -> ParameterSpec.builder(
                    name,
                    LambdaTypeName.get(
                        receiver = nestedType.type.dslBuilderClassName,
                        returnType = Unit::class.asTypeName()
                    )
                ).build()
            }

        /** DSL builder: `fun <t>(block: <T>Impl.DslBuilder.() -> Unit) { ... }` */
        fun createDslBuilder(name: MemberName, nestedType: KDTypeWrapper, isCollection: Boolean) =
            createDslBuilderParameter("p1", nestedType).let { blockParam ->
                FunSpec.builder(name.simpleName).apply {
                    addParameter(blockParam)
                    if (isCollection)
                        addStatement(
                            "${KDType.APPLY_BUILDER}.also(%N::add)",
                            nestedType.type.dslBuilderClassName,
                            blockParam,
                            name
                        )
                    else
                        addStatement(
                            "%N = ${KDType.APPLY_BUILDER}",
                            name,
                            nestedType.type.dslBuilderClassName,
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
                    if (keyType.type.valueObjectType.isValueObject) {
                        append(KDType.APPLY_BUILDER)
                        templateArgs += keyType.type.dslBuilderClassName
                    } else append("%N")
                    templateArgs += p1
                    append("] = ")
                    if (valueType.type.valueObjectType.isValueObject) {
                        append(KDType.APPLY_BUILDER)
                        templateArgs += valueType.type.dslBuilderClassName
                    } else append("%N")
                    templateArgs += p2
                }.toString().also { addStatement(it, *templateArgs.toTypedArray()) }
            }.build()
    }

    private class ToBuilderFun(kdType: KDType) {
        private val returnType =
            TypeVariableName("B", ValueObject.IBuilder::class.asTypeName().parameterizedBy(STAR))

        private val builder = FunSpec.builder("toBuilder")
            .addModifiers(KModifier.OVERRIDE)
            .addUncheckedCast()
            .addTypeVariable(returnType)
            .returns(returnType)
            .addStatement("val $RET = %T()", kdType.builderClassName)

        fun element(name: String, isNullable: Boolean) {
            StringBuilder("$RET.$name = $name").apply {
                if (isNullable) append('?')
                append(".${KDValueObjectType.KDValueObjectSingle.PARAM_NAME}")
            }.toString().also(builder::addStatement)
        }

        fun valueObject(name: String) {
            builder.addStatement("$RET.$name = $name")
        }

        fun listOrSet(name: String, isNullable: Boolean, isSet: Boolean) {
            StringBuilder("$RET.$name = $name.map { it").apply {
                if (isNullable) append("?")
                append(".${KDValueObjectType.KDValueObjectSingle.PARAM_NAME} }")
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

        fun map(name: String, isKeyNullable: Boolean, isValueNullable: Boolean) {
            StringBuilder("$RET.$name = $name.entries.associate { ").apply {
                append("it.key")
                if (isKeyNullable) append("?")
                append(".${KDValueObjectType.KDValueObjectSingle.PARAM_NAME} to it.value")
                if (isValueNullable) append("?")
                append(".${KDValueObjectType.KDValueObjectSingle.PARAM_NAME} }")
            }.toString().also(builder::addStatement)
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
        fun toStatement(isKey: Boolean) =
            ("it.key".takeIf { isKey } ?: "it.value").let { pn ->
                pn.takeUnless { type.valueObjectType.isValueObject }
                    ?.let { it.takeIf { isNullable }?.let { "$pn?.let(%T::create)" } ?: "%T.create($pn)" } ?: pn
            }
    }
}
