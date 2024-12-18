package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

public data class KDTypeHelper(
    val logger : KDLogger,
    val toBeGenerated: ClassName,
    val typeName: TypeName,
    val properties: List<Property>
) {

    /* Property(toBeGenerated.member(it.simpleName.asString()), it.type.toTypeName())*/
    public data class Property(
        val name: MemberName,
        val typeName: TypeName
    )
}
