package ru.it_arch.kddd

public sealed interface ValueObject : Kddd {

    /** For `data class` */
    public interface Data : ValueObject {
        public fun <T: Kddd, A: Kddd> create(p1: A): T { TODO() }
        public fun <T: Kddd, A: Kddd, B: Kddd> create(p1: A, p2: B): T { TODO() }
    }

    /** For `value class` */
    public interface Boxed<BOXED : Any> : ValueObject {
        public val boxed: BOXED

        /**
         * Копирование объекта с новым значением.
         *
         * Имеет тот же смысл, что и метод `copy()` у `data class`. Обусловлен необходиммостью создовать объект на
         * уровне абстракции, чтобы иметь возможность писать логику, еще до генерации имплементации.
         * */
        public fun <T : Boxed<BOXED>> create(boxed: BOXED): T
    }

    /** For `enum class`, `sealed interface` */
    public interface Sealed : ValueObject
}
