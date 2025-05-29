package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDIgnore
import ru.it_arch.kddd.ValueObject

/**
 * Демонстрация использования примитивов в качестве полей.
 *
 * Обертки над примитивными типами создаются путем наследования от [ValueObject.Value] с параметром соответствующего типа.
 * Сам примитивный тип содержится в свойстве [ValueObject.Value.boxed].
 * Перед созданием объекта обертки вызывается метод валидации [ValueObject.Value.validate], который в случае невалидности значения должен выкинуть исключение. Может быть пустым при отсутствии валидации.
 * */
@KDIgnore
@KDGeneratable(json = true)
public interface Primitives : ValueObject.Data {

    public val str: StringValue // String wrapper
    public val size: Size // Integer wrapper
    public val boolValue: BoolValue
    public val byteValue: ByteValue
    public val charValue: CharValue
    public val floatValue: FloatValue
    public val doubleValue: DoubleValue
    public val longValue: LongValue
    public val shortValue: ShortValue

    override fun validate() {
        require(str.boxed.length < size.boxed) { "`str` length must be < `size`" }
    }

    public interface StringValue : ValueObject.Value<String> {
        override fun validate() {
            require(boxed.isNotBlank()) { "Property `str` must not be blank!" }
        }
    }

    public interface Size : ValueObject.Value<Int> {
        override fun validate() {
            require(boxed in 10..100) { "Property `size` must be in range 10..100" }
        }
    }

    public interface BoolValue : ValueObject.Value<Boolean> {
        override fun validate() { }
    }

    public interface ByteValue : ValueObject.Value<Byte> {
        override fun validate() { }
    }

    public interface CharValue : ValueObject.Value<Char> {
        override fun validate() { }
    }

    public interface FloatValue : ValueObject.Value<Float> {
        override fun validate() { }
    }

    public interface DoubleValue : ValueObject.Value<Double> {
        override fun validate() { }
    }

    public interface LongValue : ValueObject.Value<Long> {
        override fun validate() { }
    }

    public interface ShortValue : ValueObject.Value<Short> {
        override fun validate() { }
    }
}
