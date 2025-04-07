package ru.it_arch.clean_ddd.ksp

import java.io.Closeable
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

internal class StringBufferedWriter(os: OutputStream) : Appendable, Closeable, AutoCloseable {

    private val buffer =  StringBuilder()
    private val out = OutputStreamWriter(os, StandardCharsets.UTF_8)

    override fun append(value: CharSequence?): Appendable {
        buffer.append(value)
        if (value?.contains('\n') == true) processLine()
        return this
    }

    override fun append(value: CharSequence?, start: Int, end: Int): Appendable =
        append(value?.subSequence(start, end))

    override fun append(value: Char): Appendable {
        buffer.append(value)
        if (value == '\n') processLine()
        return this
    }

    override fun close() {
        out.close()
    }

    private fun processLine() {
        buffer.toString().replace("import ru.it_arch.clean_ddd.ksp.model.OptIn", "import kotlinx.serialization.ExperimentalSerializationApi")
            .also(out::append)
        buffer.clear()
    }
}
