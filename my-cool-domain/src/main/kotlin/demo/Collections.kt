package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.k3dm.Generatable
import ru.it_arch.k3dm.ValueObject

/**
 * Демонстрация использования коллекций.
 *
 */
@Generatable(json = true)
public interface Collections : ValueObject.Data {

    public val list: List<Size>
    public val size: Size
    public val nested: List<Map<Size, List<Size>>>
    //public val uuid: CommonTypes.MyUUID

    override fun validate() {

    }

    public interface Size : ValueObject.Value<Int> {
        override fun validate() {
            require(boxed in 1..100) { "Property `size` must be in range 10..100" }
        }
    }
}
