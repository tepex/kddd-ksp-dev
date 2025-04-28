package ru.it_arch.clean_ddd.ksp_model

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.it_arch.clean_ddd.ksp_model.model.KDProperty
import ru.it_arch.clean_ddd.ksp_model.model.KDType
import ru.it_arch.clean_ddd.ksp_model.utils.KDLogger
import ru.it_arch.kddd.OptIn

/**
 *
 * @property logger внутренний логгер.
 * @property model [KDType.Model], для которой создается билдер.
 * @property companionBuilder [TypeSpec.Builder] билдер для `companion object : KSerializer<MyTypeImpl>`.
 * @property descriptorPropertyHolder holder для свойства `descriptor`.
 * @property serializeFunHolder
 * */
public class JsonBuilderHolder private constructor(
    private val logger: KDLogger,
    private val model: KDType.Model
) {

    private val companionBuilder = TypeSpec.companionObjectBuilder().addSuperinterface(KSerializer::class.asTypeName()
        .parameterizedBy(model.impl.className))

    private val descriptorPropertyHolder = DescriptorPropertyHolder(model.impl.className)
    private val serializeFunHolder = SerializeFunHolder(model.impl.className)
    private val deserializeFun = DeserializeFun(model.impl.className)

    init {
        model.properties.forEachIndexed { index, property -> when (property.type.isCollection()) {
            true ->
                processCollection(property.member, JsonType.Collection(property.type.toParametrizedType()), false)
                    .also { jsonType ->
                        descriptorPropertyHolder.addCollection(property.serialName, jsonType)
                        serializeFunHolder.addElement(property.serialName, jsonType)
                        deserializeFun.addElement(property.serialName, jsonType)
                    }
            false -> {
                val result = model.getKDType(property.type)
                if (result.first is KDType.Generatable) {
                    descriptorPropertyHolder
                        .addElement(property.serialName, result.first as KDType.Generatable, property.type.isNullable)
                    serializeFunHolder
                        .addElement(property.serialName, JsonType.Element(result, property.type.isNullable))
                    deserializeFun
                        .addElement(property.serialName, JsonType.Element(result, property.type.isNullable))
                } else {
                    TODO()
                }
            }
        } }

        val descriptor = descriptorPropertyHolder.build()
        companionBuilder.addProperty(descriptor)
        companionBuilder.addFunction(serializeFunHolder.build(descriptor))
        companionBuilder.addFunction(deserializeFun.build(descriptor))

        /* @OptIn(ExperimentalSerializationApi::class) */
        companionBuilder.addKdoc("https://stackoverflow.com/questions/65272262/custom-serializer-for-data-class-without-serializable")
        // Это, увы, не работает. Придется сделать грязный хак.
        //AnnotationSpec.builder(OptIn::class)
        AnnotationSpec.builder(OptIn::class).apply { // Это мой OptIn, а не kotlin.OptIn
            addMember("ExperimentalSerializationApi::class")
        }.build().also(companionBuilder::addAnnotation)
    }

    /**
     * 1. IN: ParametrizedTypeName ->
     * 2. For `descriptorFun`: simple names: List<Set<String>> with null
     * 3. For `funSerialize`: List/Set/Map Serializer + mapper type -> String :
     *     encodeSerializableElement(descriptor, 9, ListSerializer(SetSerializer(String.serializer())),
     *                     value.nestedList1.map { it.map { it.boxed }.toSet() })
     * 4. For `funDeserialize`:
     * */
    // Создаем свою тяжелую иерархию для разных кейсов
    private tailrec fun processCollection(name: MemberName, collection: JsonType.Collection, isFinish: Boolean): JsonType {
        return if (isFinish) {
            // закрываем скобочки?
            collection
        }
        else {
            collection.parameterizedTypeName.typeArguments.forEachIndexed { index, arg ->
                @Suppress("NON_TAIL_RECURSIVE_CALL")
                when (arg.isCollection()) {
                    true  ->  processCollection(name, JsonType.Collection(arg.toParametrizedType()), false)
                    false -> model.getKDType(arg).let { JsonType.Element(it, arg.isNullable) }
                }.also { collection.addArg(index, it) }
            }
            collection.finish()
            processCollection(name, collection, true)
        }
    }

    public fun build(): TypeSpec {
        return companionBuilder.build()
    }

    /**
     * Генерация свойства [KSerializer.descriptor].
     * ```
     * override val descriptor: SerialDescriptor =
     *     buildClassSerialDescriptor(<MyTypeImpl>::class.java.name) { ... }
     * ```
     *
     * @param className имя класса для сериализации/десериализации.
     * */
    private class DescriptorPropertyHolder(className: ClassName) {
        val descriptorProperty = PropertySpec.builder("descriptor", SerialDescriptor::class).addModifiers(KModifier.OVERRIDE)
        val sb = StringBuilder("%M(%T::class.java.name) {«⇥\n")
        private val args = mutableListOf(MemberName("kotlinx.serialization.descriptors", "buildClassSerialDescriptor"), className)

        fun addElement(name: KDProperty.Name, kdType: KDType.Generatable, isNullable: Boolean) {
            // element<String?>("uuid", isOptional = true)
            // element<InnerImpl>("inner")
            sb.append("%M<%T")
            if (isNullable) sb.append('?')
            sb.append(">(\"$name\"")
            if (isNullable) sb.append(", isOptional = true")
            sb.append(")\n")
            args += MemberName("kotlinx.serialization.descriptors", "element")
            val param = if (kdType is KDType.Boxed) {
                if (kdType.isPrimitive) kdType.boxedType
                else if (kdType.isParsable) STRING
                else error("Property `$name` type must have parameterized type primitive or parsable")
            } else kdType.impl
            args += param
        }

        fun addCollection(name: KDProperty.Name, jsonType: JsonType) {
            sb.append("%M<${jsonType.asString}>")
            sb.append("(\"$name\")\n")
            args += MemberName("kotlinx.serialization.descriptors", "element")
        }

        fun build() =
            descriptorProperty
                .initializer("$sb»}", *args.toTypedArray())
                .build()
    }

    /**
     * Генерация метода [KSerializer.serialize].
     *
     * ```
     * override fun serialize(encoder: Encoder, value: MyTypeImpl) { ... }
     * ```
     * @property className имя сериализуемого класса.
     * */
    private class SerializeFunHolder(private val className: ClassName) {
        private val funSerialise = FunSpec.builder("serialize")
            .addModifiers(KModifier.OVERRIDE)

        private val elements = mutableListOf<Pair<KDProperty.Name, JsonType>>()

        fun addElement(propertyName: KDProperty.Name, type: JsonType) {
            elements += propertyName to type
        }

        fun build(descriptor: PropertySpec): FunSpec {
            val encoderParam = ParameterSpec.builder("encoder", Encoder::class).build()
            val valueParam = ParameterSpec.builder("value", className).build()
            val encoderStructure = MemberName("kotlinx.serialization.encoding", "encodeStructure")
            return funSerialise.addParameter(encoderParam).apply {
                addParameter(valueParam)
                // ```encoder.encodeStructure(descriptor) {```
                addStatement("%N.%M(%N) {⇥", encoderParam, encoderStructure, descriptor)
                elements.forEachIndexed { i, (name, jsonType) -> when(jsonType) {
                    is JsonType.Element -> {
                        // Формирование параметров метода ```addStatement(tmpl, *args)``` - строкового шаблона и списка аргументов для него
                        val args = mutableListOf<Any>(descriptor)
                        if (jsonType.name.isNullable) {
                            args += MemberName("kotlinx.serialization.builtins", "serializer")
                            args += valueParam
                            // ```encodeNullableSerializableElement(descriptor, <index>, ```
                            "encodeNullableSerializableElement(%N, $i, " + when(jsonType.kdType) {
                                is KDType.Boxed -> if (jsonType.kdType.isPrimitive)
                                    // ```<Primitive>.serializer(), value.<param name>?.boxed[.<serialization fun>()])```
                                    "${jsonType.kdType.asSimplePrimitive()}.%M(), %N.${jsonType.kdType.asSerialize(name, true)})"
                                    else
                                        // Nullable Common type
                                        // ```String.serializer(), value.<param name>?.boxed?.<serialization fun>())```
                                        "String.%M(), %N.${jsonType.kdType.asSerialize(name, true)})"
                                is KDType.Generatable ->
                                    // Nullable Inner type
                                    // ```<ClassName>.serializer(), `value`.<param name> as <ClassName>?)```
                                    "${jsonType.kdType.impl.simpleName}.%M(), %N.$name as %T?)"
                                        .also { args += jsonType.kdType.impl }
                                else -> TODO("Unsupported type: ${jsonType.kdType}")
                            }
                        } else { // Non nullable
                            when(jsonType.kdType) {
                                is KDType.Boxed -> {
                                    args += valueParam
                                    if (jsonType.kdType.isPrimitive)
                                        // ```encode<Primitive>Element(descriptor, <index>, value.<param name>.boxed)```
                                        "${jsonType.encodePrimitiveElementFunName}(%N, $i, %N.${jsonType.kdType.asSerialize(name,false)})"
                                    else
                                        // Non nullable common type
                                        // ```encodeStringElement(descriptor, <index>, value.<param name>.boxed.<serialization fun>())```
                                        "${jsonType.encodePrimitiveElementFunName}(%N, $i, %N.${jsonType.kdType.asSerialize(name,false)})"
                                }
                                is KDType.Generatable ->
                                    // Non nullable Inner type
                                    // ```encodeSerializableElement(descriptor, <index>, <ClassName>.serializer(), value.<param name> as <ClassName>)```
                                    "encodeSerializableElement(%N, $i, ${jsonType.kdType.impl.simpleName}.%M(), %N.$name as %T)"
                                        .also {
                                            args += MemberName("kotlinx.serialization.builtins", "serializer")
                                            args += valueParam
                                            args += jsonType.kdType.impl
                                        }
                                else -> TODO("Unsupported type: ${jsonType.kdType}")
                            }
                        }.also { addStatement(it, *args.toTypedArray()) }
                    }
                    is JsonType.Collection ->
                        addStatement("encodeSerializableElement(%N, $i, ${jsonType.serializerTemplate}, %N.$name${jsonType.serializationMapper})",
                            *(listOf<Any>(descriptor) + jsonType.serializerVarargs + valueParam).toTypedArray()
                        )
                } }
                addStatement("}")
            }.build()
        }
    }

    /**
     * Генерация метода [KSerializer.deserialize].
     *
     * ```
     * override fun deserialize(decoder: Decoder): MyTypeImpl { ... }
     * ```
     *
     * @property className имя десериализуемого класса.
     * */
    private class DeserializeFun(private val className: ClassName) {

        private val elements = mutableListOf<Pair<KDProperty.Name, JsonType>>()

        private val funDeserialize = FunSpec.builder("deserialize")
            .addModifiers(KModifier.OVERRIDE)
            .returns(className)

        fun addElement(propertyName: KDProperty.Name, type: JsonType) {
            elements += propertyName to type
        }

        fun build(descriptor: PropertySpec): FunSpec {
            val decoderParam = ParameterSpec.builder("decoder", Decoder::class).build()
            val decoderStructure = MemberName("kotlinx.serialization.encoding", "decodeStructure")
            return funDeserialize.addParameter(decoderParam).apply {
                addStatement("val ret = %N.%M(%N) {⇥", decoderParam, decoderStructure, descriptor)
                addStatement("val builder = Builder()")
                addStatement("loop@ while (true) {⇥")
                addStatement("when (val i = decodeElementIndex(%N)) {⇥", descriptor)
                elements.forEachIndexed { i, (name, jsonType) ->
                    val prefix = "$i -> builder.$name ="
                    when(jsonType) {
                        is JsonType.Element -> {
                            val args = mutableListOf<Any>(descriptor)

                            if (jsonType.name.isNullable) {
                                args += MemberName("kotlinx.serialization.builtins", "serializer")
                                "$prefix decodeNullableSerializableElement(%N, $i, " + when(jsonType.kdType) {
                                    is KDType.Boxed ->
                                        if (jsonType.kdType.isPrimitive)
                                            // ```<Primitive>.serializer())?.let...```
                                            "${jsonType.kdType.asSimplePrimitive()}.%M())?${jsonType.kdType.deserializationCode}"
                                                .takeUnless { jsonType.kdType.isString }
                                                // ```String.serializer().nullable)?.let...```
                                                ?: "${jsonType.kdType.asSimplePrimitive()}.%M().%M)?${jsonType.kdType.deserializationCode}"
                                                    .also { args += MemberName("kotlinx.serialization.builtins", "nullable") }
                                        else
                                            // Nullable Common type
                                            // ```String.serializer().nullable)?.let...```
                                            "String.%M().%M)?${jsonType.kdType.deserializationCode}"
                                                .also { args += MemberName("kotlinx.serialization.builtins", "nullable") }
                                    is KDType.Generatable ->
                                        // Nullable Inner type
                                        // ```<ClassName>.serializer().nullable)```
                                        "${jsonType.kdType.impl.simpleName}.%M().%M)"
                                            .also { args += MemberName("kotlinx.serialization.builtins", "nullable") }
                                    else -> TODO("Unsupported type: ${jsonType.kdType}")
                                }
                            } else { // Non nullable
                                when(jsonType.kdType) {
                                    is KDType.Boxed ->
                                        // ```decode<Primitive>Element(descriptor, <index>).let...```
                                        "$prefix ${jsonType.decodePrimitiveElementFunName}(%N, $i)${jsonType.kdType.deserializationCode}"
                                            .takeIf { jsonType.kdType.isPrimitive } ?:
                                            // Non nullable Common type
                                            // ```decodeStringElement(descriptor, <index>).let...```
                                            "$prefix decodeStringElement(%N, $i)${jsonType.kdType.deserializationCode}"
                                    is KDType.Generatable ->
                                        // Non nullable inner type
                                        // ```decodeSerializableElement(descriptor, <index>, <ClassName>.serializer())```
                                        "$prefix decodeSerializableElement(%N, $i, ${jsonType.kdType.impl.simpleName}.%M())"
                                            .also { args += MemberName("kotlinx.serialization.builtins", "serializer") }
                                    else -> TODO("Unsupported type: ${jsonType.kdType}")
                                }
                            }.also { addStatement(it, *args.toTypedArray()) }
                        }
                        is JsonType.Collection -> {
                            /*
                            addStatement("encodeSerializableElement(%N, $i, ${jsonType.serializerTemplate}, %N.${el.first}${jsonType.serializationMapper})",
                                *(listOf<Any>(descriptor) + jsonType.serializerVarargs + valueParam).toTypedArray()
                            )*/
                            addStatement("$prefix decodeSerializableElement(%N, $i, ${jsonType.serializerTemplate})${jsonType.deserializationMapper}",
                                *(listOf<Any>(descriptor) + jsonType.serializerVarargs).toTypedArray()
                                )

                        }
                    }
                }
                addStatement("%M -> break@loop", MemberName("kotlinx.serialization.encoding.CompositeDecoder.Companion", "DECODE_DONE"))
                addStatement("else -> throw %T(\"Unexpected index \$i\")", SerializationException::class.asTypeName())

                addStatement("⇤}")
                addStatement("⇤}")
                addStatement("builder.build()")
                addStatement("⇤}\n⇤⇤return ret as %T", className)
            }.build()
        }
    }

    public companion object {
        context(logger: KDLogger)
        public operator fun invoke(holder: KDType.Model): JsonBuilderHolder =
            JsonBuilderHolder(logger, holder)
    }
}
