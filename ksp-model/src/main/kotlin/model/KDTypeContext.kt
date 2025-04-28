package ru.it_arch.clean_ddd.ksp_model.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
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
 * @property kddd имя интерфейса исходной [Kddd]-модели.
 * @property impl полностью квалифицированное имя класса имплементации [Kddd]-типа.
 * @property annotations список аннотаций [Kddd]-типа.
 * @property properties список свойств [Kddd]-типа.
 * */
@ConsistentCopyVisibility
public data class KDTypeContext private constructor(
    val options: KDOptions,
    val logger : KDLogger,
    val typeCatalog: TypeCatalog,
    val kddd: TypeName,
    val impl: KDClassName,
    //val fullClassName: KDClassName.FullClassNameBuilder,
    val annotations: Sequence<Annotation>,
    val properties: List<KDProperty>
) : ValueObject.Data {

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T =
        TODO("Must not used")

    public companion object {
        context(options: KDOptions, logger: KDLogger)
        public operator fun invoke(
            typeCatalog: TypeCatalog,
            kddd: TypeName,
            impl: KDClassName,
            annotations: Sequence<Annotation>,
            properties: List<KDProperty>
        ): KDTypeContext = KDTypeContext(
            options,
            logger,
            typeCatalog,
            kddd,
            impl,
            annotations,
            properties
        )
    }
}
