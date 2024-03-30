package space.iseki.dcc.gen

import org.benf.cfr.reader.api.CfrDriver
import org.benf.cfr.reader.api.ClassFileSource
import org.benf.cfr.reader.api.OutputSinkFactory
import org.benf.cfr.reader.api.OutputSinkFactory.SinkClass
import org.benf.cfr.reader.api.SinkReturns
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import space.iseki.dcc.Codec
import space.iseki.dcc.Decoder
import space.iseki.dcc.Encoder
import kotlin.test.assertEquals

class GenTest {

    data class Embedded(val x: Double = 0.0, val y: Double = 0.0)

    // Enhanced version of A with more complex properties
    data class A(
        val a: Int = 11,
        val b: Int,
        val c: String = "aaaa",
        val d: Embedded = Embedded(),
        val e: List<String> = listOf("example"),
        val f: Int? = null
    )

    @Test
    fun test2() {
        val enhancedA = DType(
            qname = A::class.java.name,
            fields = listOf(
                Field("a", "I", true),
                Field("b", "I", false),
                Field("c", "Ljava/lang/String;", true),
                Field(
                    "d",
                    "Lspace/iseki/dcc/gen/GenTest\$Embedded;",
                    true,
                ), // Note: Custom types should be referenced by their qualified name
                Field("e", "Ljava/util/List;", true),
                Field("f", "Ljava/lang/Integer;", false) // Nullable types are reference types in JVM bytecode
            ),
            useDefault = true,
        )
        println("===================== Generating... =====================")
        val data = Gen(Gen.ENABLE_CHECK or Gen.ENABLE_DEBUG_PRINT).generate(enhancedA)
        println("=====================   Generated   =====================")
        println("generated, length: " + data.size)
        val lc = loadClass(enhancedA.qname + "\$DCodec", data)
        println("printing decompiled source code: ")
        println(decompileByteCode(data))
        @Suppress("UNCHECKED_CAST") val aCodec = getInstance(lc as Class<Codec<A>>)
        assertEquals(6, aCodec.getFieldsMirror().size)
        assertEquals("a", aCodec.getFieldsMirror().first().name())
        assertEquals("f", aCodec.getFieldsMirror().last().name())

        // encoder test
        val encoder = Mockito.mock<Encoder>()!!
        aCodec.encodeTo(A(b = 12), encoder)
        Mockito.mockingDetails(encoder).printInvocations()//.also(::println)
        val eOrderVerifier = Mockito.inOrder(encoder)
        eOrderVerifier.verify(encoder).setInt(aCodec, 0, 11)
        eOrderVerifier.verify(encoder).setInt(aCodec, 1, 12)
        eOrderVerifier.verify(encoder).setObject(aCodec, 2, "aaaa")
        eOrderVerifier.verify(encoder).setObject(aCodec, 3, Embedded())
        eOrderVerifier.verify(encoder).setObject(aCodec, 4, listOf("example"))
        eOrderVerifier.verify(encoder).setObject(aCodec, 5, null)

        // decoder test
        val decoder = Mockito.mock<Decoder>()
        // 0
        Mockito.`when`(decoder.isDefault(aCodec, 0)).thenReturn(true)
        Mockito.`when`(decoder.getInt(aCodec, 0)).then { error("shouldn't be called") }
        // 1
        Mockito.`when`(decoder.isDefault(aCodec, 1)).thenReturn(false)
        Mockito.`when`(decoder.getInt(aCodec, 1)).thenReturn(5)
        // 2
        Mockito.`when`(decoder.isDefault(aCodec, 2)).thenReturn(false)
        Mockito.`when`(decoder.getObject(aCodec, 2, String::class.java)).thenReturn("foo")
        // 3
        Mockito.`when`(decoder.isDefault(aCodec, 3)).thenReturn(true)
        Mockito.`when`(decoder.getObject(aCodec, 3, Embedded::class.java)).then { error("shouldn't be called") }
        // 4
        Mockito.`when`(decoder.isDefault(aCodec, 4)).thenReturn(false)
        Mockito.`when`(decoder.getObject(aCodec, 4, List::class.java)).thenReturn(listOf("duang"))

        try {
            val r = aCodec.decodeFrom(decoder)
            assertEquals(A(b = 5, c = "foo", e = listOf("duang")), r)
        } catch (th: Throwable) {
            Mockito.mockingDetails(decoder).printInvocations().also(::println)
            throw th
        }
    }

    private fun loadClass(name: String, ba: ByteArray): Class<*> {
        val cls: Class<*>
        object : ClassLoader() {
            init {
                cls = defineClass(name, ba, 0, ba.size)
            }
        }
        return cls
    }

    private fun <T : Any> getInstance(cdc: Class<Codec<T>>): Codec<T> {
        @Suppress("UNCHECKED_CAST") return cdc.declaredFields.find { it.name == "INSTANCE" }!!.get(null) as Codec<T>
    }

    private fun decompileByteCode(bc: ByteArray): String {
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
                p0: OutputSinkFactory.SinkType?, p1: MutableCollection<SinkClass>?
            ): List<SinkClass> {
                return if (p0 == OutputSinkFactory.SinkType.JAVA) {
                    listOf(SinkClass.DECOMPILED)
                } else {
                    emptyList()
                }
            }

            override fun <T : Any?> getSink(
                p0: OutputSinkFactory.SinkType?, p1: SinkClass?
            ): OutputSinkFactory.Sink<T> = OutputSinkFactory.Sink {
                if (it is SinkReturns.Decompiled) {
                    j = it.java
                }
            }
        }).withClassFileSource(o).build().analyse(listOf("foo.class"))
        return j
    }

}

data class A(val a: Int = 11, val b: Int = 12, val c: String = "aaaa")

