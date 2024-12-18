package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import ru.it_arch.clean_ddd.ksp.model.KDLogger
import ru.it_arch.clean_ddd.ksp.model.KDType
import ru.it_arch.clean_ddd.ksp.model.KDTypeHelper

public abstract class KDVisitor(
    protected val resolver: Resolver,
    protected val globalKDTypes: MutableMap<TypeName, KDType>,
    private val logger: KSPLogger,
    private val generateClassName: KSClassDeclaration.() -> ClassName
) : KSDefaultVisitor<KDType.Generatable, Unit>() {

    protected val kdLogger: KDLogger = KDLoggerImpl(logger)

    public abstract fun createBuilder(model: KDType.Model)

    public fun visitKDDeclaration(declaration: KSClassDeclaration): KDType? {

        //val typeParams = declaration.typeParameters.flatMap { it.toTypeVariableName().bounds }

        val typeArgs = if (declaration.typeParameters.isNotEmpty()) {
            declaration.typeParameters.map { resolver.getTypeArgument(it.bounds.first(), Variance.INVARIANT) }
                .also { args -> kdLogger.log("$declaration type args: ${args.map { it.toTypeName() }}") }
        } else emptyList()
        //declaration.typeParameters.map { resolver.createKSTypeReferenceFromKSType(it.bounds.first()) }
        //kdLogger.log("visit: $declaration typ: $typeParams")
        val toBeGenerated = declaration.generateClassName()

        //val typeName = declaration.asType(emptyList()).toTypeName()
        val typeName = declaration.asType(typeArgs).toTypeName()
        //kdLogger.log("    typeName: $typeName")
        //val typeName = declaration.asType(declaration.typeParameters.map { it.toTypeVariableName() }).toTypeName()
        val helper = KDTypeHelper(
            kdLogger,
            toBeGenerated,
            typeName,
            declaration.getAllProperties()
                .map { KDTypeHelper.Property(toBeGenerated.member(it.simpleName.asString()), it.type.toTypeName()) }.toList()
        )
        return declaration.kdTypeOrNull(helper, kdLogger).getOrElse {
            logger.warn(it.message ?: "Cant parse parent type", declaration)
            null
        }?.also { kdType ->
            if (globalKDTypes.containsKey(typeName)) {
                kdLogger.log("skip: $typeName")
            } else {
                globalKDTypes[typeName] = kdType
                //kdLogger.log("added global: ${globalKDTypes.keys}")
                if (kdType is KDType.Generatable) declaration.accept(this, kdType)
            }
        }
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KDType.Generatable) {
        classDeclaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .forEach { nestedDeclaration ->
                //kdLogger.log("process declaration: $classDeclaration")
                visitKDDeclaration(nestedDeclaration)
                    ?.also(data::addNestedType)
                    ?: logger.error("Unsupported type declaration", nestedDeclaration)
            }

        // compare parameters with nested
        /*
        if (data is KDType.Model) {
            val pppp = data.parameters.flatMap { param ->
                if (param.typeReference.typeName is ClassName) listOf(param)
                else if (param.typeReference.typeName is ParameterizedTypeName) param.typeReference.typeName.typeArguments.map {  }
                else -> error("")
            }
            val params = data.parameters.map { "${it.typeReference.typeName} -> ${it.typeReference.typeName::class.java.simpleName}" }
            val nested = data.nestedTypes.keys
            kdLogger.log("${data.sourceTypeName}: params: $params")
            kdLogger.log("         nested: $nested")
        }*/

        if (data is KDType.Model) {
            createBuilder(data)
            if (data is KDType.IEntity) data.generateBaseContract()
        }
        //data.finish()
    }

    override fun defaultHandler(node: KSNode, data: KDType.Generatable) {}
}
