package space.iseki.dcc

@Suppress("unused")
interface FieldMirror {
    fun name(): String
    fun type(): Class<*>
    fun optional(): Boolean
}