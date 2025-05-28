package ru.it_arch.kddd.domain.model.type

import ru.it_arch.kddd.Kddd

@ConsistentCopyVisibility
public data class EntityImpl private constructor(
    private val data: DataClassImpl
) : KdddType.DataClass by data {

    private val id = data.properties.find { it.name.boxed == ID_NAME }
        ?: error("ID parameter not found for Entity ${kddd.className}")

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    override fun validate() {}

    public companion object {
        public const val ID_NAME: String = "id"

        public operator fun invoke(data: DataClassImpl): EntityImpl =
            EntityImpl(data)
    }
}

