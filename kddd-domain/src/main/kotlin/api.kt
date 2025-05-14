package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.core.BoxedWithCommon
import ru.it_arch.clean_ddd.domain.core.Data
import ru.it_arch.clean_ddd.domain.core.IEntity
import ru.it_arch.clean_ddd.domain.core.KdddType

public fun Map<String, String>.toOptions(): Options = options {
    subpackage = get(Options.OPTION_IMPLEMENTATION_SUBPACKAGE)
    generatedClassNameRe = get(Options.OPTION_GENERATED_CLASS_NAME_RE)
    generatedClassNameResult = get(Options.OPTION_GENERATED_CLASS_NAME_RESULT)
    useContextParameters = get(Options.OPTION_CONTEXT_PARAMETERS)?.toBooleanStrictOrNull()
    jsonNamingStrategy = get(Options.OPTION_JSON_NAMING_STRATEGY)
}

/**
 * Преобразование строкового имени класса (вложенного) в цепочку [ClassName].
 *
 * Пример:
 * ```
 * "A.B.C": ClassName(name = "C").enclosing -> ClassName(name = "B").enclosing -> ClassName(name = "A").enclosing = null
 * ```
 * @see "src/test/kotlin/ClassNameTest.kt"
 * */
//context(_: PackageName)
/*
public fun String.toKddClassName(): KdddType.KdddClassName {
    val classNames = split('.')
    var parent: KdddType.KdddClassName? = null
    classNames.forEach { className ->
        parent = className {
            name = ClassName.Name.KdddType(className)
            enclosing = parent
        }
    }
    return parent!!
}*/

context(ctx: Context, _: Options)
public fun String.toKDddType(): KdddType = ctx.toGeneratable().let { generatable ->
    when(this) {
        Data::class.java.simpleName           -> Data(generatable, ctx.properties)
        IEntity::class.java.simpleName        -> IEntity(Data(generatable, ctx.properties))
        KdddType.Boxed::class.java.simpleName -> this toBoxedTypeWith generatable
        else                                  -> error("Unknown type: $this")
    }
}

public val CompositeClassName.fullClassName: CompositeClassName.FullClassName
    get() = CompositeClassName.FullClassName("$packageName.$className")

/**
 * 1. <CommonType>(src).let(::MyTypeImpl)
 * 2. <CommonType>.<deserialization static method>(src).let(::MyTypeImpl)
 * */
public val BoxedWithCommon.templateParseBody: String
    get() = "return %T${deserializationMethod.boxed.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""}(%N).let(::%T)"

public typealias SimpleStatement = (String) -> Unit
public typealias IndexedStatement = (String, Int) -> Unit

public fun Data.templateForkBody(simpleStatement: SimpleStatement, indexStatement: IndexedStatement) {
    simpleStatement("val ret = ${Data.BUILDER_CLASS_NAME}().apply {⇥\n")
    properties.forEachIndexed { i, _ ->
        indexStatement("%N = args[$i] as %T", i)
    }
    simpleStatement("⇤}.build() as T")
    simpleStatement("return ret")
}

public fun Data.templateBuilderBodyCheck(indexStatement: IndexedStatement) {
    properties.filter { it.isNullable.not() }.forEachIndexed { i, _ ->
        indexStatement("""checkNotNull(%N) { "Property '%T.%N' must be initialized!" }""", i)
    }
}

