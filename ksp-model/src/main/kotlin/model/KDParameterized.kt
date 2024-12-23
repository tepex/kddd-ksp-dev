package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName

internal class KDParameterized(
    val node: ParameterizedTypeName
) {
    private var args: List<TypeName> = node.typeArguments

    private var _isSubstituted = false
    val isSubstituted: Boolean
        get() = _isSubstituted

    fun substitute(transform: (TypeName) -> TypeName) {
        args = args.map(transform)
        _isSubstituted = true
    }

    fun terminate(): KDParameterized =
        node.rawType.toMutableCollection().parameterizedBy(args).let(::KDParameterized)
            .also { _isSubstituted = true }

    private companion object {
        fun ClassName.toMutableCollection() = when (this) {
            LIST -> MUTABLE_LIST
            SET -> MUTABLE_SET
            MAP -> MUTABLE_MAP
            else -> error("Unsupported collection for mutable: $this")
        }
    }
}
