package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.ksp.model.KDType
import ru.it_arch.clean_ddd.ksp.model.KDTypeContext

public const val FILE_HEADER_STUB: String = """
AUTO-GENERATED FILE. DO NOT MODIFY.
This file generated automatically by «KDDD» framework.
Author: Tepex <tepex@mail.ru>, Telegram: @Tepex
"""

context(KDTypeContext)
public fun KSClassDeclaration.kdTypeOrNull(): Result<KDType?> {
    superTypes.forEach { item -> item.kdTypeOrNull()?.also { return it } }
    // Not found
    return Result.success(null)
}

context(KDTypeContext)
private fun KSTypeReference.kdTypeOrNull(): Result<KDType>? = when(toString()) {
    KDType.Sealed::class.java.simpleName      -> Result.success(KDType.Sealed.create())
    KDType.Data::class.java.simpleName        -> Result.success(KDType.Data.create(false))
    KDType.IEntity::class.java.simpleName     -> Result.success(KDType.IEntity.create())
    KDType.Boxed::class.java.simpleName       -> runCatching { KDType.Boxed.create(toTypeName()) }
    else -> null
}
