package com.ubopod.uboapp.phone.ui.common

import com.ubopod.ubokotlin.models.RenderPropValue
import com.ubopod.ubokotlin.models.RenderViewData

/**
 * Pull a string-shaped prop from a [RenderViewData], looking up the first
 * key whose value is a [RenderPropValue.StringValue]. Falls back to the
 * empty string when none of the requested keys are set.
 *
 * Mirrors the Swift sub-renderer pattern of "try `data`, then `url`, then
 * `payload`" — pass the keys in priority order.
 */
public fun RenderViewData.stringProp(vararg keys: String): String {
    for (k in keys) {
        when (val v = props[k]) {
            is RenderPropValue.StringValue -> return v.value
            is RenderPropValue.IntValue -> return v.value.toString()
            is RenderPropValue.FloatValue -> return v.value.toString()
            is RenderPropValue.BoolValue -> return v.value.toString()
            else -> {}
        }
    }
    return ""
}

/**
 * Pull a bytes-shaped prop from a [RenderViewData]. Returns `null` if no
 * matching key holds a [RenderPropValue.BytesValue].
 */
public fun RenderViewData.bytesProp(vararg keys: String): ByteArray? {
    for (k in keys) {
        val v = props[k]
        if (v is RenderPropValue.BytesValue) return v.value
    }
    return null
}
