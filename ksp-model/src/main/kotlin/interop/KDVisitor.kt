package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.ksp.model.KDLogger
import ru.it_arch.clean_ddd.ksp.model.KDType

public abstract class KDVisitor(
    private val logger: KSPLogger,
    private val generateClassName: KSClassDeclaration.() -> ClassName
) : KSDefaultVisitor<KDType.Generatable, Unit>() {

    protected val kdLogger: KDLogger = KDLoggerImpl(logger)

    public abstract fun createBuilder(model: KDType.Model)

    public fun visitDeclaration(declaration: KSClassDeclaration): KDType? =
        declaration.kdTypeOrNull(declaration.generateClassName()).getOrElse {
            logger.warn(it.message ?: "Cant parse parent type", declaration)
            null
        }.also { if (it is KDType.Generatable) declaration.accept(this, it) }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KDType.Generatable) {
        classDeclaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .forEach { nestedDeclaration ->
                visitDeclaration(nestedDeclaration)
                    ?.also { data.addNestedType(nestedDeclaration.asType(emptyList()).toTypeName(), it) }
                    ?: logger.error("Unsupported type declaration", nestedDeclaration)
            }

        if (data is KDType.Model) {
            createBuilder(data)
            if (data is KDType.IEntity) data.build()
        }
    }

    override fun defaultHandler(node: KSNode, data: KDType.Generatable) {}
}
