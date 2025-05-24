package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.domain.PropertyHolder
import ru.it_arch.clean_ddd.domain.fullClassName
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
            property {
                name = Property.Name(decl.simpleName.asString())
                //className = propertyTypeName.fullClassName
                type = propertyTypeName.toPropertyType()
                isNullable = propertyTypeName.isNullable
                //collectionType = propertyTypeName.collectionType
            }
        }
    }.toList()

internal val TypeName.fullClassName: CompositeClassName.FullClassName
    get() = CompositeClassName.FullClassName(toString().replace("?", ""))

internal fun TypeCatalog.getTypeHolderOrError(className: CompositeClassName.FullClassName): TypeHolder =
    this[className] ?: error("Type ${className} not found in type catalog!")

internal typealias PropertyTypeHolder = Triple<Property.Name, KdddType, TypeName>

context(typeCatalog: TypeCatalog)
// Исключение для коллекц
internal fun TypeHolder.getPropertyHolders(): List<PropertyTypeHolder> =
    propertyTypes.entries.map { entry ->
        //val typeCatalog: TypeCatalog = TypeCatalog
        // entry.value.fullClassName -- can be collection
        typeCatalog.getTypeHolderOrError(entry.value.fullClassName)
            .let { PropertyTypeHolder(entry.key, it.kdddType, it.classType) }
    }

internal val PropertyTypeHolder.propertyHolder: PropertyHolder
    get() = PropertyHolder(first, second)

internal fun List<PropertyTypeHolder>.getTypeName(name: Property.Name): TypeName? =
    find { it.first == name }?.third

/*
private val TypeName.collectionType: Property.CollectionType?
    get() = if (this is ParameterizedTypeName) when(rawType) {
        SET -> Property.CollectionType.SET
        LIST -> Property.CollectionType.LIST
        MAP -> Property.CollectionType.MAP
        else -> null
    } else null*/

// https://discuss.kotlinlang.org/t/3-tailrec-questions/3981 #3
@Suppress("NON_TAIL_RECURSIVE_CALL")
private tailrec fun TypeName.toPropertyType(): Property.PropertyType =
    if (this !is ParameterizedTypeName) Property.PropertyType.PropertyElement(fullClassName)
    else when(rawType) {
            SET -> {
                check(typeArguments.size == 1)
                typeArguments.first().toPropertyType()
                    .let(Property.PropertyType.PropertyCollection::PropertySet)
            }
            LIST -> {
                check(typeArguments.size == 1)
                typeArguments.first().toPropertyType()
                    .let(Property.PropertyType.PropertyCollection::PropertyList)
            }
            MAP -> {
                check(typeArguments.size == 2)
                Property.PropertyType.PropertyCollection.PropertyMap(
                    typeArguments[0].toPropertyType(),
                    typeArguments[1].toPropertyType()
                )
            }
            else -> error("Unsupported collection type: $rawType")
        }
