package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import ru.it_arch.kddd.KDSerialName

@ConsistentCopyVisibility
public data class KDPropertyHolder private constructor(
    val name: MemberName,
    val typeName: TypeName,
    val annotation: KDSerialName? = null
) {

    public companion object {
        /** For KDType.BOXED */
        public fun create(name: MemberName, propertyTypeName: TypeName): KDPropertyHolder =
            KDPropertyHolder(name, propertyTypeName)

        /** For other */
        public fun create(property: KDTypeContext.Property): KDPropertyHolder =
            KDPropertyHolder(property.name, property.typeName, property.annotation)
    }
}
