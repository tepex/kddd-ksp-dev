package ru.it_arch.clean_ddd.ksp_model.model

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import ru.it_arch.kddd.KDSerialName

/**
 * Определяет свойство в классе имплементации.
 *
 * @property name имя свойства.
 * @property type [KotlinPoet](https://square.github.io/kotlinpoet/1.x/kotlinpoet/kotlinpoet/com.squareup.kotlinpoet/-type-name/index.html) тип свойства.
 * @property serialName имя сериализированного свойства если применена аннотация [KDSerialName].
 * */
@ConsistentCopyVisibility
public data class KDProperty private constructor(
    val name: MemberName,
    val type: TypeName,
    private val serialNameAnnotation: KDSerialName?
) {

    val serialName: String = serialNameAnnotation?.value ?: name.simpleName

    public companion object {
        public operator fun invoke(name: MemberName, type: TypeName, annotation: KDSerialName? = null): KDProperty =
            KDProperty(name, type, annotation)
    }
}
