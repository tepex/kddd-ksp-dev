package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

internal data class KDClassDeclaration(
    val implClassName: ClassName,
    val typeName: TypeName,
    val properties: List<Property>
) {

    data class Property(
        val name: MemberName,
        val typeName: TypeName
    )
}
