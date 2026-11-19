package com.rahulbaberwal.blacktile

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Represents a single privacy mask tile overlay.
 * Coordinates (x, y) and dimensions (w, h) are stored in pixels.
 */
data class Tile(
    val id: String,
    var x: Int,
    var y: Int,
    var w: Int,
    var h: Int
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("x", x)
        obj.put("y", y)
        obj.put("w", w)
        obj.put("h", h)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): Tile {
            return Tile(
                id = obj.optString("id", UUID.randomUUID().toString()),
                x = obj.optInt("x", 100),
                y = obj.optInt("y", 200),
                w = obj.optInt("w", 500),
                h = obj.optInt("h", 350)
            )
        }
    }
}

/**
 * Manages persistence for tiles, editing opacity, locked opacity, lock mode,
 * and visibility using standard Android SharedPreferences and JSON.
 */
object MaskPrefs {
    private const val PREFS_NAME = "black_tile_prefs"
    private const val KEY_TILES = "tiles_json"
    private const val KEY_OPACITY = "opacity"
    private const val KEY_LOCKED_OPACITY = "locked_opacity"
    private const val KEY_OVERLAY_STYLE = "overlay_style"
    private const val KEY_LOCKED = "locked"
    private const val KEY_VISIBLE = "visible"

    const val DEFAULT_EDIT_OPACITY = 100
    const val DEFAULT_LOCKED_OPACITY = 100
    const val MIN_OPACITY = 20
    const val MAX_OPACITY = 100

    const val STYLE_PURE_BLACK = "pure_black"
    const val STYLE_MATTE_OBSIDIAN = "matte_obsidian"
    const val STYLE_DARK_NAVY = "dark_navy"
    const val STYLE_DARK_SLATE = "dark_slate"
    const val STYLE_CARBON_WEAVE = "carbon_weave"
    const val STYLE_DOT_MATRIX = "dot_matrix"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun loadTiles(context: Context): MutableList<Tile> {
        val list = mutableListOf<Tile>()
        val jsonStr = getPrefs(context).getString(KEY_TILES, null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(Tile.fromJson(obj))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    fun saveTiles(context: Context, tiles: List<Tile>) {
        val array = JSONArray()
        for (tile in tiles) {
            array.put(tile.toJson())
        }
        getPrefs(context).edit().putString(KEY_TILES, array.toString()).apply()
    }

    fun clearAllTiles(context: Context) {
        getPrefs(context).edit().remove(KEY_TILES).apply()
    }

    fun getOpacity(context: Context): Int {
        return getPrefs(context).getInt(KEY_OPACITY, DEFAULT_EDIT_OPACITY)
            .coerceIn(MIN_OPACITY, MAX_OPACITY)
    }

    fun setOpacity(context: Context, opacity: Int) {
        val clamped = opacity.coerceIn(MIN_OPACITY, MAX_OPACITY)
        getPrefs(context).edit()
            .putInt(KEY_OPACITY, clamped)
            .putInt(KEY_LOCKED_OPACITY, clamped)
            .apply()
    }

    fun getLockedOpacity(context: Context): Int {
        // Always return the primary tile opacity so locked tiles never change opacity unexpectedly
        return getOpacity(context)
    }

    fun setLockedOpacity(context: Context, opacity: Int) {
        setOpacity(context, opacity)
    }

    fun isLocked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LOCKED, false)
    }

    fun setLocked(context: Context, locked: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LOCKED, locked).apply()
    }

    fun isVisible(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VISIBLE, true)
    }

    fun setVisible(context: Context, visible: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_VISIBLE, visible).apply()
    }

    fun getOverlayStyle(context: Context): String {
        return getPrefs(context).getString(KEY_OVERLAY_STYLE, STYLE_PURE_BLACK) ?: STYLE_PURE_BLACK
    }

    fun setOverlayStyle(context: Context, style: String) {
        getPrefs(context).edit().putString(KEY_OVERLAY_STYLE, style).apply()
    }

    fun resetToDefaults(context: Context) {
        getPrefs(context).edit()
            .putInt(KEY_OPACITY, DEFAULT_EDIT_OPACITY)
            .putInt(KEY_LOCKED_OPACITY, DEFAULT_LOCKED_OPACITY)
            .putString(KEY_OVERLAY_STYLE, STYLE_PURE_BLACK)
            .putBoolean(KEY_LOCKED, false)
            .putBoolean(KEY_VISIBLE, true)
            .apply()
    }

    fun createDefaultTile(context: Context, displayWidth: Int, displayHeight: Int): Tile {
        val density = context.resources.displayMetrics.density
        val defaultW = (240 * density).toInt().coerceAtMost(displayWidth - 40)
        val defaultH = (160 * density).toInt().coerceAtMost(displayHeight - 100)
        val initialX = ((displayWidth - defaultW) / 2).coerceAtLeast(0)
        val initialY = ((displayHeight - defaultH) / 3).coerceAtLeast(100)

        return Tile(
            id = UUID.randomUUID().toString(),
            x = initialX,
            y = initialY,
            w = defaultW.coerceAtLeast((80 * density).toInt()),
            h = defaultH.coerceAtLeast((80 * density).toInt())
        )
    }
}
