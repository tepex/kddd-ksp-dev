package ru.it_arch.kddd

public sealed interface ValueObject : Kddd {

    /** For `data class` */
    public interface Data : ValueObject {
        public fun <T: Kddd, A: Kddd> fork(vararg args: A): T
    }

    /** For `value class` */
    public interface Value<BOXED : Any> : ValueObject {
        public val boxed: BOXED

        /**
         * Копирование объекта с новым значением.
         *
         * Имеет тот же смысл, что и метод `copy()` у `data class`. Обусловлен необходимостью создовать объект на
         * уровне абстракции, чтобы иметь возможность писать логику, еще до генерации имплементации.
         * */
        public fun <T : Value<BOXED>> fork(boxed: BOXED): T
    }

    /** For `enum class`, `sealed interface` */
    public interface Sealed : ValueObject
}
