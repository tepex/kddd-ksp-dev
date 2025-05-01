package ru.it_arch.clean_ddd.domain

public val Type.Boxed.boxed: Property
    get() = properties.first()

public fun Map<String, String>.toOptions(): Options = options {
    subpackage = get(Options.OPTION_IMPLEMENTATION_SUBPACKAGE)
    generatedClassNameRe = get(Options.OPTION_GENERATED_CLASS_NAME_RE)
    generatedClassNameResult = get(Options.OPTION_GENERATED_CLASS_NAME_RESULT)
    useContextParameters = get(Options.OPTION_CONTEXT_PARAMETERS)?.toBooleanStrictOrNull()
    jsonNamingStrategy = get(Options.OPTION_JSON_NAMING_STRATEGY)
}
