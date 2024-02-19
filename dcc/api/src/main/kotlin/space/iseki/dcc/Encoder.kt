package space.iseki.dcc

@Suppress("unused")
interface Encoder {
    fun setByte(name: String, value: Byte) = setObject(name, value)
    fun setChar(name: String, value: Char) = setObject(name, value)
    fun setShort(name: String, value: Short) = setObject(name, value)
    fun setInt(name: String, value: Int) = setObject(name, value)
    fun setLong(name: String, value: Long) = setObject(name, value)
    fun setFloat(name: String, value: Float) = setObject(name, value)
    fun setDouble(name: String, value: Double) = setObject(name, value)
    fun setBoolean(name: String, value: Boolean) = setObject(name, value)
    fun setObject(name: String, value: Any?)
}