package ru.it_arch.kddd.magic

import ru.it_arch.kddd.magic.domain.Price
import ru.it_arch.kddd.magic.domain.asString
import ru.it_arch.kddd.magic.impl.PriceImpl
import java.text.NumberFormat
import java.util.Locale

inline fun Price.asString(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.of("ru", "RU"))
    return formatter.format(boxed)
}

fun examplePrice() {
    val price1 = PriceImpl.parse("3.4456")
    val price2 = PriceImpl.parse("3000.2498")
    val price3 = price2 / price1
    println("price1: $price1, price2: ${price2.asString()}")
    println("result: ${price3.asString()}")
}
