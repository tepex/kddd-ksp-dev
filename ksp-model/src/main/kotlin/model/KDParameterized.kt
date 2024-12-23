package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

internal class KDParameterized(
    val node: ParameterizedTypeName
) {
    var args: List<TypeName> = node.typeArguments

    var isSubstituted = false

    fun substitute(transform: (TypeName) -> TypeName) {
        args = args.map(transform)
    }

    fun terminate(holder: KDType.Model): KDParameterized =
        (node.rawType.toMutableCollection()
            .parameterizedBy(args.takeIf { isSubstituted } ?: args.map{ it.substituteArg(holder) }))
            .let(::KDParameterized)
}
