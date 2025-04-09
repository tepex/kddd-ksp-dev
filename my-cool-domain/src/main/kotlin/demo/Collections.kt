package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.ValueObject

/**
 * Демонстрация использования коллекций.
 *
 */
@KDGeneratable(json = true)
public interface Collections : ValueObject.Data {

    public val list: List<Size>

    override fun validate() {

    }

    public interface Size : ValueObject.Boxed<Int> {
        override fun validate() {
            require(boxed in 10..100) { "Property `size` must be in range 10..100" }
        }
    }
}
