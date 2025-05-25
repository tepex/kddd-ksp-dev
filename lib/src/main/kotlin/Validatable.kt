package ru.it_arch.kddd

public interface Validatable {
    /** Should throw IllegalArgumentException on validation error */
    public fun validate()
}
