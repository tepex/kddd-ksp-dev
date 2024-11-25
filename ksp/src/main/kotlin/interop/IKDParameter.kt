package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.TypeName

internal interface IKDParameter {
    val name: String
    val type: TypeName
}
