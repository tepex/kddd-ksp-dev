package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.ValueObject

/**
 * Демонстрация использования коллекций.
 *
 */
@KDGeneratable(json = false)
public interface Collections : ValueObject.Data {

    override fun validate() {

    }
}
