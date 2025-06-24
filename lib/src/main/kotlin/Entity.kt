package ru.it_arch.kddd

public interface Entity : Kddd {
    public val id: ValueObject
    public var content: ValueObject.Data
}
