package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

/** For <Origin>.Builder class */
internal class KDTypeBuilderHelper(val kdType: KDType) {
    /** class Builder */
    val classBuilder = TypeSpec.classBuilder(kdType.builderClassName)
    /** fun toBuilder() */
    val toBuilderFun = ToBuilderFun(kdType)
    /** fun Builder.build() */
    val builderBuildFun = FunSpec.builder(KDType.BUILDER_BUILD_METHOD_NAME)

    /** BOXED or holderTypeName */
    fun createDslBuilderParameter(name: String, nestedType: KDType) = when(nestedType.valueObjectType) {
        is KDValueObjectType.KDValueObjectSingle ->
            ParameterSpec.builder(name, nestedType.valueObjectType.boxedType).build()
        else -> ParameterSpec.builder(
            name,
            LambdaTypeName.get(receiver = nestedType.builderClassName, returnType = Unit::class.asTypeName())
        ).build()
    }

    /** DSL builder: `fun <t>(block: <T>Impl.Builder.() -> Unit) { ... }` */
    fun createDslBuilder(param: KDParameter, nestedType: KDType, isCollection: Boolean) =
        createDslBuilderParameter("p1", nestedType).let { blockParam ->
            FunSpec.builder(param.name.simpleName).apply {
                addParameter(blockParam)
                if (isCollection)
                    addStatement("%T().apply(%N).${KDType.BUILDER_BUILD_METHOD_NAME}().also(%N::add)", nestedType.builderClassName, blockParam, param.name)
                else
                    addStatement("%N = %T().apply(%N).${KDType.BUILDER_BUILD_METHOD_NAME}()", param.name, nestedType.builderClassName, blockParam)
            }.build()
        }

    /** DSL builder: `fun <t>(boxed: BOXED, block: <T>Impl.Builder.() -> Unit) { ... }`*/
    /*
public fun myMap(name: String, block: InnerStructImpl.Builder.() -> Unit) {
    myMap[NameImpl.create(name)] = InnerStructImpl.Builder().apply(block).build()
}*/

    fun createDslBuilder1(param: KDParameter, nestedType1: KDType, nestedType2: KDType) =
        FunSpec.builder(param.name.simpleName).apply {
            val p1 = createDslBuilderParameter("p1", nestedType1)
            val p2 = createDslBuilderParameter("p2", nestedType2)
            addParameter(p1)
            addParameter(p2)
        }.build()

    /*
    ret.name = name.value
    ret.optName = optName?.value

    ret.names = names.map { it.value }
    ret.nullableNames = nullableNames.map { it?.value }
    ret.indexes = indexes.map { it.value }.toSet()
    ret.myMap = myMap.entries.associate { it.key.value to it.value?.value }
    ret.inner = inner // no null check
     */
    class ToBuilderFun(val kdType: KDType) {
        val builder = FunSpec.builder("toBuilder")
            .returns(kdType.builderClassName)
            .addStatement("val $RET = %T()", kdType.builderClassName)

        fun add(param: String) {
            builder.addStatement("$RET.$param = $param.${KDValueObjectType.KDValueObjectSingle.PARAM_NAME}")
        }

        fun build(): FunSpec =
            builder.addStatement("return $RET").build()

        companion object {
            const val RET = "ret"
        }
    }
}
