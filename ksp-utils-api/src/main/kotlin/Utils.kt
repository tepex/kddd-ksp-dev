package ru.it_arch.kddd.utils

import com.google.devtools.ksp.symbol.KSClassDeclaration
import ru.it_arch.kddd.domain.model.Property

public interface Utils {
    public fun toProperties(declaration: KSClassDeclaration): List<Property>
}
