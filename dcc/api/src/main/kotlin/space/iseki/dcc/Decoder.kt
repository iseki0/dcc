package space.iseki.dcc

@Suppress("unused")
interface Decoder {
    fun getByte(name: String): Byte = getObject(name, Byte::class.java) as Byte
    fun getChar(name: String): Char = getObject(name, Char::class.java) as Char
    fun getShort(name: String): Short = getObject(name, Short::class.java) as Short
    fun getInt(name: String): Int = getObject(name, Int::class.java) as Int
    fun getLong(name: String): Long = getObject(name, Long::class.java) as Long
    fun getFloat(name: String): Float = getObject(name, Float::class.java) as Float
    fun getDouble(name: String): Double = getObject(name, Double::class.java) as Double
    fun getBoolean(name: String): Boolean = getObject(name, Boolean::class.java) as Boolean
    fun getObject(name: String, type: Class<*>): Any?
    fun isDefault(name: String): Boolean
}


