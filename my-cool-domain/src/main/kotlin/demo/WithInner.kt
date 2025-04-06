package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.ValueObject

/**
 * Демонстрация использования вложенных типов в качестве полей.
 *
 */
@KDGeneratable(json = false)
public interface WithInner : ValueObject.Data {
    public val myInner: Inner
    public val myOptionalInner: Inner?

    override fun validate() {

    }

    @KDGeneratable(json = false)
    public interface Inner : ValueObject.Data {
        public val innerLong: InnerLong
        public val innerStr: InnerStr

        override fun validate() {}

        public interface InnerLong : ValueObject.Boxed<Long> {
            override fun validate() {}
        }

        public interface InnerStr : ValueObject.Boxed<String> {
            override fun validate() {}
        }
    }
}
