package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.domain.CompositeClassName
import ru.it_arch.clean_ddd.domain.ILogger
import ru.it_arch.clean_ddd.domain.core.KdddType
import ru.it_arch.clean_ddd.domain.Options
import ru.it_arch.clean_ddd.domain.compositeClassName
import ru.it_arch.clean_ddd.domain.fullClassName
import ru.it_arch.clean_ddd.domain.kDddContext
import ru.it_arch.clean_ddd.domain.toKDddType
import ru.it_arch.clean_ddd.ksp.model.TypeHolder
import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable

internal class Visitor(
    val resolver: Resolver,
    val options: Options,
    val logger: ILogger
) : KSDefaultVisitor<KdddType.ModelContainer, Unit>() {

    private val _typeCatalog = mutableMapOf<CompositeClassName.FullClassName, TypeHolder>()
    val typeCatalog: TypeCatalog
        get() = _typeCatalog.toMap()

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KdddType.ModelContainer) {
        classDeclaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .forEach { nestedDeclaration ->
                //kdLogger.log("process declaration: $classDeclaration")
                visitKDDeclaration(nestedDeclaration, data)
                    // !!! build and add it later !!!
                    ?.also(data::addNestedType)
                    ?: logger.err("Unsupported type declaration $nestedDeclaration")
            }

        // TODO: to ext
        //if (data is KDType.IEntity) data.generateBaseContract()
    }

    override fun defaultHandler(node: KSNode, data: KdddType.ModelContainer) {}

    // TODO: add impl package name


    @OptIn(KspExperimental::class)
    fun visitKDDeclaration(declaration: KSClassDeclaration, container: KdddType.ModelContainer?): KdddType? =
        declaration.typeParameters.map { resolver.getTypeArgument(it.bounds.first(), Variance.INVARIANT) }
            .let(declaration::asType).toTypeName().let { typeName ->
                val className = compositeClassName {
                    packageName = declaration.packageName.asString()
                    fullClassName = typeName.toString()
                }
                val propertyHolders = declaration.toPropertyHolders()
                with(options) {
                    with(
                        kDddContext {
                            enclosing = container
                            kddd = className
                            annotations = (
                                declaration.getAnnotationsByType(KDGeneratable::class) +
                                declaration.getAnnotationsByType(KDParsable::class)
                            ).toSet()
                            properties = propertyHolders.toProperties()
                        }//.also { logger.log("context<$declaration, kddd: ${it.kddd} container: ${container?.kddd }>") }
                    ) {
                        with(logger) {
                            declaration.superTypes.firstOrNull()?.toString()?.toKDddType() ?: run {
                                logger.log("Cant parse parent type: $declaration")
                                null
                            }
                        }
                    }
                }?.also { type ->
                    logger.log("${type.kddd.fullClassName}: impl class name: ${type.impl}")
                    _typeCatalog[className.fullClassName] = TypeHolder(typeName, propertyHolders)
                    if (type is KdddType.ModelContainer) declaration.accept(this, type)
                }
            }
}
