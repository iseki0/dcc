package space.iseki.dcc.gen

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

    @JvmRecord
    data class TestRecord(
        val i: Int,
        val iDefault: Int = 2,
        val iNull: Int?,
        val iNullDefault: Int? = 4,
    )

    data class TestData(
        val i: Int,
        val iDefault: Int = 2,
        val iNull: Int?,
        val iNullDefault: Int? = 4,
    )

    @Test
    fun testRecord() {
        val m = DType(
            qname = TestRecord::class.java.name,
            fields = listOf(
                Field("i", "I", false, "i"),
                Field("iDefault", "I", true, "iDefault"),
                Field("iNull", "Ljava/lang/Integer;", false, "iNull"),
                Field("iNullDefault", "Ljava/lang/Integer;", true, "iNullDefault"),
            ),
            useDefault = true,
        )
        val data = Gen(Gen.ENABLE_CHECK or Gen.ENABLE_DEBUG_PRINT).generate(m)
        val lc = loadClass<Codec<TestRecord>>(m.qname + "\$DCodec", data)
        val codec = getCodecInstance(lc)
        // test encoding
        val encoder = Mockito.mock<Encoder>()!!
        codec.encodeTo(TestRecord(1, 2, 3, 4), encoder)
        val eOrderVerifier = Mockito.inOrder(encoder)
        eOrderVerifier.verify(encoder).setInt(codec, 0, 1)
        eOrderVerifier.verify(encoder).setInt(codec, 1, 2)
        eOrderVerifier.verify(encoder).setObject(codec, 2, 3)
        eOrderVerifier.verify(encoder).setObject(codec, 3, 4)
        // test decoding
        val decoder = Mockito.mock<Decoder>()
        Mockito.`when`(decoder.isDefault(codec, 0)).thenReturn(false)
        Mockito.`when`(decoder.getInt(codec, 0)).thenReturn(5)
        Mockito.`when`(decoder.isDefault(codec, 1)).thenReturn(true)
        Mockito.`when`(decoder.getInt(codec, 1)).then { error("shouldn't be called") }
        Mockito.`when`(decoder.isDefault(codec, 2)).thenReturn(false)
        Mockito.`when`(decoder.getObject(codec, 2, Int::class.java)).thenReturn(6)
        Mockito.`when`(decoder.isDefault(codec, 3)).thenReturn(true)
        Mockito.`when`(decoder.getObject(codec, 3, Int::class.java)).then { error("shouldn't be called") }
        val r = codec.decodeFrom(decoder)
        assertEquals(TestRecord(5, 2, null, 4), r)
    }

    @Test
    fun testCommon() {
        val m = DType(
            qname = TestData::class.java.name,
            fields = listOf(
                Field("i", "I", false),
                Field("iDefault", "I", true),
                Field("iNull", "Ljava/lang/Integer;", false),
                Field("iNullDefault", "Ljava/lang/Integer;", true),
            ),
            useDefault = true,
        )
        val data = Gen(Gen.ENABLE_CHECK or Gen.ENABLE_DEBUG_PRINT).generate(m)
        val lc = loadClass<Codec<TestData>>(m.qname + "\$DCodec", data)
        val codec = getCodecInstance(lc)
        // test encoding
        val encoder = Mockito.mock<Encoder>()!!
        codec.encodeTo(TestData(1, 2, 3, 4), encoder)
        val eOrderVerifier = Mockito.inOrder(encoder)
        eOrderVerifier.verify(encoder).setInt(codec, 0, 1)
        eOrderVerifier.verify(encoder).setInt(codec, 1, 2)
        eOrderVerifier.verify(encoder).setObject(codec, 2, 3)
        eOrderVerifier.verify(encoder).setObject(codec, 3, 4)
        // test decoding
        val decoder = Mockito.mock<Decoder>()
        Mockito.`when`(decoder.isDefault(codec, 0)).thenReturn(false)
        Mockito.`when`(decoder.getInt(codec, 0)).thenReturn(5)
        Mockito.`when`(decoder.isDefault(codec, 1)).thenReturn(true)
        Mockito.`when`(decoder.getInt(codec, 1)).then { error("shouldn't be called") }
        Mockito.`when`(decoder.isDefault(codec, 2)).thenReturn(false)
        Mockito.`when`(decoder.getObject(codec, 2, Int::class.java)).thenReturn(6)
        Mockito.`when`(decoder.isDefault(codec, 3)).thenReturn(true)
        Mockito.`when`(decoder.getObject(codec, 3, Int::class.java)).then { error("shouldn't be called") }
        val r = codec.decodeFrom(decoder)
        assertEquals(TestData(5, 2, null, 4), r)
    }

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
        val lc = loadClass<Codec<A>>(enhancedA.qname + "\$DCodec", data)
        println("printing decompiled source code: ")
        println(decompileByteCode(data))
        val aCodec = getCodecInstance(lc)
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


    @Test
    fun test33Optional() {
        data class A(
            val f0: Int = 0,
            val f1: Int = 1,
            val f2: Int = 2,
            val f3: Int = 3,
            val f4: Int = 4,
            val f5: Int = 5,
            val f6: Int = 6,
            val f7: Int = 7,
            val f8: Int = 8,
            val f9: Int = 9,
            val f10: Int = 10,
            val f11: Int = 11,
            val f12: Int = 12,
            val f13: Int = 13,
            val f14: Int = 14,
            val f15: Int = 15,
            val f16: Int = 16,
            val f17: Int = 17,
            val f18: Int = 18,
            val f19: Int = 19,
            val f20: Int = 20,
            val f21: Int = 21,
            val f22: Int = 22,
            val f23: Int = 23,
            val f24: Int = 24,
            val f25: Int = 25,
            val f26: Int = 26,
            val f27: Int = 27,
            val f28: Int = 28,
            val f29: Int = 29,
            val f30: Int = 30,
            val f31: Int = 31,
            val f32: Int = 32,
        )

        val dType = DType(
            qname = A::class.java.name,
            fields = (0..32).map { Field("f$it", "I", true) },
            useDefault = true,
        )
        val ba = Gen(Gen.ENABLE_CHECK).generate(dType)
        val a = getCodecInstance(loadClass<Codec<A>>(dType.qname + "\$DCodec", ba))
        val decoder = Mockito.mock<Decoder>()
        Mockito.`when`(decoder.isDefault(Mockito.eq(a), Mockito.intThat { it < 34 })).thenReturn(false)
        Mockito.`when`(decoder.getInt(Mockito.eq(a), Mockito.intThat { it < 34 })).thenReturn(100)
        val expected = A::class.java.constructors.find { it.parameterCount == 33 }!!
            .newInstance(*(1..33).map { 100 }.toTypedArray())
        val decoded = a.decodeFrom(decoder)
        assertEquals(expected, decoded)
        println(decoded)
        decompileByteCode(ba).also(::println)
    }
}

