package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName

@ConsistentCopyVisibility
internal data class KDParameter private constructor(
    val name: MemberName,
    val typeReference: KDReference
) {

    companion object {
        fun create(name: MemberName, property: KSPropertyDeclaration) =
            KDParameter(name, KDReference.create(property.type.toTypeName()))

        fun create(name: MemberName, typeName: TypeName) =
            KDParameter(name, KDReference.create(typeName))
    }
}
