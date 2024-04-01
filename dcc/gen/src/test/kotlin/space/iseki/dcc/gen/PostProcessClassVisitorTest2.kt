package space.iseki.dcc.gen

import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.CheckClassAdapter
import org.objectweb.asm.util.TraceClassVisitor
import space.iseki.dcc.Codec
import space.iseki.dcc.Data
import space.iseki.dcc.Dcc
import space.iseki.dcc.Encoder
import java.io.PrintWriter
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PostProcessClassVisitorTest2 {

    @JvmRecord
    @Dcc
    data class DR(val a: String, val b: Int = 1) {
        companion object : TC {
            override val name: String = DR::class.java.name

            override val bc: ByteArray = getByteCode(DR::class.java)
            override val d = DType(
                qname = name.replace('.', '/'),
                fields = listOf(
                    Field(name = "a", "Ljava/lang/String;", optional = false, getter = "a"),
                    Field(name = "b", "I", optional = true, getter = "b"),
                ),
                useDefault = true,
            )
        }
    }

    class DRT : TCT {
        override fun doTest() {
            println("============== doTest")
            println(Data.getCodec(DR::class.java))
        }

    }
    class DT : TCT {
        override fun doTest() {
            println("============== doTest")
            println(Data.getCodec(D::class.java))
            if (System.getenv()!=null){
                val r =Data.getCodec(D::class.java)
                println(r)

                r.encodeTo(D("aaa"), testEncoder)
            }
        }

    }


    @Dcc
    data class D(val a: String, val b: Int = 1) {
        companion object : TC {
            override val name: String = D::class.java.name
            override val bc: ByteArray = getByteCode(D::class.java)
            override val d = DType(
                qname = name.replace('.', '/'),
                fields = listOf(
                    Field(name = "a", "Ljava/lang/String;", optional = false, getter = "getA"),
                    Field(name = "b", "I", optional = true, getter = "getB"),
                ),
                useDefault = true,
            )
        }
    }


    interface TC {
        val name: String
        val bc: ByteArray
        val d: DType
    }

    interface TCT {
        fun doTest()
    }


    private fun t(bc: ByteArray, d: DType) {
        val cr = ClassReader(bc)
        val ppcv = PostProcessClassVisitor(null)
        cr.accept(ppcv, 0)
        assertNotNull(ppcv.dType)
        assertEquals(d, ppcv.dType)
    }
    @Test
    fun testAnalyze() {
        t(DR.bc, DR.d)
        t(D.bc, D.d)
    }

    @Test
    fun testGenerate_DR() {
        tg1(DR.d, DRT::class.java)
    }
    @Test
    fun testGenerate_D() {
        tg1(D.d, DT::class.java)
    }

    private fun <T> tg1(dType: DType, tct: Class<T>) where T : TCT {
        val g = Gen(Gen.ENABLE_CHECK).generate(dType)
        val cName = dType.qname + "\$DCodec"
        val bc = getByteCode(tct)
        val cr = ClassReader(bc)
        run {
            println("==== Raw bytecode ====")
            val tcv = TraceClassVisitor(null, PrintWriter(System.out))
            cr.accept(tcv, 0)
            println("==== End ====")
        }
        println("==== Post processed ====")
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        val cca = CheckClassAdapter(cw, true)
        val tcv = TraceClassVisitor(cca, PrintWriter(System.out))
        val p = PostProcessClassVisitor(tcv)
        cr.accept(p, 0)
        println("==== End ====")
        assertNull(p.dType)
        val tBc = cw.toByteArray()
        object : ClassLoader(this::class.java.classLoader) {
            @Suppress("RedundantNullableReturnType") // initialization
            val cm: Map<String, Class<*>>? = mapOf(
                cName to defineClass(cName.replace('/', '.'), g, 0, g.size)
            ).also { println("map: $it") }

            override fun loadClass(name: String): Class<*> {
                println("load $name")
                cm?.get(name)?.let{return it}
                return super.loadClass(name)
            }

            init {
                println("defineClass...")
                val c = defineClass(tct.name,tBc, 0, tBc.size)
                println("post defineClass")
                @Suppress("DEPRECATION")
                val i = c.newInstance()
                println(i as TCT)
                i.doTest()
            }
        }
    }

}

private fun getByteCode(c: Class<*>): ByteArray {
    val iName = c.name.replace('.', '/')
    return object {}::class.java.classLoader.getResourceAsStream("$iName.class")!!.use { it.readAllBytes() }
}

private val testEncoder = object: Encoder{
    override fun setObject(codec: Codec<*>, name: String, value: Any?) {
        println("$name -> $value")
    }
}
