package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.domain.CompositeClassName
import ru.it_arch.clean_ddd.domain.ILogger
import ru.it_arch.clean_ddd.domain.Property
import ru.it_arch.clean_ddd.domain.core.KdddType
import ru.it_arch.clean_ddd.domain.property
import ru.it_arch.clean_ddd.ksp.model.ExtensionFile
import ru.it_arch.clean_ddd.ksp.model.PropertyHolder
import ru.it_arch.clean_ddd.ksp.model.TypeHolder

// === Type aliases ===

/**
 * */
internal typealias TypeCatalog = Map<CompositeClassName, TypeHolder>

/**
 * */
internal typealias OutputFile = Pair<KdddType.ModelContainer, Dependencies>


// === Builders ===

internal fun propertyHolder(block: PropertyHolder.Builder.() -> Unit): PropertyHolder =
    PropertyHolder.Builder().apply(block).build()

internal fun extensionFile(block: ExtensionFile.Builder.() -> Unit): ExtensionFile =
    ExtensionFile.Builder().apply(block).build()

// === Mappers ===

context(logger: ILogger)
internal fun KSClassDeclaration.toPropertyHolders(): List<PropertyHolder> =
    getAllProperties().map { decl ->
        decl.type.toTypeName().let { propertyTypeName ->
            propertyHolder {
                property = property {
                    name = Property.Name(decl.simpleName.asString())
                    packageName = CompositeClassName.PackageName(decl.packageName.asString())
                    className = propertyTypeName.toString()
                    isNullable = propertyTypeName.isNullable
                    collectionType = propertyTypeName.collectionType
                }
                type = propertyTypeName
            }
        }
    }.toList()

