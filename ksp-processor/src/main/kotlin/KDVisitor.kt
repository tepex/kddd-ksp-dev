package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import kotlinx.serialization.json.Json
import ru.it_arch.clean_ddd.ksp_model.utils.KDLogger
import ru.it_arch.clean_ddd.ksp_model.model.KDOptions
import ru.it_arch.clean_ddd.ksp_model.model.KDOutputFile
import ru.it_arch.clean_ddd.ksp_model.model.KDType
import ru.it_arch.clean_ddd.ksp_model.simpleName
import ru.it_arch.clean_ddd.ksp_model.utils.OptIn as MyOptIn

internal abstract class KDVisitor(
    private val resolver: Resolver,
    private val options: KDOptions,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : KSDefaultVisitor<KDType.Generatable, Unit>() {

    protected val kdLogger: KDLogger = KDLoggerImpl(logger)
    private val typeCatalog = mutableSetOf<KDType>()
    abstract fun createBuilders(model: KDType.Model)

    fun generate(symbols: Sequence<KSFile>) {

        // TODO: dirty!!! refactor this 💩
        var packageName: String? = null

        val outputFiles = symbols.flatMap { file ->
            file.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.INTERFACE }
                .map { declaration ->
                    //packageName = options.getImplementationPackage(declaration.packageName.asString()).boxed
                    packageName = "TODO"
                    visitKDDeclaration(declaration).takeIf { it is KDType.Generatable }?.let { kdType ->
                        with(options) {
                            createOutputFile(declaration, kdType as KDType.Generatable) to file
                        }
                    }
                }.filterNotNull()
        }.toMap()

        outputFiles.keys.filter { it.generatable is KDType.Model }.forEach { file ->
            buildAndAddNestedTypes(file.generatable as KDType.Model)

            createBuilders(file.generatable as KDType.Model)

            // TODO if (file.generatable.hasDsl) createDslBuilderExtensionFunction(file)
            if (file.generatable.hasJson) createJsonExtensionFunctions(file)
        }

        outputFiles.entries
            .forEach { it.key.fileSpecBuilder.build().replaceAndWrite(codeGenerator, Dependencies(false, it.value)) }

        packageName?.let(::generateJsonProperty)
    }

    /** Перехват выходного потока с построчной буферизацией. Нужно для подмены строк на выходе. Грязный хак. */
    private fun FileSpec.replaceAndWrite(codeGenerator: CodeGenerator, dependencies: Dependencies) {
        codeGenerator.createNewFile(dependencies, packageName, name).also { StringBufferedWriter(it).use(::writeTo) }
    }

    private fun generateJsonProperty(packageName: String) {
        FileSpec.builder(packageName, "json").apply {
            StringBuilder("Json {«\nprettyPrint = true\n").also { sb ->
                options.jsonNamingStrategy?.let { sb.append("namingStrategy = ${it.className}") }
                sb.append("\n»}")
                PropertySpec.builder("json", Json::class)
                    .addAnnotation(AnnotationSpec.builder(MyOptIn::class).addMember("ExperimentalSerializationApi::class").build())
                    .initializer(sb.toString()).build().also(::addProperty)
            }
        }.build().replaceAndWrite(codeGenerator, Dependencies(false))
    }

    private fun createJsonExtensionFunctions(file: KDOutputFile) {
        FunSpec.builder("toJson").apply {
            receiver(file.generatable.name)
            returns(String::class)
            addStatement("return json.encodeToString(this as ${file.generatable.classNameRef})")
        }.build().also(file.fileSpecBuilder::addFunction)

        FunSpec.builder("to${file.generatable.name.simpleName}").apply {
            receiver(String::class)
            returns(file.generatable.name)
            // json.decodeFromString<PrimitivesImpl>(this)
            addStatement("return json.decodeFromString<${file.generatable.classNameRef}>(this)")
        }.build().also(file.fileSpecBuilder::addFunction)
    }

    private tailrec fun buildAndAddNestedTypes(model: KDType.Model, isFinish: Boolean = false) {
        val nestedModels = model.nestedTypes.filterIsInstance<KDType.Model>()
        return if (nestedModels.isEmpty() || isFinish) {
            // append
            model.nestedTypes.filterIsInstance<KDType.Generatable>().forEach { type ->
                if (type is KDType.Model) createBuilders(type)
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

        val typeName = declaration.asType(typeArgs).toTypeName()
        val context = with(options) {
            with(kdLogger) {
                typeContext(declaration, typeCatalog.toSet(), typeName)
            }
        }

        return with(context) {
            declaration.kdTypeOrNull(kdLogger).getOrElse {
                this@KDVisitor.logger.warn(it.message ?: "Cant parse parent type", declaration)
                null
            }
        }?.also { kdType ->
            typeCatalog[typeName] = kdType
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
