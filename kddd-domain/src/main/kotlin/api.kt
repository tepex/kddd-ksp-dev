package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.internal.hasDsl
import ru.it_arch.clean_ddd.domain.internal.toGeneratable
import ru.it_arch.clean_ddd.domain.internal.toBoxedTypeWith
import ru.it_arch.clean_ddd.domain.model.CompositeClassName
import ru.it_arch.clean_ddd.domain.model.kddd.BoxedWithCommon
import ru.it_arch.clean_ddd.domain.model.kddd.Data
import ru.it_arch.clean_ddd.domain.model.kddd.IEntity
import ru.it_arch.clean_ddd.domain.model.Context
import ru.it_arch.clean_ddd.domain.model.ILogger
import ru.it_arch.clean_ddd.domain.model.Options
import ru.it_arch.clean_ddd.domain.model.Property
import ru.it_arch.clean_ddd.domain.model.kddd.KdddType

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

public fun Data.templateForkBody(simpleStatement: SimpleStatement, indexedStatement: IndexedStatement) {
    simpleStatement("val ret = ${Data.BUILDER_CLASS_NAME}().apply {⇥")
    properties.forEachIndexed { i, _ ->
        indexedStatement("%N = args[$i] as %T", i)
    }
    simpleStatement("⇤}.build() as T")
    simpleStatement("return ret")
}

public fun Data.templateBuilderBodyCheck(statement: PropertyStatement) {
    properties.filter { it.isNullable.not() && it.isCollectionType.not() }.forEach { property ->
        statement("""checkNotNull(%N) { "Property '%N.%N' must be initialized!" }""", property)
    }
}

public fun Data.templateBuilderFunBuildReturn(simpleStatement: SimpleStatement, propertyStatement: PropertyStatement) {
    simpleStatement("return %N(⇥")
    properties.forEachIndexed { i, property ->
        StringBuilder("%N = %N").apply {
            "!!".takeUnless { property.isNullable || property.isCollectionType }?.let(::append)
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

public fun Data.templateToDslBuilderBody(startStatement: SimpleStatement, endStatement: SimpleStatement, propertyStatement: PropertyStatement) {
    startStatement("val ret = %T()")
    properties.forEach { property ->
        propertyStatement("ret.${property.name} = ${property.name}\n", property)
    }
    endStatement("return ret")
}

public val Property.isCollectionType: Boolean
    get() = collectionType != null

public infix fun Property.`get initializer for DSL Builder or canonical Builder`(isDsl: Boolean): String =
    collectionType?.let { when(it) {
        Property.CollectionType.SET  -> "mutableSetOf()".takeIf { isDsl } ?: "emptySet()"
        Property.CollectionType.LIST -> "mutableListOf()".takeIf { isDsl } ?: "emptyList()"
        Property.CollectionType.MAP  -> "mutableMapOf()".takeIf { isDsl } ?: "emptyMap()"
    } } ?: "null"


//public fun


private fun `preserve imports for Android Studio, not used`(context: Context, logger: ILogger) {}
