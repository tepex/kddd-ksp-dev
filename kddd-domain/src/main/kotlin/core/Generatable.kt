package ru.it_arch.clean_ddd.domain.core

import ru.it_arch.clean_ddd.domain.CompositeClassName
import ru.it_arch.clean_ddd.domain.core.KdddType.ModelContainer
import ru.it_arch.kddd.ValueObject

public interface Generatable : ValueObject.Data {
    public val kddd: CompositeClassName.ClassName
    public val impl: CompositeClassName.ClassName
    public val enclosing: ModelContainer?
}
