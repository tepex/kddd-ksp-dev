package ru.it_arch.kddd.presentation

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject
import ru.it_arch.kddd.domain.model.Property
import ru.it_arch.kddd.domain.model.type.KdddType

@ConsistentCopyVisibility
public data class KspTypeHolder(
    val kdddType: KdddType,
    val type: KSType,
    val propertyTypes: Map<Property.Name, KSPropertyDeclaration>
) : ValueObject.Data {

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    /*
    companion object {
        operator fun invoke(kdddType: KdddType, type: KSType): Declarations {
            type.declaration.
        }
    }*/
}
