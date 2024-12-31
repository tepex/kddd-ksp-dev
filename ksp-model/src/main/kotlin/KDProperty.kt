package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import ru.it_arch.kddd.KDSerialName

@ConsistentCopyVisibility
public data class KDProperty private constructor(
    val name: MemberName,
    val typeName: TypeName,
    val annotation: KDSerialName?
) {

    public companion object {
        /** For KDType.BOXED */
        public fun create(name: MemberName, typeName: TypeName, annotation: KDSerialName? = null): KDProperty =
            KDProperty(name, typeName, annotation)
    }
}
