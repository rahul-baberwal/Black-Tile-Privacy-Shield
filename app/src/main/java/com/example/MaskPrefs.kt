package com.example

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
 * Manages persistence for tiles, opacity, lock mode, and visibility
 * using standard SharedPreferences and JSON.
 */
object MaskPrefs {
    private const val PREFS_NAME = "black_tile_prefs"
    private const val KEY_TILES = "tiles_json"
    private const val KEY_OPACITY = "opacity"
    private const val KEY_LOCKED = "locked"
    private const val KEY_VISIBLE = "visible"

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

    fun getOpacity(context: Context): Int {
        return getPrefs(context).getInt(KEY_OPACITY, 100).coerceIn(20, 100)
    }

    fun setOpacity(context: Context, opacity: Int) {
        val clamped = opacity.coerceIn(20, 100)
        getPrefs(context).edit().putInt(KEY_OPACITY, clamped).apply()
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
