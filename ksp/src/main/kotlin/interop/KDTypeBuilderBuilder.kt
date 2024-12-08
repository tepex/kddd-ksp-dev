package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
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
import ru.it_arch.clean_ddd.ksp.interop.KDReference.Collection.CollectionType.*
import ru.it_arch.clean_ddd.ksp.interop.KDTypeBuilderBuilder.ToBuilderFun.Companion.RET

internal class KDTypeBuilderBuilder(
    private val holder: KDType,
    private val logger: KSPLogger
) {
    /** fun Impl.toBuilder(): Impl.Builder */
    private val toBuilderFun = ToBuilderFun(holder)
    /** class Impl.Builder */
    private val classBuilder = TypeSpec.classBuilder(holder.builderClassName)

    /** fun Impl.Builder.build(): Impl */
    private val builderBuildFun = FunSpec.builder(KDType.BUILDER_BUILD_METHOD_NAME)
        .returns(holder.className)

    init {
        //helper.toBuilderFun.add("name")

        holder.parameters.map(::toBuilderPropertySpec).also(classBuilder::addProperties)
        holder.parameters
            .filter { it.typeReference is KDReference.Element && !it.typeReference.typeName.isNullable }
            .forEach { p ->
                builderBuildFun
                    .addStatement("""requireNotNull(%N) { "Property '%T.%N' is not set!" }""", p.name, holder.className, p.name)
            }

        builderBuildFun.addStatement("return %T(⇥", holder.className)
        holder.parameters.forEach { param ->
            when(param.typeReference) {
                is KDReference.Element    -> holder.getNestedType(param.typeReference.typeName)
                    .also { addParameterForElement(param.name, it, param.typeReference.typeName.isNullable) }
                is KDReference.Collection -> param.typeReference.parameterizedTypeName.typeArguments
                    .map { holder.getNestedType(it) to it.isNullable }
                    .also { addParameterForCollection(param.name, param.typeReference.collectionType, it) }
            }
        }
        builderBuildFun.addStatement("⇤)")
    }

    fun build() =
        classBuilder.addFunction(builderBuildFun.build()).build()

    fun buildFunToBuilder() =
        toBuilderFun.build()

    private fun addParameterForElement(name: MemberName, nestedType: KDType, isNullable: Boolean) {
        if (nestedType.valueObjectType.isValueObject) {
            createDslBuilder(name, nestedType, false).also(classBuilder::addFunction)
            builderBuildFun.addStatement("%N = %N${if (!isNullable) "!!" else ""},", name, name)
            toBuilderFun.valueObject(name.simpleName)
        } else {
            if (isNullable) builderBuildFun.addStatement("%N = %N?.let(%T::create),", name, name, nestedType.className)
            else builderBuildFun.addStatement("%N = %T.create(%N!!),", name, nestedType.className, name)
            toBuilderFun.element(name.simpleName, isNullable)
        }
    }

    private fun addParameterForCollection(
        name: MemberName,
        collectionType: KDReference.Collection.CollectionType,
        parametrized: List<Pair<KDType, Boolean>>) {

        when(collectionType) {
            LIST, SET -> parametrized.first().also { (kdType, isNullable) ->
                StringBuilder("%N = %N").apply {
                    takeIf { collectionType == LIST }?.append(".toList(),") ?: append(".toSet(),")
                }.toString().takeIf { kdType.valueObjectType.isValueObject }?.also { str ->
                    builderBuildFun.addStatement(str, name, name)
                    createDslBuilder(name, kdType,true).also(classBuilder::addFunction)
                } ?: StringBuilder("%N = %N.map").apply {
                    takeIf { isNullable }?.append(" { it?.let(%T::create) }") ?: append("(%T::create)")
                    takeIf { collectionType == SET }?.append(".toSet()")
                    append(',')
                }.toString()
                    .also { builderBuildFun.addStatement(it, name, name, kdType.className) }
                toBuilderFun.listOrSet(name.simpleName, isNullable, collectionType == SET)
            }

            // TODO: when parametrized type is ValueObject
            MAP -> parametrized.find { it.first.valueObjectType.isValueObject }
                ?.also {
                    createDslBuilder1(name, parametrized[0].first, parametrized[1].first).also(classBuilder::addFunction)
                    builderBuildFun.addStatement("%N = %N.toMap(),", name, name)
                }
                ?: StringBuilder("%N = %N.entries.associate { ").apply {
                    takeIf { parametrized[0].second }?.append("it.key?.let(%T::create) to ") ?: append("%T.create(it.key) to ")
                    takeIf { parametrized[1].second }?.append("it.value?.let(%T::create)") ?: append("%T.create(it.value)")
                    append(" },")
                }.toString()
                    .also { builderBuildFun.addStatement(it, name, name, parametrized[0].first.className, parametrized[1].first.className) }
                    .also { toBuilderFun.map(name.simpleName, parametrized[0].second, parametrized[1].second) }
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
     * Map<KDType{ValueObject}, KDType{ValueObjectSingle<BOXED>}> -> MutableMap<ValueObject, ValueObjectSingle<BOXED>>
     * Map<KDType{ValueObjectSingle<BOXED>, KDType{ValueObject}>  -> MutableMap<ValueObjectSingle<BOXED>, ValueObject>
     * */
    private fun toBuilderPropertySpec(param: KDParameter) = param.typeReference.let { ref ->
        when(ref) {
            is KDReference.Element -> holder.getNestedType(ref.typeName).let { innerType ->
                if (innerType.valueObjectType is KDValueObjectType.KDValueObjectSingle) innerType.valueObjectType.boxedType
                else ref.typeName
            }.let { PropertySpec.builder(param.name.simpleName, it.toNullable()).mutable().initializer("null") }

            is KDReference.Collection -> ref.parameterizedTypeName.typeArguments
                .takeIf { args -> args.any { holder.getNestedType(it).valueObjectType.isValueObject } }?.let { args ->
                    // ValueObject -> ValueObject
                    ref.parameterizedTypeName.rawType
                        .toMutableCollection()
                        .parameterizedBy(args)
                        .let { PropertySpec.builder(param.name.simpleName, it).initializer(ref.collectionType.mutableInitializer) }
                } ?: run {
                ref.parameterizedTypeName.typeArguments.map { typeName ->
                    when(val voType = holder.getNestedType(typeName).valueObjectType) {
                        // ValueObjectSingle<BOXED> -> BOXED
                        is KDValueObjectType.KDValueObjectSingle -> voType.boxedType.toNullable(typeName.isNullable)
                        else -> typeName
                    }
                }.let { args ->
                    ref.parameterizedTypeName.copy(typeArguments = args)
                        .let { PropertySpec.builder(param.name.simpleName, it).mutable().initializer(ref.collectionType.initializer) }
                }
            }
        }.build()
    }

    private companion object {
        fun ClassName.toMutableCollection() = when (this) {
            com.squareup.kotlinpoet.LIST -> MUTABLE_LIST
            com.squareup.kotlinpoet.SET -> MUTABLE_SET
            com.squareup.kotlinpoet.MAP -> MUTABLE_MAP
            else -> error("Unsupported collection for mutable: $this")
        }

        /** BOXED or holderTypeName */
        fun createDslBuilderParameter(name: String, nestedType: KDType) = when (nestedType.valueObjectType) {
            is KDValueObjectType.KDValueObjectSingle ->
                ParameterSpec.builder(name, nestedType.valueObjectType.boxedType).build()

            else -> ParameterSpec.builder(
                name,
                LambdaTypeName.get(receiver = nestedType.builderClassName, returnType = Unit::class.asTypeName())
            ).build()
        }

        /** DSL builder: `fun <t>(block: <T>Impl.Builder.() -> Unit) { ... }` */
        fun createDslBuilder(name: MemberName, nestedType: KDType, isCollection: Boolean) =
            createDslBuilderParameter("p1", nestedType).let { blockParam ->
                FunSpec.builder(name.simpleName).apply {
                    addParameter(blockParam)
                    if (isCollection)
                        addStatement(
                            "%T().apply(%N).${KDType.BUILDER_BUILD_METHOD_NAME}().also(%N::add)",
                            nestedType.builderClassName,
                            blockParam,
                            name
                        )
                    else
                        addStatement(
                            "%N = %T().apply(%N).${KDType.BUILDER_BUILD_METHOD_NAME}()",
                            name,
                            nestedType.builderClassName,
                            blockParam
                        )
                }.build()
            }

        /** DSL builder: `fun <t>(boxed: BOXED, block: <T>Impl.Builder.() -> Unit) { ... }`*/
        /*
public fun myMap(name: String, block: InnerStructImpl.Builder.() -> Unit) {
    myMap[NameImpl.create(name)] = InnerStructImpl.Builder().apply(block).build()
}*/

        fun createDslBuilder1(name: MemberName, nestedType1: KDType, nestedType2: KDType) =
            FunSpec.builder(name.simpleName).apply {
                val p1 = createDslBuilderParameter("p1", nestedType1)
                val p2 = createDslBuilderParameter("p2", nestedType2)
                addParameter(p1)
                addParameter(p2)
            }.build()

    }

    /*
    ret.name = name.value
    ret.optName = optName?.value

    ret.names = names.map { it.value }
    ret.nullableNames = nullableNames.map { it?.value }
    ret.indexes = indexes.map { it.value }.toSet()
    ret.myMap = myMap.entries.associate { it.key.value to it.value?.value }
    ret.inner = inner // no null check
     */
    private class ToBuilderFun(kdType: KDType) {
        private val builder = FunSpec.builder("toBuilder")
            .returns(kdType.builderClassName)
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
            builder.addStatement("return $RET").build()

        private companion object {
            const val RET = "ret"
        }
    }
}
