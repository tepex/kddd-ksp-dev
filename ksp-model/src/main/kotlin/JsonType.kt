package ru.it_arch.clean_ddd.ksp_model

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import ru.it_arch.clean_ddd.ksp_model.model.CollectionType
import ru.it_arch.clean_ddd.ksp_model.model.KDType

internal sealed interface JsonType {

    val name: TypeName

    val asString: String
    val encodePrimitiveElementFunName: String
    val decodePrimitiveElementFunName: String

    @ConsistentCopyVisibility
    data class Element private constructor(
        override val name: TypeName,
        val kdType: KDType,
        // TODO: remove
        val isInner: Boolean
    ): JsonType {

        override val asString: String =
            if (kdType is KDType.Boxed && kdType.isPrimitive) kdType.asSimplePrimitive()
            else "String?".takeIf { name.isNullable } ?: "String"

        override val encodePrimitiveElementFunName: String =
            "encode${if (kdType is KDType.Boxed && kdType.isPrimitive) kdType.asSimplePrimitive() else "String"}Element"

        override val decodePrimitiveElementFunName: String =
            "decode${if (kdType is KDType.Boxed && kdType.isPrimitive) kdType.asSimplePrimitive() else "String"}Element"

        companion object {
            operator fun invoke(kdTypeSearchResult: KDTypeSearchResult, isNullable: Boolean): Element =
                Element(
                    (if (kdTypeSearchResult.first is KDType.Boxed) (kdTypeSearchResult.first as KDType.Boxed).rawType
                    else kdTypeSearchResult.first.name).toNullable(isNullable),
                    kdTypeSearchResult.first,
                    kdTypeSearchResult.second
                )
        }
    }

    @ConsistentCopyVisibility
    data class Collection private constructor(
        val parameterizedTypeName: ParameterizedTypeName
    ) : JsonType {

        private val type = parameterizedTypeName.toCollectionType()
        private val args = mutableListOf<JsonType>()
        var serializerTemplate = if (type == CollectionType.MAP) "%M(${TMPL_SIGN}0, ${TMPL_SIGN}1)" else "%M($TMPL_SIGN)"
            private set
        // for addStatement("", *varargs)
        private val _serializerVarargs = mutableListOf<Any>(
            MemberName("kotlinx.serialization.builtins", "${type.originName}Serializer")
        )
        val serializerVarargs: List<Any>
            get() = _serializerVarargs.toList()

        private val serializationMappers = mutableListOf<String>()
        lateinit var serializationMapper: String
            private set

        private val deserializationMappers = mutableListOf<String>()
        lateinit var deserializationMapper: String
            private set

        override val name: TypeName = parameterizedTypeName

        fun addArg(i: Int, node: JsonType) {
            // for descriptor
            args += node
            // for serializer
            serializerTemplate = if (type == CollectionType.MAP) serializerTemplate.replace("${TMPL_SIGN}$i", serializerReplacement(node))
            else serializerTemplate.replace(TMPL_SIGN, serializerReplacement(node))
            val localIt = type getItArgNameForIndex i
            // .map { it.map { it.boxed }.toSet() })
            serializationMappers += when(node) {
                is Element -> if (node.kdType is KDType.Boxed) node.kdType.asSerialize(localIt, node.name.isNullable) else localIt
                is Collection -> "$localIt${node.serializationMapper}"
            }
            // .map { it.map { it.let(NameImpl::create) }.toSet() }
            // for deserializer
            deserializationMappers += when(node) {
                is Element ->
                    if (node.kdType is KDType.Boxed) "${localIt}${"?".takeIf { node.name.isNullable } ?: ""}${node.kdType.asDeserialize(node.isInner)}" else "XXXImpl.serializer()"
                is Collection -> "$localIt${node.deserializationMapper}"
            }
        }

        fun finish() {
            val hasNotContainsBoxed = args.all { it is Element && it.kdType !is KDType.Boxed }
            serializationMapper = serializationMappers.toList().createMapper(type, false, hasNotContainsBoxed)
            deserializationMapper = deserializationMappers.toList().createMapper(type, false, hasNotContainsBoxed)
        }

        private fun serializerReplacement(node: JsonType): String = when(node) {
            is Element -> {
                val primitiveType = if (node.kdType is KDType.Boxed && node.kdType.isPrimitive) node.kdType.asSimplePrimitive() else "String"
                _serializerVarargs += MemberName("kotlinx.serialization.builtins", "serializer")
                "$primitiveType.%M().%M".takeIf { node.name.isNullable }
                    ?.also { _serializerVarargs += MemberName("kotlinx.serialization.builtins", "nullable") }
                    ?: "$primitiveType.%M()"
            }
            is Collection -> node.serializerTemplate.also { _serializerVarargs.addAll(node.serializerVarargs) }
        }

        override val encodePrimitiveElementFunName: String = "encodeSerializableElement"

        override val decodePrimitiveElementFunName: String = "decodeSerializableElement"

        override val asString: String by lazy {
            when(val type = parameterizedTypeName.toCollectionType()) {
                CollectionType.MAP -> "${type.originName}<${args[0].asString}, ${args[1].asString}>"
                else               -> "${type.originName}<${args.first().asString}>"
            }
        }

        companion object {
            private const val TMPL_SIGN = "#"

            operator fun invoke(typeName: ParameterizedTypeName) = Collection(typeName)
        }
    }
}
