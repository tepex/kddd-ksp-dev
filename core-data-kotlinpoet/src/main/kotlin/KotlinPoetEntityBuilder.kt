package ru.it_arch.clean_ddd.core.data

import com.squareup.kotlinpoet.TypeSpec
import ru.it_arch.clean_ddd.core.data.model.TypeHolder
import ru.it_arch.clean_ddd.domain.KotlinCodeEntityBuilder
import ru.it_arch.clean_ddd.domain.model.kddd.IEntity

internal class KotlinPoetEntityBuilder(
    private val implClassBuilder: TypeSpec.Builder,
    private val typeHolder: TypeHolder,
    private val entity: IEntity
) : KotlinCodeEntityBuilder {

    override fun generateImplementationClass() {
        TODO("Not yet implemented")
    }
}
