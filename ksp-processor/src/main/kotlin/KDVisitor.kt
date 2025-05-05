package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.domain.ILogger
import ru.it_arch.clean_ddd.domain.KdddType
import ru.it_arch.clean_ddd.domain.Options
import ru.it_arch.clean_ddd.domain.kDddContext
import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDIgnore
import ru.it_arch.kddd.KDParsable

context(options: Options, logger: ILogger)
internal class KDVisitor(
    private val resolver: Resolver,
    private val codeGenerator: CodeGenerator,
) : KSDefaultVisitor<KdddType.ModelContainer, Unit>() {

    val options: Options = Options
    val logger: ILogger = ILogger

    private val typeCatalog = mutableSetOf<KdddType>()

    @OptIn(KspExperimental::class)
    fun generate(symbols: Sequence<KSFile>) {

        // TODO: dirty!!! refactor this 💩
        // Пакет общих файлов-расширений. Пока нет определенности, как лучше его выбрать. Пока-что берется первый попавшийся.
        //var basePackageName: ClassName.PackageName? = null

        val outputFiles = symbols.flatMap { file ->
            logger.log("process file: $file")
            file.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.INTERFACE && it.getAnnotationsByType(KDIgnore::class).count() == 0 }
                .map { declaration ->
                    //basePackageName ?: run { basePackageName = declaration toImplementationPackage options.subpackage }
                    visitKDDeclaration(declaration, null).let { kdType -> when(kdType) {
                        is KdddType.ModelContainer -> with(options) { createOutputFile(declaration, kdType) to file }
                        else            -> null
                    } }
                }.filterNotNull()
        }.toMap()

        logger.log("type catalog: $typeCatalog")

        outputFiles.keys.forEach { file ->
            buildAndAddNestedTypes(file.model)

            //createBuilders(file.model)

            // TODO if (file.generatable.hasDsl) createDslBuilderExtensionFunction(file)
            //if (file.model.hasJson) createJsonExtensionFunctions(file)
        }

        outputFiles.entries
            .forEach { it.key.fileSpecBuilder.build().replaceAndWrite(codeGenerator, Dependencies(false, it.value)) }

        //basePackageName?.let(::generateJsonProperty)
    }

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
    private fun visitKDDeclaration(declaration: KSClassDeclaration, container: KdddType.ModelContainer?): KdddType? =
        (if (declaration.typeParameters.isNotEmpty())
            declaration.typeParameters.map { resolver.getTypeArgument(it.bounds.first(), Variance.INVARIANT) }
                .also { args -> logger.log("$declaration type args: ${args.map { it.toTypeName() }}") }
        else emptyList()).let { typeArgs ->
            with(options) {
                with(kDddContext {
                    kdddClassName = declaration.asType(typeArgs).toTypeName().toString()
                    parent = container
                    annotations = (declaration.getAnnotationsByType(KDGeneratable::class) +
                        declaration.getAnnotationsByType(KDParsable::class)
                    ).toList()
                    declaration.getAllProperties().map { it.toProperty() }
                }) {
                    declaration.superTypes.firstOrNull()?.toKdddTypeOrNull() ?: run {
                        logger.log("Cant parse parent type: $declaration")
                        null
                    }
                }
            }?.also { kdddType ->
                typeCatalog += kdddType
                if (kdddType is KdddType.ModelContainer) declaration.accept(this, kdddType)
            }
        }

    /** Перехват выходного потока с построчной буферизацией. Нужно для подмены строк на выходе. Грязный хак. */
    private fun FileSpec.replaceAndWrite(codeGenerator: CodeGenerator, dependencies: Dependencies) {
        codeGenerator.createNewFile(dependencies, packageName, name).also { StringBufferedWriter(it).use(::writeTo) }
    }

    private tailrec fun buildAndAddNestedTypes(model: KdddType.ModelContainer, isFinish: Boolean = false) {
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
