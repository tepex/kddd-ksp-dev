package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.KSPLogger
import ru.it_arch.clean_ddd.domain.KDLogger

public class KDLoggerImpl private constructor(private val kspLogger: KSPLogger) : KDLogger {
    override fun log(text: String) {
        kspLogger.warn(text)
    }

    override fun err(text: String) {
        kspLogger.error(text)
    }

    public companion object {
        public operator fun invoke(logger: KSPLogger): KDLogger = KDLoggerImpl(logger)
    }
}
