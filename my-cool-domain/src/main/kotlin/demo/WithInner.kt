package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.k3dm.Generatable
import ru.it_arch.k3dm.ValueObject

/**
 * Демонстрация использования вложенных типов в качестве полей.
 *
 */
@Generatable(json = true)
public interface WithInner : ValueObject.Data {
    public val myInner: MyInner
    public val myOptionalInner: MyInner?

    override fun validate() {

    }

    @Generatable(json = true)
    public interface MyInner : ValueObject.Data {
        public val innerLong: InnerLong
        public val innerStr: InnerStr

        override fun validate() {}

        public interface InnerLong : ValueObject.Value<Long> {
            override fun validate() {}
        }

        public interface InnerStr : ValueObject.Value<String> {
            override fun validate() {}
        }
    }
}
