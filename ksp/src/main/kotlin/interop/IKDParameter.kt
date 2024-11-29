package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import ru.it_arch.clean_ddd.ksp.interop.IKDParameter.KDType.Collection.CollectionType
import ru.it_arch.ddd.IEntity
import ru.it_arch.ddd.ValueObject
import ru.it_arch.ddd.ValueObjectSingle

internal interface IKDParameter : IEntity {
    val name: Name
    val kdType: KDType

    override val id: ValueObject
        get() = name

    interface Name : ValueObjectSingle<String> {
        override fun validate() {}
    }

    sealed class KDType(val type: TypeName) {
        data class Element(val typeName: TypeName) : KDType(typeName)
        data class Collection(val typeName: ParameterizedTypeName, val collectionType: CollectionType) : KDType(typeName) {

            enum class CollectionType(val initializer: String) {
                SET("emptySet()"), LIST("emptyList()"), MAP("emptyMap()")
            }
        }

        companion object {
            fun create(typeName: TypeName) =
                if (typeName is ParameterizedTypeName) {
                    CollectionType.entries
                        .find { it.name.lowercase().replaceFirstChar { it.titlecaseChar() } == typeName.rawType.simpleName }
                        ?.let { Collection(typeName, it) }
                        ?: error("Not supported collection type $typeName")
                } else Element(typeName)
        }
    }
}
