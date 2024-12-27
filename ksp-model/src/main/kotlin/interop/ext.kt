package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.ksp.model.KDType
import ru.it_arch.clean_ddd.ksp.model.KDTypeHelper

public const val FILE_HEADER_STUB: String = """
AUTO-GENERATED FILE. DO NOT MODIFY.
This file generated automatically by «KDDD» framework.
Author: Tepex <tepex@mail.ru>, Telegram: @Tepex
"""

public fun KSClassDeclaration.kdTypeOrNull(helper: KDTypeHelper): Result<KDType?> {
    superTypes.forEach { it.kdTypeOrNull(helper)?.also { return it } }
    // Not found
    return Result.success(null)
}

private fun KSTypeReference.kdTypeOrNull(helper: KDTypeHelper): Result<KDType>? = when(toString()) {
    KDType.Sealed::class.java.simpleName      -> Result.success(KDType.Sealed.create(helper))
    KDType.Data::class.java.simpleName        -> Result.success(KDType.Data.create(helper, false))
    KDType.IEntity::class.java.simpleName     -> Result.success(KDType.IEntity.create(helper))
    KDType.Boxed::class.java.simpleName       -> runCatching { KDType.Boxed.create(helper, toTypeName()) }
    else -> null
}
