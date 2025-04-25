package ru.it_arch.clean_ddd.ksp_model.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import ru.it_arch.clean_ddd.ksp_model.FullClassNameBuilder
import ru.it_arch.clean_ddd.ksp_model.TypeCatalog
import ru.it_arch.clean_ddd.ksp_model.utils.KDLogger
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

/**
 * Контекст, необходимый для генерации имплементаций [KDType] [Kddd]-типов.
 *
 * @property options опции фреймворка.
 * @property logger внутренний логгер.
 * @property typeCatalog реестр всех созданных KDDD-типов ([Kddd]-тип -> [KDType]).
 * @property kDddPackage пакет [Kddd]-типа.
 * @property name имя интерфейса исходной [Kddd]-модели.
 * @property implName полностью квалифицированное имя класса имплементации [Kddd]-типа.
 * @property annotations список аннотаций [Kddd]-типа.
 * @property properties список свойств [Kddd]-типа.
 * */
@ConsistentCopyVisibility
public data class KDTypeContext private constructor(
    val options: KDOptions,
    val logger : KDLogger,
    val typeCatalog: TypeCatalog,
    val kDddPackage: PackageName,
    val implPackage: PackageName,
    val name: TypeName,
    val implName: ClassName,
    val fullClassName: FullClassNameBuilder,
    val annotations: Sequence<Annotation>,
    val properties: List<KDProperty>
) : ValueObject.Data {

    init {
        validate()
    }

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T =
        TODO("Not yet implemented")

    public companion object {
        context(options: KDOptions, logger: KDLogger)
        public operator fun invoke(
            typeCatalog: TypeCatalog,
            kDddPackage: PackageName,
            implPackage: PackageName,
            name: TypeName,
            implName: ClassName,
            fullClassName: FullClassNameBuilder,
            annotations: Sequence<Annotation>,
            properties: List<KDProperty>
        ): KDTypeContext = KDTypeContext(
            options,
            logger,
            typeCatalog,
            kDddPackage,
            implPackage,
            name,
            implName,
            fullClassName,
            annotations,
            properties
        )
    }
}
