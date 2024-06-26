@file:JvmName("Utils")

package space.iseki.dcc.gen

import kotlinx.metadata.declaresDefaultValue
import kotlinx.metadata.isData
import kotlinx.metadata.isSecondary
import kotlinx.metadata.jvm.KotlinClassMetadata

private val EMPTY_INT_ARRAY = IntArray(0)
private val EMPTY_STRING_ARRAY = Array(0) { "" }
internal fun genMetadata(
    k: Int?,
    mv: IntArray?,
    bv: IntArray?,
    data1: Array<String>?,
    data2: Array<String>?,
    xs: String?,
    pn: String?,
    xi: Int?
) = Metadata(
    kind = k ?: 1,
    metadataVersion = mv ?: EMPTY_INT_ARRAY,
    bytecodeVersion = bv ?: EMPTY_INT_ARRAY,
    data1 = data1 ?: EMPTY_STRING_ARRAY,
    data2 = data2 ?: EMPTY_STRING_ARRAY,
    extraString = xs ?: "",
    packageName = pn ?: "",
    extraInt = xi ?: 0,
)

internal fun readDataKM(metadata: Metadata): KmData? {
    val km = (KotlinClassMetadata.readStrict(metadata) as? KotlinClassMetadata.Class)?.kmClass?.takeIf { it.isData }
        ?: return null
    val p = km.constructors.first { !it.isSecondary }.valueParameters.map { KmData.F(it.name, it.declaresDefaultValue) }
    return KmData(p)
}

@JvmRecord
data class KmData(val fields: List<F>) {
    @JvmRecord
    data class F(val name: String, val optional: Boolean)
}

