package ru.it_arch.kddd.domain

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.core.test.TestScope

object ProjectConfig : AbstractProjectConfig() {
    override var displayFullTestPath: Boolean? = true
}

/** Позитивный тест. Проверка того, что ожидается. Версия для корутин. */
suspend fun FunSpecContainerScope.pos(name: String, test: suspend TestScope.() -> Unit) {
    test("🟢 $name", test)
}

/** Позитивный тест. Проверка того, что ожидается. */
fun FunSpec.pos(name: String, test: suspend TestScope.() -> Unit) {
    test("🟢 $name", test)
}

/** Негативный тест. Проверка того, что не должно случиться. Версия для корутин. */
suspend fun FunSpecContainerScope.neg(name: String, test: suspend TestScope.() -> Unit) {
    test("⛔️ $name", test)
}

/** Негативный тест. Проверка того, что не должно случиться. */
fun FunSpec.neg(name: String, test: suspend TestScope.() -> Unit) {
    test("⛔️ $name", test)
}

/** Тест технического характера. Идейного смысла не имеет. Для обеспечения полного покрытия. Версия для корутин. */
suspend fun FunSpecContainerScope.tech(name: String, test: suspend TestScope.() -> Unit) {
    test("🛠 $name", test)
}

/** Тест технического характера. Идейного смысла не имеет. Для обеспечения полного покрытия. */
fun FunSpec.tech(name: String, test: suspend TestScope.() -> Unit) {
    test("🛠 $name", test)
}
