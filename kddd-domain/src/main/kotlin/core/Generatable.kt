package ru.it_arch.clean_ddd.domain.core

import ru.it_arch.clean_ddd.domain.core.KdddType.ModelContainer
import ru.it_arch.kddd.ValueObject

public interface Generatable : ValueObject.Data {
    public val kddd: CompositeClassName
    public val implClassName: CompositeClassName.ClassName
    public val implPackageName: CompositeClassName.PackageName
    public val enclosing: ModelContainer?
}
