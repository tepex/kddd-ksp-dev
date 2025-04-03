package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.ValueObject

@KDGeneratable(json = true)
public interface Primitives : ValueObject.Data {

    public val str: StringValue
    public val size: Size

    override fun validate() {
        require(str.boxed.length < size.boxed) { "`str` length must be < `size`" }
    }

    public interface StringValue : ValueObject.Boxed<String> {
        override fun validate() {
            require(boxed.isNotBlank()) { "Property `str` must not be blank!" }
        }
    }

    public interface Size : ValueObject.Boxed<Short> {
        override fun validate() {
            require(boxed in 10..100) { "Property `size` must be in range 10..100" }
        }
    }
}
