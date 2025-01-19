package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

internal sealed interface JsonType {

    val typeName: TypeName

    fun asString(): String
    fun encodeXElement(): String

    data class Element private constructor(
        override val typeName: TypeName,
        val kdType: KDType
    ): JsonType {

        override fun asString(): String =
            if (typeName.isNullable) "String?" else "String"

        override fun encodeXElement(): String =
            "encode${if (kdType is KDType.Boxed) kdType.rawTypeName.toString().substringAfterLast('.') else "String"}Element"

        companion object {
            fun create(kdTypeSearchResult: KDTypeSearchResult, isNullable: Boolean): Element =
                Element(
                    (if (kdTypeSearchResult.first is KDType.Boxed) (kdTypeSearchResult.first as KDType.Boxed).rawTypeName
                    else kdTypeSearchResult.first.sourceTypeName).toNullable(isNullable),
                    kdTypeSearchResult.first
                )
        }
    }

    data class Collection(
        val parameterizedTypeName: ParameterizedTypeName
    ) : JsonType {

        private val type = parameterizedTypeName.toCollectionType()
        private val args = mutableListOf<JsonType>()
        private var _serializerTemplate = if (type == CollectionType.MAP) "%M(${TMPL_SIGN}0, ${TMPL_SIGN}1)" else "%M($TMPL_SIGN)"
        val serializerTemplate: String
            get() = _serializerTemplate
        // for addStatement("", *varargs)
        private val _serializerParameters = mutableListOf<Any>(
            MemberName("kotlinx.serialization.builtins", "${type.originName}Serializer")
        )
        val serializerParameters: List<Any>
            get() = _serializerParameters.toList()

        private val _toStringMappers = mutableListOf<String>()
        private lateinit var _toStringMapper: String
        val toStringMapper: String
            get() = _toStringMapper

        override val typeName: TypeName = parameterizedTypeName

        fun addArg(i: Int, node: JsonType) {
            // for descriptor
            args += node
            // for serializer
            _serializerTemplate = if (type == CollectionType.MAP) _serializerTemplate.replace("${TMPL_SIGN}$i", serializerReplacement(node))
            else _serializerTemplate.replace(TMPL_SIGN, serializerReplacement(node))
            val localIt = type.getItArgName(i)
            _toStringMappers += when(node) {
                is Element -> if (node.kdType is KDType.Boxed) node.kdType.asSerialize(localIt, node.typeName.isNullable) else localIt
                is Collection -> "$localIt${node.toStringMapper}"
            }
            // for deserializer
        }

        fun finish() {
            val isNoMap = args.all { it is Element && it.kdType !is KDType.Boxed }
            val isNoTerminal = if (isNoMap) false
            else if (type == CollectionType.MAP) true else type != CollectionType.SET
            _toStringMapper = type.mapperAsString(_toStringMappers.toList(), false, isNoMap, isNoTerminal)
        }

        private fun serializerReplacement(node: JsonType): String = when(node) {
            is Element -> {
                _serializerParameters += MemberName("kotlinx.serialization.builtins", "serializer")
                "String.%M().%M".takeIf { node.typeName.isNullable }
                    ?.also { _serializerParameters += MemberName("kotlinx.serialization.builtins", "nullable") }
                    ?: "String.%M()"
            }
            is Collection -> node.serializerTemplate.also { _serializerParameters.addAll(node.serializerParameters) }
        }

        override fun encodeXElement(): String =
            "encodeSerializableElement"

        override fun asString(): String = when(val type = parameterizedTypeName.toCollectionType()) {
            CollectionType.MAP -> "${type.originName}<${args[0].asString()}, ${args[1].asString()}>"
            else -> "${type.originName}<${args.first().asString()}>"
        }

        companion object {
            private const val TMPL_SIGN = "#"
            fun create(typeName: ParameterizedTypeName) = Collection(typeName)
        }
    }
}
