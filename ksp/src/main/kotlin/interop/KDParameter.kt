package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

@ConsistentCopyVisibility
internal data class KDParameter private constructor(
    val name: MemberName,
    val typeReference: KDReference
) {

    companion object {
        fun create(name: MemberName, propertyTypeName: TypeName) =
            KDParameter(name, KDReference.create(propertyTypeName))

        fun create(property: KDTypeHelper.Property) =
            KDParameter(property.name, KDReference.create(property.typeName))
    }
}
