package ru.it_arch.kddd

public sealed interface ValueObject {

    /** For `data class` */
    public interface Data : ValueObject, Validatable

    /** For `value class` */
    public interface Boxed<BOXED : Any> : ValueObject, Validatable {
        public val boxed: BOXED
        public fun <T : Boxed<BOXED>> copy(boxed: BOXED): T
    }

    /** For `enum class`, `sealed interface` */
    public interface Sealed : ValueObject
}
