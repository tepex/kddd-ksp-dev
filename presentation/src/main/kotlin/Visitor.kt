package ru.it_arch.kddd.presentation

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.visitor.KSDefaultVisitor

import ru.it_arch.kddd.domain.model.CompositeClassName
import ru.it_arch.kddd.domain.model.ILogger
import ru.it_arch.kddd.domain.model.type.KdddType
import ru.it_arch.kddd.domain.model.Options
import ru.it_arch.kddd.domain.compositeClassName
import ru.it_arch.kddd.domain.fullClassName
import ru.it_arch.kddd.domain.kDddContext
import ru.it_arch.kddd.domain.model.Property
import ru.it_arch.kddd.domain.toKDddType

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.utils.Utils

public class Visitor(
    private val resolver: Resolver,
    private val options: Options,
    private val utils: Utils,
    private val logger: ILogger
) : KSDefaultVisitor<KdddType.ModelContainer, Unit>() {

    private val _typeCatalog = mutableMapOf<CompositeClassName.FullClassName, KspTypeHolder>()
    private val typeCatalog: TypeCatalog
        get() = _typeCatalog.toMap()

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KdddType.ModelContainer) {
        classDeclaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .forEach { nestedDeclaration ->
                visitKDDeclaration(nestedDeclaration, data).onSuccess(data::addNestedType)
            }

        // TODO: to ext???
        //if (data is KDType.IEntity) data.generateBaseContract()
    }

    override fun defaultHandler(node: KSNode, data: KdddType.ModelContainer) {}

    @OptIn(KspExperimental::class)
    public fun visitKDDeclaration(declaration: KSClassDeclaration, container: KdddType.ModelContainer?): Result<KdddType> =
        declaration.typeParameters.map { resolver.getTypeArgument(it.bounds.first(), Variance.INVARIANT) }
            .let(declaration::asType).let { ksType ->

                val className = compositeClassName {
                    packageName = CompositeClassName.PackageName(declaration.packageName.asString())
                    fullClassName = ksType.toString()
                }
                //val propertyHolders = with(logger) { declaration.toPropertyHolders() }
                with(options) {
                    with(
                        kDddContext {
                            enclosing = container
                            kddd = className
                            annotations = (
                                declaration.getAnnotationsByType(KDGeneratable::class) +
                                    declaration.getAnnotationsByType(KDParsable::class)
                                ).toSet()
                            properties = utils.toProperties(declaration)
                        }//.also { logger.log("context<$declaration, kddd: ${it.kddd} container: ${container?.kddd }>") }
                    ) {
                        with(logger) {
                            declaration.superTypes.firstOrNull()?.toString()?.toKDddType() ?: run {
                                "No Kddd super type for: $declaration".let { err ->
                                    logger.err(err)
                                    Result.failure(IllegalStateException(err))
                                }
                            }
                        }
                    }
                }.onSuccess { kdddType ->
                    _typeCatalog[className.fullClassName] = KspTypeHolder(
                        kdddType,
                        ksType,
                        declaration.getAllProperties().associateBy { Property.Name(it.simpleName.asString()) }
                    )
                    if (kdddType is KdddType.ModelContainer) declaration.accept(this, kdddType)
                }
            }
}
