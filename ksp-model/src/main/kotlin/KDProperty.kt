package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import ru.it_arch.k3dm.SerialName

@ConsistentCopyVisibility
public data class KDProperty private constructor(
    val name: MemberName,
    val typeName: TypeName,
    val annotation: SerialName?
) {

    val serialName: String = annotation?.value ?: name.simpleName

    public companion object {
        /** For KDType.BOXED */
        public operator fun invoke(name: MemberName, typeName: TypeName, annotation: SerialName? = null): KDProperty =
            KDProperty(name, typeName, annotation)
    }
}
