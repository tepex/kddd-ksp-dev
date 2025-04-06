package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.demo.impl.withInner

fun testWithInner() {
    withInner {
        myInner {

        }
    }
}
