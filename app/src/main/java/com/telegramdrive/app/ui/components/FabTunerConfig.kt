package com.telegramdrive.app.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * ==============================================================================
 * 🛠️ NAVIGATION FAB LIVE TUNER CONFIGURATION
 * ==============================================================================
 * To completely DISABLE and hide the Live Tuner in production / release:
 *   -> Set [ENABLE_FAB_TUNER] = false
 *
 * To ENABLE the Live Tuner for debugging & optical tweaking:
 *   -> Set [ENABLE_FAB_TUNER] = true
 *   -> Long-press the Navigation FAB in the app to open the live tuning panel!
 * ==============================================================================
 */
object FabTunerConfig {
    /**
     * Master switch. Set to `false` to completely disable the long-press gesture
     * and compile-out the tuner sheet in the UI.
     */
    const val ENABLE_FAB_TUNER: Boolean = false
}

data class FabTuningParams(
    val refractionAmount: Float = 64f,       // Optical Z-distance displacement (dp)
    val refractionHeight: Float = 28f,       // Lens dome curvature zone (dp, 28 = full dome)
    val depthEffect: Boolean = true,         // 3D spherical surface normal refraction
    val chromaticAberration: Float = 0.10f,  // Optical prism RGB dispersion (0.0 to 0.35)
    val blurRadius: Float = 8f,              // Glass blur radius (dp)
    val elevation: Float = 22f,              // Physical elevation drop shadow (dp)
    val fabSize: Float = 56f,                // Button diameter (dp)
    val backingTintAlpha: Float = 0.0f,      // Translucent dark backing tint (0.0 to 0.8)
    val saturation: Float = 1.5f             // Color saturation boost (1.0 to 2.5)
) {
    companion object {
        val DefaultFolders = FabTuningParams(
            refractionAmount = 64f,
            refractionHeight = 28f,
            depthEffect = true,
            chromaticAberration = 0.10f,
            blurRadius = 8f,
            elevation = 22f,
            fabSize = 56f,
            backingTintAlpha = 0.0f,
            saturation = 1.5f
        )
        
        val DefaultStandard = FabTuningParams(
            refractionAmount = 32f,
            refractionHeight = 16f,
            depthEffect = false,
            chromaticAberration = 0.0f,
            blurRadius = 8f,
            elevation = 12f,
            fabSize = 56f,
            backingTintAlpha = 0.0f,
            saturation = 1.5f
        )
    }
}

class FabTunerStorage(context: Context) {
    private val prefs = context.getSharedPreferences("fab_tuner_live_prefs", Context.MODE_PRIVATE)

    fun loadParams(isFoldersTab: Boolean): FabTuningParams {
        val prefix = if (isFoldersTab) "folders_" else "std_"
        val default = if (isFoldersTab) FabTuningParams.DefaultFolders else FabTuningParams.DefaultStandard

        if (!prefs.contains("${prefix}refractionAmount")) {
            return default
        }

        return FabTuningParams(
            refractionAmount = prefs.getFloat("${prefix}refractionAmount", default.refractionAmount),
            refractionHeight = prefs.getFloat("${prefix}refractionHeight", default.refractionHeight),
            depthEffect = prefs.getBoolean("${prefix}depthEffect", default.depthEffect),
            chromaticAberration = prefs.getFloat("${prefix}chromaticAberration", default.chromaticAberration),
            blurRadius = prefs.getFloat("${prefix}blurRadius", default.blurRadius),
            elevation = prefs.getFloat("${prefix}elevation", default.elevation),
            fabSize = prefs.getFloat("${prefix}fabSize", default.fabSize),
            backingTintAlpha = prefs.getFloat("${prefix}backingTintAlpha", default.backingTintAlpha),
            saturation = prefs.getFloat("${prefix}saturation", default.saturation)
        )
    }

    fun saveParams(params: FabTuningParams, isFoldersTab: Boolean) {
        val prefix = if (isFoldersTab) "folders_" else "std_"
        prefs.edit().apply {
            putFloat("${prefix}refractionAmount", params.refractionAmount)
            putFloat("${prefix}refractionHeight", params.refractionHeight)
            putBoolean("${prefix}depthEffect", params.depthEffect)
            putFloat("${prefix}chromaticAberration", params.chromaticAberration)
            putFloat("${prefix}blurRadius", params.blurRadius)
            putFloat("${prefix}elevation", params.elevation)
            putFloat("${prefix}fabSize", params.fabSize)
            putFloat("${prefix}backingTintAlpha", params.backingTintAlpha)
            putFloat("${prefix}saturation", params.saturation)
            apply()
        }
    }

    fun resetParams(isFoldersTab: Boolean): FabTuningParams {
        val prefix = if (isFoldersTab) "folders_" else "std_"
        prefs.edit().apply {
            remove("${prefix}refractionAmount")
            remove("${prefix}refractionHeight")
            remove("${prefix}depthEffect")
            remove("${prefix}chromaticAberration")
            remove("${prefix}blurRadius")
            remove("${prefix}elevation")
            remove("${prefix}fabSize")
            remove("${prefix}backingTintAlpha")
            remove("${prefix}saturation")
            apply()
        }
        return if (isFoldersTab) FabTuningParams.DefaultFolders else FabTuningParams.DefaultStandard
    }
}
