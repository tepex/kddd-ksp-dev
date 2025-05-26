package ru.it_arch.clean_ddd.ksp

import ru.it_arch.kddd.utils.Utils

internal val utils: Utils by lazy {
    KdddProcessorProvider::class.java.classLoader.loadClass("ru.it_arch.kddd.utils.BuilderKt").getMethod("utils").invoke(null) as Utils
}
