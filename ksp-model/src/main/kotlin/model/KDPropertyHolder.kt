package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

@ConsistentCopyVisibility
public data class KDPropertyHolder private constructor(
    val name: MemberName,
    val typeName: TypeName
) {

    public companion object {
        /** For KDType.BOXED */
        public fun create(name: MemberName, propertyTypeName: TypeName): KDPropertyHolder =
            KDPropertyHolder(name, propertyTypeName)

        /** For other */
        public fun create(property: KDTypeHelper.Property): KDPropertyHolder =
            KDPropertyHolder(property.name, property.typeName)
    }
}
