package de.belkis.spyhound.testspy

import android.app.Activity
import android.os.Bundle

/** Harmless empty activity for end-to-end Spyhound debug indicator tests. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
