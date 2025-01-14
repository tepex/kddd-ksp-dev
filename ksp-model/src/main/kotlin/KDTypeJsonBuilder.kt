package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.it_arch.clean_ddd.ksp.model.DSLType.Collection

public class KDTypeJsonBuilder private constructor(
    private val holder: KDType.Model,
    private val logger: KDLogger
) {

    private val companionBuilder = TypeSpec.companionObjectBuilder().addSuperinterface(KSerializer::class.asTypeName().parameterizedBy(holder.className))

    private val descriptorFun = DescriptorFun(holder.className)

    private val funSerialise = FunSpec.builder("serialize")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("encoder", Encoder::class)
        .addParameter("value", holder.className)

    private val funDeserialize = FunSpec.builder("deserialize")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("decoder", Decoder::class)
        .returns(holder.className)

    //private val sb

    init {
        holder.propertyHolders.forEachIndexed { index, property ->
            if (property.typeName is ParameterizedTypeName) {
                val ct = Collection.create(property.typeName, logger)
                processCollection(property.name, property.typeName, false)



            } else {
                // direct to statement +Chunk

                // It works for scalar types

                val kdType = holder.getKDType(property.typeName).first
                if (kdType is KDType.Boxed) {
                    logger.log("${property.serialName}: ${kdType.boxedType} ${kdType.isPrimitive} parse: ${kdType.isParsable}")
                } else if (kdType is KDType.Generatable) logger.log(">>> ${property.serialName}: ${kdType.className}")

                if (kdType is KDType.Generatable) {
                    descriptorFun.addElement(property.serialName, kdType, property.typeName.isNullable)
                } else {
                    TODO()
                }
                // funSerialize.add
                // funDeserialize.add
            }
        }

        companionBuilder.addProperty(descriptorFun.build())

        companionBuilder.addFunction(funSerialise.build())

        companionBuilder.addFunction(funDeserialize.build())
    }

    /**
     * 1. IN: ParametrizedTypeName -> KDType.Parametrized
     * 2. For `descriptorFun`: simple names: List<Set<String>> with null
     * 3. For `funSerialize`: List/Set/Map Serializer + mapper type -> String :
     *     encodeSerializableElement(descriptor, 9, ListSerializer(SetSerializer(String.serializer())),
     *                     value.nestedList1.map { it.map { it.boxed }.toSet() })
     * 4. For `funDeserialize`:
     * */
    private tailrec fun processCollection(name: MemberName, parameterized: ParameterizedTypeName, isFinish: Boolean) {
        if (isFinish) {
            // закрываем скобочки?
            return
        }
        else {
            val collectionType = parameterized.toCollectionType()
            val sb = StringBuilder("$collectionType<")
            //logger.log("${name.simpleName}: $collectionType<")
            parameterized.typeArguments.forEachIndexed { index, typeName ->
                sb.append("$typeName, ")
            }
            sb.append('>')
            logger.log("${name.simpleName}: $sb")
            return processCollection(name, parameterized, true)
        }
    }

    public fun build(): TypeSpec {
        return companionBuilder.build()
    }

    private class DescriptorFun(
        className: ClassName
    ) {
        val descriptorProperty = PropertySpec.builder("descriptor", SerialDescriptor::class).addModifiers(KModifier.OVERRIDE)
        val sb = StringBuilder("%M(%T::class.java.name) {«⇥\n")
        private val args = mutableListOf(MemberName("kotlinx.serialization.descriptors", "buildClassSerialDescriptor"), className)

        fun addElement(name: String, kdType: KDType.Generatable, isNullable: Boolean) {
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
            } else kdType.className
            args += param
            /*
            if (isNullable) sb.append("%M<String?>(\"$name\", isOptional = true)\n")
            else sb.append("%M<String>(\"$name\")\n")

            args += MemberName("kotlinx.serialization.descriptors", "element")*/
        }

        fun build() =
            descriptorProperty
                .initializer("$sb»}", *args.toTypedArray())
                .build()
    }

    public companion object {
        context(KDOptions)
        public fun create(holder: KDType.Model, logger: KDLogger): KDTypeJsonBuilder =
            KDTypeJsonBuilder(holder, logger)
    }
}
