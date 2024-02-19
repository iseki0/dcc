package space.iseki.dcc.gen

import space.iseki.dcc.Dcc
import java.util.*

@JvmRecord
@Dcc
data class Field(
    val name: String,
    val descriptor: String,
    val optional: Boolean = false,
    val getter: String = "get" + name.capitalize(),
)

private fun String.capitalize() =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

@JvmRecord
@Dcc
data class DType(val qname: String, val fields: List<Field>, val useDefault: Boolean)

