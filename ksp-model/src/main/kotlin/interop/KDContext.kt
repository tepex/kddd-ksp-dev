package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import ru.it_arch.clean_ddd.ksp.model.KDType

public class KDContext private constructor(
    public val kdType: KDType,
    public val packageName: String,
    public val toBeGenerated: ClassName,
    public val builderFunName: String,
) {
    public val receiver: ClassName = ClassName(packageName, toBeGenerated.simpleName, KDType.Data.DSL_BUILDER_CLASS_NAME)

    public companion object {
        public fun create(
            declaration: KSClassDeclaration,
            kdType: KDType,
            generateClassName: KSClassDeclaration.() -> ClassName
        ): KDContext = KDContext(
            kdType,
            "${declaration.packageName.asString()}.impl",
            declaration.generateClassName(),
            declaration.simpleName.asString().replaceFirstChar { it.lowercaseChar() }
        )
    }
}
