package space.iseki.dcc

@Suppress("unused")
interface Codec<T : Any> {
    fun decodeFrom(decoder: Decoder): T
    fun encodeTo(data: T, encoder: Encoder)
    fun getFieldsMirror(): List<FieldMirror>
    fun getNameByIndex(index: Int): String = getFieldsMirror()[index].name()

    companion object {
        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun versionCheck(major: Int, minor: Int) {
        }
    }
}
