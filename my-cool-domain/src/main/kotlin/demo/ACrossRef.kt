package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.ValueObject
import java.util.UUID
import ru.it_arch.clean_ddd.domain.demo.sub.Point as Point1

public interface ACrossRef : ValueObject.Data {
    public val myType: MyCustomInnerType
    public val point: Point
    public val point1: Point1?
    public val x: Point1.Coordinate
    public val myUUID: MyUUID
    public val myOptionalUUID: MyUUID?
    public val nested: Nested
    public val nestedNested: Nested.NestedNested
    public val myList: List<MyCustomInnerType>
    public val myMap: Map<MyCustomInnerType, List<Point?>>

    override fun validate() {}

    public interface MyCustomInnerType : ValueObject.Value<Int> {
        override fun validate() {  }
    }

    @KDParsable(deserialization = "fromString")
    public interface MyUUID : ValueObject.Value<UUID> {
        override fun validate() {}
    }

    public interface Nested : ValueObject.Data {
        public val someType: SomeType
        public val nestedNested: NestedNested

        override fun validate() {}

        public interface SomeType : ValueObject.Value<String> {
            override fun validate() {}
        }

        public interface NestedNested : ValueObject.Data {
            public val someType: SomeType

            override fun validate() {}
        }
    }
}
