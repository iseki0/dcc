package space.iseki.dcc.gen

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.CheckClassAdapter
import space.iseki.dcc.Codec
import space.iseki.dcc.Encoder
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import javax.tools.*


class PostProcessClassVisitorTest {
    @Test
    @Suppress("UNCHECKED_CAST")
    fun testRecord() {
        @Language("Java") val code = """
            import space.iseki.dcc.*;
            @Dcc
            public record foo(int a, int b){
                public static Codec<foo> getCodec(){
                    return Data.getCodec(foo.class);
                }
            }
        """.trimIndent()
        val fooBC = compileJava(code, "foo")
        println("compiled, length ${fooBC.size}")
        val cr = ClassReader(fooBC)
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        val ppcv = PostProcessClassVisitor(CheckClassAdapter(cw, true))
        cr.accept(ppcv, 0)
        val codecBC = Gen(Gen.ENABLE_CHECK).generate(ppcv.dType)
        val ppBC = cw.toByteArray()
        println("processed, codec: ${codecBC.size}, post-processed: ${ppBC.size}")
        println("============ Dump post-processed ===================")
        dumpByteCodeToStdout(ppBC)
//        println("============      Decompiled     ===================")
//        decompileByteCode(ppBC).also(::println)
        println("============         DONE        ===================")
        val classes = loadClasses(mapOf("foo\$DCodec" to codecBC!!, "foo" to ppBC))
        val fooCls = classes["foo"] as Class<Codec<Any>>
        println(fooCls.constructors.toList())
        val fooObj = classes["foo"]!!.getConstructor(Int::class.java, Int::class.java).newInstance(1, 2)
        println(fooObj)
        val encoder = Mockito.mock(Encoder::class.java)!!
        val fooCodec = fooCls.declaredMethods.find { it.name == "getCodec" }!!.invoke(null) as Codec<Any>
        println(fooCodec)
        fooCodec.encodeTo(fooObj, encoder)
        val eOrderVerifier = Mockito.inOrder(encoder)
        eOrderVerifier.verify(encoder).setInt(fooCodec, 0, 1)
        eOrderVerifier.verify(encoder).setInt(fooCodec, 1, 2)
    }

}

internal class SimpleForwardingFM(jfm: JavaFileManager) : ForwardingJavaFileManager<JavaFileManager>(jfm) {
    val byteCodeMap: MutableMap<String, ByteArrayOutputStream> = HashMap()

    @Throws(IOException::class)
    override fun getJavaFileForOutput(
        location: JavaFileManager.Location, className: String, kind: JavaFileObject.Kind, sibling: FileObject
    ): JavaFileObject {
        if (kind == JavaFileObject.Kind.CLASS) {
            return ByteCode(className, byteCodeMap)
        }
        return super.getJavaFileForOutput(location, className, kind, sibling)
    }
}

internal class ByteCode(
    className: String, byteCodeMap: MutableMap<String, ByteArrayOutputStream>
) : SimpleJavaFileObject(URI.create("bytecode:///$className"), JavaFileObject.Kind.CLASS) {
    private val outputStream = ByteArrayOutputStream()

    init {
        byteCodeMap[className] = outputStream
    }

    override fun openOutputStream(): ByteArrayOutputStream {
        return outputStream
    }
}

internal class JavaSourceFromString(name: String, private val code: String) : SimpleJavaFileObject(
    URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE
) {
    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
        return code
    }
}
