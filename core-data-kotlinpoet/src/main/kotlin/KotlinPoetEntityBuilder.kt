package ru.it_arch.kddd.core.data

import com.squareup.kotlinpoet.TypeSpec
import ru.it_arch.kddd.core.data.model.TypeHolder
import ru.it_arch.kddd.domain.KotlinCodeEntityBuilder
import ru.it_arch.kddd.domain.model.type.EntityImpl

internal class KotlinPoetEntityBuilder(
    private val implClassBuilder: TypeSpec.Builder,
    private val typeHolder: TypeHolder,
    private val entity: EntityImpl
) : KotlinCodeEntityBuilder {

    override fun generateImplementationClass() {
        TODO("Not yet implemented")
    }
}
