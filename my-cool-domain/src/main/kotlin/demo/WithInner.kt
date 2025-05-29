package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDIgnore
import ru.it_arch.kddd.ValueObject

/**
 * Демонстрация использования вложенных типов в качестве полей.
 *
 */
@KDIgnore
@KDGeneratable(json = true)
public interface WithInner : ValueObject.Data {
    public val myInner: MyInner
    public val myOptionalInner: MyInner?

    override fun validate() {

    }

    @KDGeneratable(json = true)
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
