package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.domain.ILogger
import ru.it_arch.clean_ddd.domain.KdddType
import ru.it_arch.clean_ddd.domain.Options
import ru.it_arch.clean_ddd.domain.compositeClassName
import ru.it_arch.clean_ddd.domain.kDddContext
import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable

internal class Visitor(
    val resolver: Resolver,
    val options: Options,
    val logger: ILogger
) : KSDefaultVisitor<KdddType.ModelContainer, Unit>() {

    private val _typeCatalog = mutableSetOf<KdddType>()
    val typeCatalog: Set<KdddType>
        get() = _typeCatalog.toSet()

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

    @OptIn(KspExperimental::class)
    fun visitKDDeclaration(declaration: KSClassDeclaration, container: KdddType.ModelContainer?): KdddType? =
        (if (declaration.typeParameters.isNotEmpty())
            declaration.typeParameters.map { resolver.getTypeArgument(it.bounds.first(), Variance.INVARIANT) }
                .also { args -> logger.log("$declaration type args: ${args.map { it.toTypeName() }}") }
        else emptyList()).let { typeArgs ->
            val kdddTypeName = declaration.asType(typeArgs).toTypeName()
            val fullClassName = compositeClassName {
                packageName = declaration.packageName.asString()
                className = kdddTypeName.toString()
            }
            with(options) {
                with(kDddContext {
                    kddd = fullClassName
                    parent = container
                    annotations = (declaration.getAnnotationsByType(KDGeneratable::class) +
                        declaration.getAnnotationsByType(KDParsable::class)
                    ).toList()
                    properties = declaration.getAllProperties().map { propDecl ->
                        val typeName = propDecl.type.toTypeName() // -> _typeCatalog
                        propDecl.toProperty()
                    }.toList()
                }.also { logger.log("context<$declaration, container: ${container?.kddd }>") }) {
                    declaration.superTypes.firstOrNull()?.toKdddTypeOrNull() ?: run {
                        logger.log("Cant parse parent type: $declaration")
                        null
                    }
                }
            }?.also { type ->
                logger.log("type: $type")
                _typeCatalog += type
                if (type is KdddType.ModelContainer) declaration.accept(this, type)
            }
        }

    /** Перехват выходного потока с построчной буферизацией. Нужно для подмены строк на выходе. Грязный хак. */
    private fun FileSpec.replaceAndWrite(codeGenerator: CodeGenerator, dependencies: Dependencies) {
        codeGenerator.createNewFile(dependencies, packageName, name).also { StringBufferedWriter(it).use(::writeTo) }
    }

    tailrec fun buildAndAddNestedTypes(model: KdddType.ModelContainer, isFinish: Boolean = false) {
        val nestedModels = model.nestedTypes.filterIsInstance<KdddType.ModelContainer>()
        return if (nestedModels.isEmpty() || isFinish) {
            // append
            model.nestedTypes.filterIsInstance<KdddType.Generatable>().forEach { type ->
                //if (type is KDType.Model) createBuilders(type)

                // TODO:
                // model.builder.addType(type.builder.build())
            }
        } else {
            nestedModels.forEach(::buildAndAddNestedTypes)
            buildAndAddNestedTypes(model, true)
        }
    }
}
