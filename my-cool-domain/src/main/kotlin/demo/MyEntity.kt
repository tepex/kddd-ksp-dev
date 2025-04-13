package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.kddd.IEntity
import ru.it_arch.kddd.ValueObject

public interface MyEntity : IEntity {

    override val id: Id
    public val content: Content

    override fun validate() {

    }

    public interface Id : ValueObject.Boxed<Int> {
        override fun validate() {}
    }

    public interface Content : ValueObject.Data {
        public val name: Name
        public val someField: SomeField

        override fun validate() {

        }

        public interface Name : ValueObject.Boxed<String> {
            override fun validate() {}
        }

        public interface SomeField : ValueObject.Boxed<Int> {
            override fun validate() {}
        }
    }

}
