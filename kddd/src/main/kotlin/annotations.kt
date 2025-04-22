package ru.it_arch.kddd

/**
 * Определяет параметры генерируемой имплементации для [ValueObject] и [IEntity]
 *
 * @property implementationName имя генерируемой имплементации. Переопределяет опцию KSP.
 * @property dsl вкл/выкл генерацию DSL. По умолчанию — true
 * @property json вкл/выкл генерацию JSON. По умолчанию — false
 * */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class KDGeneratable(
    val implementationName: String = "",
    val dsl: Boolean = true,
    val json: Boolean = false,
)

/**
 * Определяет методы сериализации и параметры генерируемой имплементации для [ValueObject.Boxed] общих типов (File, URI, UUID, и т.п.
 *
 * @property serialization метод сериализации имплементации. По умолчанию — `toString()`
 * @property deserialization способ создания имплементации. Имя статического метода. По умолчанию — через конструктор класса
 * @property useStringInDsl true — в DSL-билдере имплементация будет создаваться из строки. false — будет использоваться непосредственно
 * */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class KDParsable(
    val serialization: String = "toString()",
    val deserialization: String = "",
    val useStringInDsl: Boolean = false,
)

/**
 * Определяет явное имя поля для сериализации.
 *
 * Аналог [SerialName][kotlinx.serialization.SerialName].
 * @property name имя поля.
 * */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class KDSerialName(val value: String)

/**
 * Определяет, что в генерируемой доменной имплементации, тил свойства используется как есть — без оборачивания в `value class` */
/* TODO: А оно нужно?
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class AsIs
*/
