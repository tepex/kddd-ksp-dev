package ru.it_arch.clean_ddd.ksp

import com.squareup.kotlinpoet.TypeSpec
import ru.it_arch.clean_ddd.domain.KotlinCodeEntityBuilder
import ru.it_arch.clean_ddd.domain.model.kddd.Data
import ru.it_arch.clean_ddd.domain.model.kddd.IEntity
import ru.it_arch.clean_ddd.ksp.model.TypeHolder

internal class KotlinPoetEntityBuilder(
    private val implClassBuilder: TypeSpec.Builder,
    private val typeHolder: TypeHolder,
    private val entity: IEntity
) : KotlinCodeEntityBuilder {

    override fun generateEntityImplementationClass(entity: IEntity) {
        TODO("Not yet implemented")
    }
}
