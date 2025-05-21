package ru.it_arch.clean_ddd.ksp

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
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
import ru.it_arch.clean_ddd.domain.model.kddd.KdddType
import ru.it_arch.clean_ddd.domain.shortName
import ru.it_arch.clean_ddd.domain.templateToDslBuilderBody
import ru.it_arch.clean_ddd.ksp.model.ExtensionFile
import ru.it_arch.clean_ddd.ksp.model.TypeHolder

context(typeCatalog: TypeCatalog)
/**
 *  Пройтись по свойствам и сформировать:
 * 1. Свойства класса DslBuilder.
 * 2. Параметры конструктора MyTypeImpl, который возвращается методом `build()`.
 * 3. Свойства с инициализацией в методе `MyType.toDslBuilder()`
 */
internal fun Data.createDslBuildClass(typeHolder: TypeHolder, implBuilder: TypeSpec.Builder, dslFile: ExtensionFile) {

    // 1
    val builderForDslBuilderClass = ClassName.bestGuess("${impl.className.shortName}.${Data.DSL_BUILDER_CLASS_NAME}")
        .let(TypeSpec::classBuilder)
    // 3
    val dslBuilderClassName = ClassName.bestGuess("${impl.fullClassName}.${Data.DSL_BUILDER_CLASS_NAME}")
    val toDslBuilderFun = FunSpec.builder(Data.TO_DSL_BUILDER_FUN)
        .receiver(typeHolder.classType)
        .returns(dslBuilderClassName)

    typeHolder.propertyHolders.forEach { propertyHolder ->
        val typeCatalog: TypeCatalog = TypeCatalog
        when (propertyHolder.property.isCollectionType) {
            true ->
                // TODO: recursion
                PropertySpec.builder(
                    propertyHolder.property.name.boxed,
                    propertyHolder.type.mutableCollectionType
                )
            false -> {
                when(typeCatalog[propertyHolder.property.className]?.kdddType!!) {
                    is KdddType.ModelContainer -> PropertySpec.builder(
                        propertyHolder.property.name.boxed,
                        propertyHolder.type.copy(nullable = true)
                    )
                    // TODO: unbox!
                    is KdddType.Boxed -> PropertySpec.builder(
                        propertyHolder.property.name.boxed,
                        propertyHolder.type.copy(nullable = true)
                    )
                    //else -> error("Type '${propertyHolder.property.className}' not found for property '$propertyHolder.property.name'!")
                }
            }
        }.initializer(propertyHolder.property `get initializer for DSL Builder or canonical Builder` true)
            .mutable()
            .build()
            .also(builderForDslBuilderClass::addProperty)
        // 3
        templateToDslBuilderBody(
            { toDslBuilderFun.addStatement(it, dslBuilderClassName) },
            toDslBuilderFun::addStatement,
            { format, i ->
                toDslBuilderFun.addComment("for property $i")
                toDslBuilderFun.addStatement(format)
            }
        )
    }

    builderForDslBuilderClass.build().also(implBuilder::addType)
    toDslBuilderFun.build().also(dslFile.builder::addFunction)
}




internal fun Data.createDslBuildClassOld(typeHolder: TypeHolder): TypeSpec =
    ClassName.bestGuess("${impl.className.shortName}.${Data.DSL_BUILDER_CLASS_NAME}")
        .let(TypeSpec::classBuilder).apply { // DslBuiler

        }.build()

            /*
        val buildFun = FunSpec.builder(Data.BUILDER_BUILD_METHOD_NAME).returns(typeHolder.classType)
        val toDslBuilderFun = FunSpec.builder(Data.TO_DSL_BUILDER_FUN).receiver(typeHolder.classType).returns()
        typeHolder.propertyHolders.forEach { propertyHolder ->

            it.property `to DSL Builder PropertySpec with type` it.type

        }.also(::addProperties)
    }.build()*/

/*
context(typeCatalog: TypeCatalog)
private infix fun Property.`to DSL Builder PropertySpec with type`(typeName: TypeName): PropertySpec {
    //val typeCatalog: TypeCatalog = TypeCatalog

    val builder = if (isCollectionType) { "collection" }
    else {
        (typeCatalog[className]?.kdddType ?: error("Type '${className}' not found for property '$name'!"))
            .let { kdddType ->
                when(kdddType) {
                    is Data -> "Data"
                    is KdddType.Boxed -> "Boxed"
                    else -> "Other: $kdddType"
                }
            }
    }

    val typeHolder = typeCatalog[className]
    var tmp = "typeName: $typeName\ntypeCatalog[$className]: $typeHolder\n"
    if (typeHolder == null) {
        typeCatalog.entries.forEach { pair ->
            tmp += "${pair.key} -> ${pair.value.classType}\n"
        }
    }

    val typeHolderPropertyHolder = typeCatalog[className]?.propertyHolders?.firstOrNull()

    return PropertySpec.builder(
        name.boxed,
        typeName.takeIf { isCollectionType }?.mutableCollectionType ?: typeName.copy(nullable = true)
    ).initializer(this `get initializer for DSL Builder or canonical Builder` true).mutable()


        //.addKdoc("$tmp -- className: $className\ntype holder: ${typeHolderPropertyHolder?.type}")

        .build()
}*/

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
