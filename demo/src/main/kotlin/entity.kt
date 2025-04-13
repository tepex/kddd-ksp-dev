package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.demo.impl.myEntity

fun testEntity() {
    val entity = myEntity {
        id = 1
        content  = content {
            name = "some string value for demo"
            someField = 33
        }
    }

    println("Entity demo: $entity")

    // Change entity
    entity.toBuilder().apply {
        content = content.toBuilder().apply {  }.build()
    }
}
