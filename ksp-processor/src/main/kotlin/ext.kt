package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.domain.model.CompositeClassName
import ru.it_arch.clean_ddd.domain.model.Property
import ru.it_arch.clean_ddd.domain.compositeClassName
import ru.it_arch.clean_ddd.domain.model.kddd.KdddType
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

internal fun typeHolder(block: TypeHolder.Builder.() -> Unit): TypeHolder =
    TypeHolder.Builder().apply(block).build()

// === Mappers ===

//context(logger: ILogger)
internal fun KSClassDeclaration.toPropertyHolders(typeCatalog: TypeCatalog): List<PropertyHolder> =
    getAllProperties().map { decl ->
        decl.type.toTypeName().let { propertyTypeName ->
            propertyTypeName.toString().let { str ->
                compositeClassName {
                    packageName = CompositeClassName.PackageName(decl.packageName.asString())
                    fullClassName = str.replace("?", "")
                }.let { propertyClassName ->
                    propertyHolder {
                        property = property {
                            name = Property.Name(decl.simpleName.asString())
                            packageName = propertyClassName.packageName
                            kdddType = typeCatalog[propertyClassName]?.kdddType
                                ?: error("Type '$propertyClassName' not found for property '${decl.simpleName.asString()}'!")
                            className = propertyClassName
                            isNullable = propertyTypeName.isNullable
                            collectionType = propertyTypeName.collectionType
                        }
                        type = propertyTypeName
                    }
                }
            }
        }
    }.toList()

