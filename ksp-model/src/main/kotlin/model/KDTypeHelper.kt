package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

public data class KDTypeHelper(
    val toBeGenerated: ClassName,
    val typeName: TypeName,
    val properties: List<Property>
) {

    public data class Property(
        val name: MemberName,
        val typeName: TypeName
    )
}
