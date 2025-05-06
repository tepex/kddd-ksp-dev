package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.KSPLogger
import ru.it_arch.clean_ddd.domain.ILogger

internal class LoggerImpl private constructor(private val kspLogger: KSPLogger) : ILogger {
    override fun log(text: String) {
        kspLogger.warn(text)
    }

    override fun err(text: String) {
        kspLogger.error(text)
    }

    companion object {
        operator fun invoke(logger: KSPLogger): ILogger = LoggerImpl(logger)
    }
}
