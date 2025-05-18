package ru.it_arch.clean_ddd.domain.model

import ru.it_arch.kddd.Kddd

@ConsistentCopyVisibility
public data class IEntity private constructor(
    private val data: Data
) : KdddType.ModelContainer by data {

    private val id = data.properties.find { it.name.boxed == ID_NAME }
        ?: error("ID parameter not found for Entity ${kddd.className}")

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    override fun validate() {}

    public companion object {
        public const val ID_NAME: String = "id"

        public operator fun invoke(data: Data): IEntity =
            IEntity(data)
    }
}

