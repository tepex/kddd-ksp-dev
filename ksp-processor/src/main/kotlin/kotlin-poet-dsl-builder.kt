package ru.it_arch.clean_ddd.ksp

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import ru.it_arch.clean_ddd.domain.fullClassName
import ru.it_arch.clean_ddd.domain.`get initializer for DSL Builder or canonical Builder`
import ru.it_arch.clean_ddd.domain.isCollectionType
import ru.it_arch.clean_ddd.domain.model.Property
import ru.it_arch.clean_ddd.domain.model.kddd.Data
import ru.it_arch.clean_ddd.domain.shortName
import ru.it_arch.clean_ddd.ksp.model.TypeHolder

context(typeCatalog: TypeCatalog)
internal fun Data.createDslBuildClass(typeHolder: TypeHolder): TypeSpec =
    ClassName.bestGuess("${impl.className.shortName}.${Data.DSL_BUILDER_CLASS_NAME}").let(TypeSpec::classBuilder).apply {
        typeHolder.propertyHolders.map { it.property `to DSL Builder PropertySpec with type` it.type }
            .also(::addProperties)
    }.build()

context(typeCatalog: TypeCatalog)
private infix fun Property.`to DSL Builder PropertySpec with type`(typeName: TypeName): PropertySpec {

    //val typeCatalog: TypeCatalog = TypeCatalog

    val typeHolderPropertyHolder = typeCatalog[className]?.propertyHolders?.firstOrNull()

    return PropertySpec.builder(
        name.boxed,
        typeName.takeIf { isCollectionType }?.mutableCollectionType ?: typeName.copy(nullable = true)
    ).initializer(this `get initializer for DSL Builder or canonical Builder` true).mutable()


        .addKdoc("$name: ${className.fullClassName}\ntype holder: ${typeHolderPropertyHolder?.type}")

        .build()
}

private val TypeName.mutableCollectionType: TypeName
    get() {
        check(this is ParameterizedTypeName) { "$this is not collection type!" }
        return when(rawType) {
            SET -> MUTABLE_SET
            LIST -> MUTABLE_LIST
            MAP -> MUTABLE_MAP
            else -> error("Usupported collection type: $this")
        }.parameterizedBy(typeArguments)
    }
