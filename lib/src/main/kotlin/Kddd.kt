package ru.it_arch.kddd

/**
 * Общий тип KDDD-моделей.
 * */
public interface Kddd {
    /**
     * Вызывается в процессе создания объекта для его валидации.
     *
     * В случае неуспеха должен выкидывать исключение. Предполагается исрользование методов [require], [requireNotNull], и т.п.
     *
     * @throws IllegalStateException
     * */
    public fun validate()
}
