package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import ru.it_arch.kddd.KDIgnore
import ru.it_arch.kddd.domain.asDataClass
import ru.it_arch.kddd.domain.model.ILogger
import ru.it_arch.kddd.domain.model.Options
import ru.it_arch.kddd.domain.model.type.KdddType.DataClass
import ru.it_arch.kddd.domain.model.type.DataClassImpl
import ru.it_arch.kddd.domain.toStringDebug
import ru.it_arch.kddd.presentation.PreOutputFile
import ru.it_arch.kddd.presentation.Visitor
import ru.it_arch.kddd.presentation.getAnnotations
import ru.it_arch.kddd.presentation.visit
import ru.it_arch.kddd.utils.Utils

internal class KdddProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Options,
    private val utils: Utils,
    private val logger: ILogger,
    private val isTesting: Boolean
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.log("options: $options, isTesting: $isTesting")
        Visitor(resolver, options, utils, logger).also { visitor ->
            resolver.getNewFiles().toPreOutputFiles(visitor)


            visitor.typeCatalog.entries.forEach { entry ->
                logger.log("${entry.key} -> kddd: ${if (entry.value.kdddType is DataClassImpl) (entry.value.kdddType as DataClassImpl).toStringDebug() else entry.value.kdddType.toString() }")
            }

            // KspTypeHolder -> TypeHolder
        }
        return emptyList()
    }

    /** file -> PreOutputFile([DataClass], Dependencies(false, file)) */
    private fun Sequence<KSFile>.toPreOutputFiles(visitor: Visitor): List<PreOutputFile> = flatMap { file ->
        file.declarations
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.INTERFACE && it.getAnnotations<KDIgnore>().firstOrNull() == null }
            .map { decl ->
                decl.visit(visitor).mapCatching { kdddType ->
                    PreOutputFile(kdddType.asDataClass.getOrThrow(), Dependencies(false, file))
                }.onFailure { it.message?.also(logger::log) }
            }
    }.asNonNullableList

    private val Sequence<Result<PreOutputFile>>.asNonNullableList
        get() = map { it.getOrNull() }.filterNotNull().toList()

    /** Перехват выходного потока с построчной буферизацией. Нужно для подмены строк на выходе. Грязный хак.
    private fun FileSpec.replaceAndWrite(codeGenerator: CodeGenerator, dependencies: Dependencies) {
        codeGenerator.createNewFile(dependencies, packageName, name).also { StringBufferWriter(it).use(::writeTo) }
    }*/

}
