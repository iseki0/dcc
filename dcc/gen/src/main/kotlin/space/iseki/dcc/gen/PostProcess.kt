package space.iseki.dcc.gen

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.nio.file.Files
import kotlin.io.path.extension

object PostProcess {
    @JvmStatic
    fun processDir(dir: File) {
        val fileSeq = dir.walk().filter {
            val p = it.toPath()
            Files.isRegularFile(p) && Files.isReadable(p) && p.extension == "class"
        }
        for(file in fileSeq){
            val cr = ClassReader(file.readBytes())
            val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
            val ppcv = PostProcessClassVisitor(cw)
            cr.accept(ppcv, 0)
            val dType = ppcv.dType
            if (ppcv.isModified) file.writeBytes(cw.toByteArray())
            if (dType!=null){
                val gen = Gen(0)
                val generated = gen.generate(dType)
                val t = File(file.parentFile, file.name.removeSuffix(".class")+"\$DCodec.class")
                t.writeBytes(generated)
            }
        }
    }
}
