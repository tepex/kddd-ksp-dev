package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import ru.it_arch.clean_ddd.ksp.model.KDLogger
import ru.it_arch.clean_ddd.ksp.model.KDType
import ru.it_arch.clean_ddd.ksp.model.KDTypeContext

public abstract class KDVisitor(
    private val resolver: Resolver,
    private val options: KDOptions,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : KSDefaultVisitor<KDType.Generatable, Unit>() {

    protected val kdLogger: KDLogger = KDLoggerImpl(logger)
    protected val globalKDTypes: MutableMap<TypeName, KDType> = mutableMapOf()

    public abstract fun createBuilder(model: KDType.Model)

    public fun generate(symbols: Sequence<KSFile>) {
        val outputFiles = symbols.flatMap { file ->
            file.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.INTERFACE }
                .map { declaration ->
                    visitKDDeclaration(declaration)?.let { kdType ->
                        with(options) {
                            KDOutputFile.create(declaration, kdType, file)
                        }
                    }
                }.filterNotNull()
        }.toList()

        outputFiles.forEach { file ->
            if (file.kdType is KDType.Model) {
                buildAndAddNestedTypes(file.kdType)
                createBuilder(file.kdType)
            }
        }

        outputFiles.forEach {
            it.buildFileSpec().writeTo(codeGenerator, it.dependencies)
        }
    }

    private tailrec fun buildAndAddNestedTypes(model: KDType.Model, isFinish: Boolean = false) {
        val nestedModels = model.nestedTypes.values.filterIsInstance<KDType.Model>()
        return if (nestedModels.isEmpty() || isFinish) {
            // append
            model.nestedTypes.values.filterIsInstance<KDType.Generatable>().forEach { type ->
                if (type is KDType.Model) createBuilder(type)
                model.builder.addType(type.builder.build())
            }
        } else {
            nestedModels.forEach(::buildAndAddNestedTypes)
            buildAndAddNestedTypes(model, true)
        }
    }

    private fun visitKDDeclaration(declaration: KSClassDeclaration): KDType? {
        val typeArgs = if (declaration.typeParameters.isNotEmpty()) {
            declaration.typeParameters.map { resolver.getTypeArgument(it.bounds.first(), Variance.INVARIANT) }
                .also { args -> kdLogger.log("$declaration type args: ${args.map { it.toTypeName() }}") }
        } else emptyList()

        val toBeGenerated = with(options) { declaration.implementationClassName }
        val typeName = declaration.asType(typeArgs).toTypeName()

        return with(KDTypeContext.typeContext(options, kdLogger, globalKDTypes, toBeGenerated, typeName, declaration)) {
            declaration.kdTypeOrNull().getOrElse {
                this@KDVisitor.logger.warn(it.message ?: "Cant parse parent type", declaration)
                null
            }
        }?.also { kdType ->
            globalKDTypes[typeName] = kdType
            if (kdType is KDType.Generatable) declaration.accept(this, kdType)
        }
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KDType.Generatable) {
        classDeclaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .forEach { nestedDeclaration ->
                //kdLogger.log("process declaration: $classDeclaration")
                visitKDDeclaration(nestedDeclaration)
                    // !!! build and add it later !!!
                    ?.also(data::addNestedType)
                    ?: logger.error("Unsupported type declaration", nestedDeclaration)
            }

        if (data is KDType.IEntity) data.generateBaseContract()
    }

    override fun defaultHandler(node: KSNode, data: KDType.Generatable) {}
}
