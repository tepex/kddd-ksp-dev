package ru.it_arch.clean_ddd.ksp.model

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

@ConsistentCopyVisibility
public data class KDParameter private constructor(
    val name: MemberName,
    public val typeReference: KDReference
) {

    public companion object {
        public fun create(name: MemberName, propertyTypeName: TypeName): KDParameter =
            KDParameter(name, KDReference.create(propertyTypeName))

        public fun create(property: KDTypeHelper.Property): KDParameter =
            KDParameter(property.name, KDReference.create(property.typeName))
    }
}
