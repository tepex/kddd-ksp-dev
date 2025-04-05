package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.ValueObject

/**
 * Демонстрация использования nullable примитивов в качестве полей.
 *
 * Обертки над примитивными типами создаются путем наследования от [ValueObject.Boxed] с параметром соответствующего типа.
 * Сам примитивный тип содержится в свойстве [ValueObject.Boxed.boxed].
 * Перед созданием объекта обертки вызывается метод валидации [ValueObject.Boxed.validate], который в случае невалидности значения должен выкинуть исключение. Может быть пустым при отсутствии валидации.
 * */
@KDGeneratable(json = true)
public interface NullablePrimitives : ValueObject.Data {

    public val str: StringValue? // String wrapper
    public val size: Size? // Integer wrapper
    public val boolValue: BoolValue?
    public val byteValue: ByteValue?
    public val charValue: CharValue?
    public val floatValue: FloatValue?
    public val doubleValue: DoubleValue?
    public val longValue: LongValue?
    public val shortValue: ShortValue?

    override fun validate() {}

    public interface StringValue : ValueObject.Boxed<String> {
        override fun validate() {
            require(boxed.isNotBlank()) { "Property `str` must not be blank!" }
        }
    }

    public interface Size : ValueObject.Boxed<Int> {
        override fun validate() {
            require(boxed in 10..100) { "Property `size` must be in range 10..100" }
        }
    }

    public interface BoolValue : ValueObject.Boxed<Boolean> {
        override fun validate() { }
    }

    public interface ByteValue : ValueObject.Boxed<Byte> {
        override fun validate() { }
    }

    public interface CharValue : ValueObject.Boxed<Char> {
        override fun validate() { }
    }

    public interface FloatValue : ValueObject.Boxed<Float> {
        override fun validate() { }
    }

    public interface DoubleValue : ValueObject.Boxed<Double> {
        override fun validate() { }
    }

    public interface LongValue : ValueObject.Boxed<Long> {
        override fun validate() { }
    }

    public interface ShortValue : ValueObject.Boxed<Short> {
        override fun validate() { }
    }
}
