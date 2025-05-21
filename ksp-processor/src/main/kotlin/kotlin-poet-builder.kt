package ru.it_arch.clean_ddd.ksp

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import ru.it_arch.clean_ddd.domain.fullClassName
import ru.it_arch.clean_ddd.domain.`get initializer for DSL Builder or canonical Builder`
import ru.it_arch.clean_ddd.domain.isCollectionType
import ru.it_arch.clean_ddd.domain.model.Property
import ru.it_arch.clean_ddd.domain.model.kddd.Data
import ru.it_arch.clean_ddd.domain.shortName
import ru.it_arch.clean_ddd.domain.templateBuilderBodyCheck
import ru.it_arch.clean_ddd.domain.templateBuilderFunBuildReturn
import ru.it_arch.clean_ddd.domain.templateToBuilderBody
import ru.it_arch.clean_ddd.ksp.model.TypeHolder

internal fun Data.createBuildClass(typeHolder: TypeHolder): TypeSpec =
    ClassName.bestGuess("${impl.className.shortName}.${Data.BUILDER_CLASS_NAME}").let(TypeSpec::classBuilder).apply {
        // add properties
        typeHolder.propertyHolders.map { it.property `to Builder PropertySpec with type` it.type }
            .also(::addProperties)

        // fun build(): KdddType
        FunSpec.builder(Data.BUILDER_BUILD_METHOD_NAME).apply {
            returns(typeHolder.classType)
            //add `checkNotNull(<property>)`
            templateBuilderBodyCheck { format, i ->
                addStatement(format, properties[i].name.boxed, Data.BUILDER_CLASS_NAME, properties[i].name.boxed)
            }
            // `return <MyTypeImpl>(p1 = p1, p2 = p2, ...)`
            templateBuilderFunBuildReturn(
                { addStatement(it, impl.className.shortName) },
                { format, i -> addStatement(format, properties[i].name.boxed, properties[i].name.boxed) }
            )
        }.build().also(::addFunction)
    }.build()

internal fun Data.createToBuilderFun(typeHolder: TypeHolder): FunSpec =
    ClassName.bestGuess("${impl.fullClassName}.${Data.BUILDER_CLASS_NAME}").let { builderClass ->
        FunSpec.builder(Data.TO_BUILDER_FUN).apply {
            receiver(typeHolder.classType)
            returns(builderClass)
            templateToBuilderBody { addStatement(it, builderClass) }
        }.build()
    }

private infix fun Property.`to Builder PropertySpec with type`(typeName: TypeName): PropertySpec =
    PropertySpec.builder(name.boxed, typeName.copy(nullable = isCollectionType.not()))
        .initializer(this `get initializer for DSL Builder or canonical Builder` false).mutable().build()

