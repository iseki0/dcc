import org.junit.jupiter.api.Test
import space.iseki.dcc.Data
import space.iseki.dcc.Dcc

class Test0 {

    @Dcc
    data class A(val a: String, val b: Int = 1)
    @Test
    fun test(){
        println(Data.getCodec(A::class.java))
    }
}