package ru.it_arch.kddd.presentation

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import ru.it_arch.kddd.domain.model.CompositeClassName
import ru.it_arch.kddd.domain.model.ILogger
import ru.it_arch.kddd.domain.model.Options
import ru.it_arch.kddd.domain.model.type.KdddType
import ru.it_arch.kddd.utils.Utils

@ConsistentCopyVisibility
public data class Visitor private constructor(
    public val resolver: Resolver,
    public val options: Options,
    public val utils: Utils,
    public val logger: ILogger
) : KSDefaultVisitor<KdddType.DataClass, Unit>() {

    private val _typeCatalog = mutableMapOf<CompositeClassName.FullClassName, KspTypeHolder>()
    public val typeCatalog: TypeCatalog
        get() = _typeCatalog.toMap()

    override fun defaultHandler(node: KSNode, data: KdddType.DataClass) {}

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KdddType.DataClass) {
        classDeclaration.declarations.filterIsInstance<KSClassDeclaration>().forEach { it.visit(this, data) }
    }

    public fun addToTypeCatalog(kdddClassName: CompositeClassName.FullClassName, holder: KspTypeHolder) {
        _typeCatalog[kdddClassName] = holder
    }

    public companion object {
        public operator fun invoke(resolver: Resolver, options: Options, utils: Utils, logger: ILogger): Visitor =
            Visitor(resolver, options, utils, logger)
    }
}
