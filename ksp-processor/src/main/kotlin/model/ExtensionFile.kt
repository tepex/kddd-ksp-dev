package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.FileSpec
import ru.it_arch.clean_ddd.domain.CompositeClassName
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
internal data class ExtensionFile private constructor(
    val builder: FileSpec.Builder,
    val packageName: CompositeClassName.PackageName,
    val name: Name
) : ValueObject.Data {

    init {
        validate()
    }

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    @JvmInline
    value class Name private constructor(override val boxed: String) : ValueObject.Boxed<String> {

        init {
            validate()
        }

        override fun validate() {}

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T {
            TODO("Not yet implemented")
        }

        companion object {
            operator fun invoke(value: String): Name =
                Name(value)
        }
    }

    class Builder {
        var builder: FileSpec.Builder? = null
        var packageName: CompositeClassName.PackageName? = null
        var name: String? = null

        fun build(): ExtensionFile {
            checkNotNull(builder) { "Property 'Builder.builder' must be initialized!" }
            checkNotNull(packageName) { "Property 'Builder.packageName' must be initialized!" }
            checkNotNull(name) { "Property 'Builder.name' must be initialized!" }
            return ExtensionFile(builder!!, packageName!!, Name(name!!))
        }
    }
}
