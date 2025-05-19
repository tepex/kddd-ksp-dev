package ru.it_arch.clean_ddd.domain.model.kddd

import ru.it_arch.clean_ddd.domain.model.CompositeClassName
import ru.it_arch.kddd.ValueObject

public interface Generatable : ValueObject.Data {
    public val kddd: CompositeClassName
    public val impl: CompositeClassName
    public val enclosing: KdddType.ModelContainer?
}
