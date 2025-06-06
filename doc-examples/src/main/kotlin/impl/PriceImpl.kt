package ru.it_arch.kddd.magic.impl

import ru.it_arch.kddd.ValueObject
import ru.it_arch.kddd.magic.domain.Price
import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class PriceImpl private constructor(override val boxed: BigDecimal) : Price {
    init {
        validate()
    }

    override fun toString(): String =
        boxed.toString()

    @Suppress("UNCHECKED_CAST")
    override fun <T : ValueObject.Value<BigDecimal>> fork(boxed: BigDecimal): T =
        PriceImpl(boxed) as T

    companion object {
        operator fun invoke(value: BigDecimal): Price =
            PriceImpl(value)

        fun parse(src: String): Price =
            PriceImpl(BigDecimal(src).setScale(2, RoundingMode.HALF_UP))

        fun parse(src: Double): Price =
            PriceImpl(BigDecimal(src).setScale(2, RoundingMode.HALF_UP))
    }
}
