package ru.it_arch.kddd.presentation

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import ru.it_arch.kddd.KDIgnore
import ru.it_arch.kddd.domain.model.CompositeClassName
import ru.it_arch.kddd.domain.model.type.KdddType

public typealias TypeCatalog = Map<CompositeClassName.FullClassName, KspTypeHolder>

public typealias KdddModelHolder = Pair<KdddType.ModelContainer, Dependencies>

@OptIn(KspExperimental::class)
public fun Resolver.collectAllKdddModels(visitor: Visitor): List<KdddModelHolder> =
    getNewFiles().toList().mapNotNull { file ->
        file.declarations
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.INTERFACE && it.getAnnotationsByType(KDIgnore::class).count() == 0 }
            .firstOrNull()
            ?.let { declaration ->
                visitor.visitKDDeclaration(declaration, null).getOrNull()
                    .let { kdddType ->
                        if (kdddType is KdddType.ModelContainer) KdddModelHolder(kdddType, Dependencies(false, file))
                        else null
                    }
            }
    }


