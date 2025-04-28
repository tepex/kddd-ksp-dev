package ru.it_arch.clean_ddd.ksp_model.model

public fun kdClassName(block: (KDClassName.DslBuilder) -> Unit): KDClassName =
    KDClassName.DslBuilder().apply(block).build()

context(_: KDOptions)
public val String.toImplementationClassName: String
    get() {
        var result = generatedClassNameResult.boxed
        generatedClassNameRe.find(this)?.groupValues?.forEachIndexed { i, group ->
            group.takeIf { i > 0 }?.also { result = result.replace("\$$i", it) }
        }
        return result
    }

public val KDClassName.fullClassName: String by lazy {

    ""
}
