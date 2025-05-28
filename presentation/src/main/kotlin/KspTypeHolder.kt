package ru.it_arch.kddd.presentation

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject
import ru.it_arch.kddd.domain.model.type.KdddType

/**
 * */
@ConsistentCopyVisibility
public data class KspTypeHolder private constructor(
    val kdddType: KdddType,
    val type: KSType,
    val propertyTypes: List<KSPropertyDeclaration>
) : ValueObject.Data {

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    public companion object {
        public operator fun invoke(kdddType: KdddType, type: KSType, propertyTypes: List<KSPropertyDeclaration>): KspTypeHolder =
            KspTypeHolder(kdddType, type, propertyTypes)
    }
}
