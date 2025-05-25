package ru.it_arch.kddd.domain

import ru.it_arch.kddd.domain.internal.hasDsl
import ru.it_arch.kddd.domain.internal.toGeneratable
import ru.it_arch.kddd.domain.internal.toBoxedTypeWith
import ru.it_arch.kddd.domain.model.CompositeClassName
import ru.it_arch.kddd.domain.model.type.BoxedWithCommon
import ru.it_arch.kddd.domain.model.type.Data
import ru.it_arch.kddd.domain.model.type.IEntity
import ru.it_arch.kddd.domain.model.type.KdddType
import ru.it_arch.kddd.domain.model.Context
import ru.it_arch.kddd.domain.model.ILogger
import ru.it_arch.kddd.domain.model.Options
import ru.it_arch.kddd.domain.model.Property


public fun Map<String, String>.toOptions(): Options = options {
    subpackage = get(Options.OPTION_IMPLEMENTATION_SUBPACKAGE)
    generatedClassNameRe = get(Options.OPTION_GENERATED_CLASS_NAME_RE)
    generatedClassNameResult = get(Options.OPTION_GENERATED_CLASS_NAME_RESULT)
    useContextParameters = get(Options.OPTION_CONTEXT_PARAMETERS)?.toBooleanStrictOrNull()
    jsonNamingStrategy = get(Options.OPTION_JSON_NAMING_STRATEGY)
}

context(ctx: Context, _: Options, logger: ILogger)
public fun String.toKDddType(): KdddType = ctx.toGeneratable().let { generatable ->
    //val ctx: Context = Context
    when(substringBefore('<')) {
        Data::class.java.simpleName           -> Data(generatable, ctx.properties, ctx.hasDsl)
        IEntity::class.java.simpleName        -> IEntity(Data(generatable, ctx.properties, ctx.hasDsl))
        KdddType.Boxed::class.java.simpleName -> this toBoxedTypeWith generatable
        else                                  -> error("Unknown type: $this")
    }
}

public val CompositeClassName.fullClassName: CompositeClassName.FullClassName
    get() = CompositeClassName.FullClassName("$packageName.$className")

public val CompositeClassName.ClassName.shortName: String
    get() = boxed.substringAfterLast('.')

public fun Data.getProperty(name: Property.Name): Property =
    properties.find { it.name == name } ?: error("Property $name not found in ${kddd.fullClassName}!")

/* === Templates for KotlinPoet addStatement === */

public fun KotlinCodeBoxedBuilder.generateBoxed() {
    generateImplementationClass()
    generateProperty()
    generateConstructor()
    generateToString()
    generateFork()
    generateCompanion()
}

public fun KotlinCodeDataBuilder.generateData() {
    generateImplementationClass()
    generateProperties()
    generateConstructor()


}

public fun KotlinCodeEntityBuilder.generateEntity() {
    generateImplementationClass()
}


/**
 * 1. <CommonType>(src).let(::MyTypeImpl)
 * 2. <CommonType>.<deserialization static method>(src).let(::MyTypeImpl)
 * */
public val BoxedWithCommon.templateParseBody: String
    get() = "return %T${deserializationMethod.boxed.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""}(%N).let(::%N)"

/** KotlinPoet format */
public typealias SimpleStatement = (String) -> Unit
/** KotlinPoet format, Property */
public typealias PropertyStatement = (String, Property) -> Unit
/** KotlinPoet format, property index */
public typealias IndexedStatement = (String, Int) -> Unit
public typealias PropertyHolder = Pair<Property.Name, KdddType>

public fun Data.templateForkBody(simpleStatement: SimpleStatement, indexedStatement: IndexedStatement) {
    simpleStatement("val ret = ${Data.BUILDER_CLASS_NAME}().apply {⇥")
    properties.forEachIndexed { i, _ ->
        indexedStatement("%N = args[$i] as %T", i)
    }
    simpleStatement("⇤}.build() as T")
    simpleStatement("return ret")
}

public fun Data.templateBuilderBodyCheck(statement: PropertyStatement) {
    properties.filter { it.isNullable.not() && it.type is Property.PropertyType.PropertyElement }.forEach { property ->
        statement("""checkNotNull(%N) { "Property '%N.%N' must be initialized!" }""", property)
    }
}

public fun Data.templateBuilderFunBuildReturn(simpleStatement: SimpleStatement, propertyStatement: PropertyStatement) {
    simpleStatement("return %N(⇥")
    properties.forEachIndexed { i, property ->
        StringBuilder("%N = %N").apply {
            "!!".takeIf { property.isNullable.not() && property.type is Property.PropertyType.PropertyElement }?.let(::append)
            ", ".takeIf { i < (properties.size - 1) }?.let(::append)
        }.also { propertyStatement(it.toString(), property) }
    }
    simpleStatement("⇤)")
}

public fun Data.templateToBuilderBody(statement: SimpleStatement) {
    StringBuilder("return %T().apply {\n⇥").apply {
        properties.forEach { property ->
            append("this.${property.name} = ${property.name}\n")
        }
        append("⇤}")
    }.also { statement(it.toString()) }
}

public fun Data.templateDslBuilder(
    propertyHolders: List<PropertyHolder>,
    collectionType: (Property.Name) -> Unit,
    boxedType: (Property.Name) -> Unit,
    dataType: (Property.Name) -> Unit
) {
    propertyHolders.forEach { holder ->
        if (getProperty(holder.first).type is Property.PropertyType.PropertyCollection) {
            collectionType(holder.first)
        } else {
            when(holder.second) {
                is KdddType.ModelContainer -> dataType(holder.first)
                is KdddType.Boxed -> boxedType(holder.first)
            }
        }
    }
}

public fun Data.templateToDslBuilderBody(startStatement: SimpleStatement, endStatement: SimpleStatement, propertyStatement: PropertyStatement) {
    startStatement("val ret = %T()")
    properties.forEach { property ->
        propertyStatement("ret.${property.name} = ${property.name}\n", property)
    }
    endStatement("return ret")
}

public infix fun Property.`get initializer for DSL Builder or canonical Builder`(isDsl: Boolean): String =
    if (type is Property.PropertyType.PropertyCollection) when(type) {
        is Property.PropertyType.PropertyCollection.PropertySet -> "mutableSetOf()".takeIf { isDsl } ?: "emptySet()"
        is Property.PropertyType.PropertyCollection.PropertyList -> "mutableListOf()".takeIf { isDsl } ?: "emptyList()"
        is Property.PropertyType.PropertyCollection.PropertyMap  -> "mutableMapOf()".takeIf { isDsl } ?: "emptyMap()"
    } else "null"


//public fun


private fun `preserve imports for Android Studio, not used`(context: Context, options: Options, logger: ILogger) {}
