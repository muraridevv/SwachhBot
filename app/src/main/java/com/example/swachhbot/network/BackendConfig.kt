package com.example.swachhbot.network

import android.content.Context
import java.util.UUID

/** Where the SwachhBot backend lives.
 *  - Emulator: 10.0.2.2 maps to the host machine.
 *  - Physical device / Raspberry Pi: use the server's LAN address.
 */
object BackendConfig {
    const val BASE_URL = "http://10.0.2.2:8080/"
}

/**
 * Stable identity of the house this installation is bound to.
 *
 * The Android app works offline against its local Room database using the
 * constant demo house id, but the backend keys everything by UUID. This helper
 * generates one UUID per install and reuses it forever, so the two stay linked.
 */
object HouseIdentity {

    private const val PREFS = "swachhbot_prefs"
    private const val KEY_HOUSE_UUID = "house_uuid"

    /** House id used by the local Room database. */
    const val LOCAL_HOUSE_ID = "DEMO_HOUSE_01"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_HOUSE_UUID, null)?.let { return it }
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_HOUSE_UUID, generated).apply()
        return generated
    }
}
