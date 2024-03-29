package space.iseki.dcc.gen

import org.junit.jupiter.api.Test
import space.iseki.dcc.Codec
import space.iseki.dcc.Decoder
import space.iseki.dcc.Encoder

class GenTest {
    @Test
    fun test(){
        val t = DType(
            qname = A::class.qualifiedName!!,
            fields = listOf(
                Field("a", "I", false),
                Field("b", "I", true),
                Field("c", "Ljava/lang/String;", true),
            ),
            useDefault = true,
        )
        val gen = Gen(Gen.ENABLE_CHECK or Gen.ENABLE_DEBUG_PRINT)
        val bc = gen.generate(t)
        object : ClassLoader() {
            init {
                val clz = defineClass(t.qname + "\$DCodec", bc, 0, bc.size)
                val a = clz.getDeclaredField("INSTANCE").get(null)
                @Suppress("UNCHECKED_CAST")
                println(a as Codec<A>)
                val data = a.decodeFrom(object : Decoder {
                    override fun getObject(codec: Codec<*>,name: String, type: Class<*>): Any {
                        println("$name -> $type")
                        if (name == "c") return "cv"
                        return 1
                    }

                    override fun isDefault(codec: Codec<*>,name: String): Boolean {
                        return name != "c"
                    }
                })
                println(data)
                println(a.getFieldsMirror())
                println(a.getFieldsMirror()::class.java)
                a.encodeTo(data, object : Encoder {
                    override fun setObject(codec: Codec<*>, name: String, value: Any?) {
                        println("set: $name -> $value")
                    }
                })
            }
        }

    }
}

data class A(val a: Int = 11, val b: Int = 12, val c: String = "aaaa")

