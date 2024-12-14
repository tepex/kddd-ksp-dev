@file:Suppress("SpellCheckingInspection")
package ru.it_arch.clean_ddd.ksp.interop

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.core.test.TestScope

object ProjectConfig : AbstractProjectConfig() {

    override var displayFullTestPath: Boolean? = true
}

/** Позитивный тест. Проверка того, что ожидается. */
suspend fun FunSpecContainerScope.pos(name: String, test: suspend TestScope.() -> Unit) {
    test("$name 🟢", test)
}

fun FunSpec.pos(name: String, test: suspend TestScope.() -> Unit) {
    test("$name 🟢", test)
}


/** Негативный тест. Проверка того, что не должно случиться. */
suspend fun FunSpecContainerScope.neg(name: String, test: suspend TestScope.() -> Unit) {
    test("$name ⛔️", test)
}

fun FunSpec.neg(name: String, test: suspend TestScope.() -> Unit) {
    test("$name ⛔️", test)
}

/** Тест технического характера. Идейного смысла не имеет. Для обеспечения полного покрытия. */
suspend fun FunSpecContainerScope.tech(name: String, test: suspend TestScope.() -> Unit) {
    test("$name 🛠", test)
}

fun FunSpec.tech(name: String, test: suspend TestScope.() -> Unit) {
    test("$name 🛠", test)
}
