package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

internal class EntityBuilder(
    private val holder: KDType.Model,
    logger: KDLogger
) {
    /** override fun toString() */
    private val toStringFun = FunSpec.builder("toString")
        .addModifiers(KModifier.OVERRIDE)
        .returns(String::class)

    init {
        holder.parameters
            .fold(mutableListOf<Pair<String, MemberName>>()) { acc, param -> acc.apply { add("%N: $%N" to param.name) } }
                .let { it.joinToString() { pair -> pair.first } to it.fold(mutableListOf<MemberName>()) { acc, pair ->
                    acc.apply {
                        add(pair.second)
                        add(pair.second)
                    }
                } }
                .also { toStringFun.addStatement("return \"[ID: , ${it.first}]\"", *it.second.toTypedArray()) }
        toStringFun.build().also(holder.builder::addFunction)
    }
}
