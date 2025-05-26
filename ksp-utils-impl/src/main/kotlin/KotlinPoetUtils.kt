package ru.it_arch.kddd.utils

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.kddd.domain.model.CompositeClassName
import ru.it_arch.kddd.domain.model.Property
import ru.it_arch.kddd.domain.property

internal class KotlinPoetUtils : Utils {

    override fun toProperties(declaration: KSClassDeclaration): List<Property> =
        declaration.getAllProperties().map { decl ->
            decl.type.toTypeName().let { propertyTypeName ->
                property {
                    name = Property.Name(decl.simpleName.asString())
                    type = propertyTypeName.toPropertyType()
                    isNullable = propertyTypeName.isNullable
                }
            }
        }.toList()

    // https://discuss.kotlinlang.org/t/3-tailrec-questions/3981 #3
    @Suppress("NON_TAIL_RECURSIVE_CALL")
    private tailrec fun TypeName.toPropertyType(): Property.PropertyType =
        if (this !is ParameterizedTypeName)
            Property.PropertyType.PropertyElement(CompositeClassName.FullClassName(toString().replace("?", "")))
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
}
