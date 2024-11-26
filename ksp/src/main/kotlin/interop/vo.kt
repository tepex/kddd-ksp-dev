package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.toTypeName

internal fun KSClassDeclaration.toValueObjectType(logger: KSPLogger): KDValueObjectType? {
    superTypes.forEach { parent ->
        val fullName = parent.resolve().declaration.let { "${it.packageName.asString()}.${it.simpleName.asString()}" }
        when(fullName) {
            KDValueObjectType.KDValueObject.CLASSNAME -> KDValueObjectType.KDValueObject
            KDValueObjectType.KDValueObjectSingle.CLASSNAME -> {
                //logger.warn(">>> $this, typeName.class: ${typeName::class.simpleName} typeName: $typeName")
                runCatching { KDValueObjectType.KDValueObjectSingle.create(parent.toTypeName()) }.getOrElse {
                    logger.warn(it.message ?: "Cant parse parent type $parent")
                    null
                }
            }
            else -> null
        }?.also { return it }
    }
    return null
}

internal val KDValueObjectType?.isValueObject: Boolean
    get() = this == KDValueObjectType.KDValueObject
