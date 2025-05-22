package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.domain.model.CompositeClassName
import ru.it_arch.clean_ddd.domain.model.ILogger
import ru.it_arch.clean_ddd.domain.model.Property
import ru.it_arch.clean_ddd.domain.model.kddd.KdddType
import ru.it_arch.clean_ddd.domain.property
import ru.it_arch.clean_ddd.ksp.model.ExtensionFile
import ru.it_arch.clean_ddd.ksp.model.TypeHolder

// === Type aliases ===

/**
 * */
internal typealias TypeCatalog = Map<CompositeClassName.FullClassName, TypeHolder>

/**
 * */
internal typealias OutputFile = Pair<KdddType.ModelContainer, Dependencies>


// === Builders ===

internal fun extensionFile(block: ExtensionFile.Builder.() -> Unit): ExtensionFile =
    ExtensionFile.Builder().apply(block).build()

internal fun typeHolder(block: TypeHolder.Builder.() -> Unit): TypeHolder =
    TypeHolder.Builder().apply(block).build()

// === Mappers ===

internal fun KSClassDeclaration.toProperties(): List<Property> =
    getAllProperties().map { decl ->
        decl.type.toTypeName().let { propertyTypeName ->
            propertyTypeName.toString().let { str ->
                property {
                    name = Property.Name(decl.simpleName.asString())
                    className = CompositeClassName.FullClassName(str.replace("?", ""))
                    isNullable = propertyTypeName.isNullable
                    collectionType = propertyTypeName.collectionType
                }
            }
        }
    }.toList()
