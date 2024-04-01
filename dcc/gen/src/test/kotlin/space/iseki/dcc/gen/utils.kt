package space.iseki.dcc.gen

import org.benf.cfr.reader.api.CfrDriver
import org.benf.cfr.reader.api.ClassFileSource
import org.benf.cfr.reader.api.OutputSinkFactory
import org.benf.cfr.reader.api.SinkReturns
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair
import org.intellij.lang.annotations.Language
import space.iseki.dcc.Codec
import javax.tools.ToolProvider

fun dumpByteCodeToStdout(bc: ByteArray) {
    val cr = org.objectweb.asm.ClassReader(bc)
    cr.accept(org.objectweb.asm.util.TraceClassVisitor(java.io.PrintWriter(System.out)), 0)
}

fun compileJava(@Language("Java") code: String, className: String): ByteArray {
    val compiler = ToolProvider.getSystemJavaCompiler()
    val file = JavaSourceFromString(className, code)
    SimpleForwardingFM(compiler.getStandardFileManager(null, null, null)).use {
        val task = compiler.getTask(null, it, null, null, null, listOf(file))
        check(task.call()) { "compilation failed" }
        return it.byteCodeMap[className]!!.toByteArray()
    }
}


internal fun <T> loadClass(name: String, ba: ByteArray): Class<T> {
    @Suppress("UNCHECKED_CAST") return loadClasses(mapOf(name to ba))[name] as Class<T>
}

internal fun loadClasses(m: Map<String, ByteArray>): Map<String, Class<*>> {
    val r: Map<String, Class<*>>
    object : ClassLoader() {
        init {
            r = m.mapValues { defineClass(it.key, it.value, 0, it.value.size) }
        }
    }
    return r
}

internal fun <T : Any> getCodecInstance(cdc: Class<Codec<T>>): Codec<T> {
    @Suppress("UNCHECKED_CAST") return cdc.declaredFields.find { it.name == "INSTANCE" }!!.get(null) as Codec<T>
}

internal fun decompileByteCode(bc: ByteArray): String {
    var j = ""
    val o = object : ClassFileSource {
        var f = true
        override fun informAnalysisRelativePathDetail(usePath: String?, classFilePath: String?) {}

        override fun addJar(jarPath: String?): Collection<String> = emptyList()

        override fun getPossiblyRenamedPath(path: String): String {
            println("getPossiblyRenamedPath: $path -> $f")
            if (f) {
                f = false
                return "foo.class"
            }
            return path
        }

        override fun getClassFileContent(path: String?): Pair<ByteArray, String> {
            println("getClassFileContent: $path")
            return Pair.make(bc, "foo.class")
        }
    }

    CfrDriver.Builder().withOutputSink(object : OutputSinkFactory {
        override fun getSupportedSinks(
            p0: OutputSinkFactory.SinkType?, p1: MutableCollection<OutputSinkFactory.SinkClass>?
        ): List<OutputSinkFactory.SinkClass> {
            return if (p0 == OutputSinkFactory.SinkType.JAVA) {
                listOf(OutputSinkFactory.SinkClass.DECOMPILED)
            } else {
                emptyList()
            }
        }

        override fun <T : Any?> getSink(
            p0: OutputSinkFactory.SinkType?, p1: OutputSinkFactory.SinkClass?
        ): OutputSinkFactory.Sink<T> = OutputSinkFactory.Sink {
            if (it is SinkReturns.Decompiled) {
                j = it.java
            }
        }
    }).withClassFileSource(o).build().analyse(listOf("foo.class"))
    return j
}