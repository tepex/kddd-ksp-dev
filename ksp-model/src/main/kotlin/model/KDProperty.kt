package ru.it_arch.clean_ddd.ksp_model.model

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import ru.it_arch.kddd.KDSerialName
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

/**
 * Определяет свойство в классе имплементации.
 *
 * @property name имя сериализированного свойства если применена аннотация [KDSerialName].
 * @property memberName имя свойства.
 * @property type тип свойства [TypeName].
 * */
@ConsistentCopyVisibility
public data class KDProperty private constructor(
    val name: Name,
    val memberName: MemberName,
    val type: TypeName,
) : ValueObject.Data {

    init {
        validate()
    }

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T =
        TODO("Not yet implemented")

    @JvmInline
    public value class Name(override val boxed: String): ValueObject.Boxed<String> {
        init {
            validate()
        }

        override fun validate() {}

        override fun toString(): String =
            boxed

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T =
            Name(boxed) as T
    }

    public companion object {
        public operator fun invoke(memberName: MemberName, type: TypeName, annotation: KDSerialName? = null): KDProperty =
            KDProperty(Name(annotation?.value ?: memberName.simpleName), memberName, type)
    }
}
