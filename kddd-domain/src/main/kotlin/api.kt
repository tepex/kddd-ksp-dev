package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.model.BoxedWithCommon
import ru.it_arch.clean_ddd.domain.model.Data
import ru.it_arch.clean_ddd.domain.model.IEntity
import ru.it_arch.clean_ddd.domain.model.KdddType

public fun Map<String, String>.toOptions(): Options = options {
    subpackage = get(Options.OPTION_IMPLEMENTATION_SUBPACKAGE)
    generatedClassNameRe = get(Options.OPTION_GENERATED_CLASS_NAME_RE)
    generatedClassNameResult = get(Options.OPTION_GENERATED_CLASS_NAME_RESULT)
    useContextParameters = get(Options.OPTION_CONTEXT_PARAMETERS)?.toBooleanStrictOrNull()
    jsonNamingStrategy = get(Options.OPTION_JSON_NAMING_STRATEGY)
}

context(ctx: Context, _: Options, logger: ILogger)
public fun String.toKDddType(): KdddType = ctx.toGeneratable().let { generatable ->
    when(substringBefore('<')) {
        Data::class.java.simpleName           -> Data(generatable, ctx.properties, ctx.hasDsl)
        IEntity::class.java.simpleName        -> IEntity(Data(generatable, ctx.properties, ctx.hasDsl))
        KdddType.Boxed::class.java.simpleName -> this toBoxedTypeWith generatable
        else                                  -> error("Unknown type: $this")
    }
}

public val CompositeClassName.fullClassName: String
    get() = "$packageName.$className"

public val CompositeClassName.ClassName.shortName: String
    get() = boxed.substringAfterLast('.')

/**
 * 1. <CommonType>(src).let(::MyTypeImpl)
 * 2. <CommonType>.<deserialization static method>(src).let(::MyTypeImpl)
 * */
public val BoxedWithCommon.templateParseBody: String
    get() = "return %T${deserializationMethod.boxed.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""}(%N).let(::%N)"

public typealias SimpleStatement = (String) -> Unit
public typealias IndexedStatement = (String, Int) -> Unit

public fun Data.templateForkBody(simpleStatement: SimpleStatement, indexStatement: IndexedStatement) {
    simpleStatement("val ret = ${Data.BUILDER_CLASS_NAME}().apply {⇥")
    properties.forEachIndexed { i, _ ->
        indexStatement("%N = args[$i] as %T", i)
    }
    simpleStatement("⇤}.build() as T")
    simpleStatement("return ret")
}

public fun Data.templateBuilderBodyCheck(indexStatement: IndexedStatement) {
    properties.filter { it.isNullable.not() }.forEachIndexed { i, _ ->
        indexStatement("""checkNotNull(%N) { "Property '%N.%N' must be initialized!" }""", i)
    }
}

public fun Data.templateBuilderFunBuildReturn(simpleStatement: SimpleStatement, indexStatement: IndexedStatement) {
    simpleStatement("return %N(⇥")
    properties.forEachIndexed { i, property ->
        StringBuilder("%N = %N").apply {
            "!!".takeUnless { property.isNullable || property.isCollectionType }?.let(::append)
            ", ".takeIf { i < (properties.size - 1) }?.let(::append)
        }.also { indexStatement(it.toString(), i) }
    }
    simpleStatement("⇤)")
}

public fun Data.templateToBuilderBody(statement: SimpleStatement) {
    StringBuilder("return %T().apply {\n⇥").apply {
        properties.forEach { property ->
            append("this.${property.name} = ${property.name}\n")
        }
        append("⇤}")
    }.toString().also { statement(it) }
}

public val Property.isCollectionType: Boolean
    get() = collectionType != null

public infix fun Property.`get initializer for DSL Builder or canonical Builder`(isDsl: Boolean): String =
    collectionType?.let { when(it) {
        Property.CollectionType.SET  -> "mutableSetOf()".takeIf { isDsl } ?: "emptySet()"
        Property.CollectionType.LIST -> "mutableListOf()".takeIf { isDsl } ?: "emptyList()"
        Property.CollectionType.MAP  -> "mutableMapOf()".takeIf { isDsl } ?: "emptyMap()"
    } } ?: "null"
