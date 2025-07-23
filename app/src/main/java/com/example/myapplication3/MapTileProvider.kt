package com.example.myapplication3

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Log
import kotlin.math.pow

class MapTileProvider(private val assetManager: AssetManager) {

    fun getTile(zoomLevel: Int, translateX: Float, translateY: Float, viewWidth: Int, viewHeight: Int, scaleFactor: Float): Bitmap? {
        // Validate inputs
        if (zoomLevel < 0 || viewWidth <= 0 || viewHeight <= 0 || scaleFactor <= 0f) {
            Log.e("MapTileProvider", "Invalid input: zoomLevel=$zoomLevel, viewWidth=$viewWidth, viewHeight=$viewHeight, scaleFactor=$scaleFactor")
            return null
        }

        val tileSize = 256
        val tilesPerSide = 2.0.pow(zoomLevel.toDouble()).toInt()

        // Create composite bitmap
        val compositeBitmap = try {
            Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        } catch (e: IllegalArgumentException) {
            Log.e("MapTileProvider", "Failed to create composite bitmap: ${e.message}")
            return null
        }
        val canvas = Canvas(compositeBitmap)

        // Calculate visible tile range
        val tileXStart = ((-translateX / tileSize).toInt() - 1).coerceIn(0, tilesPerSide - 1)
        val tileYStart = ((-translateY / tileSize).toInt() - 1).coerceIn(0, tilesPerSide - 1)
        val tileXEnd = (((-translateX + viewWidth) / tileSize).toInt() + 1).coerceIn(0, tilesPerSide - 1)
        val tileYEnd = (((-translateY + viewHeight) / tileSize).toInt() + 1).coerceIn(0, tilesPerSide - 1)

        Log.d("MapTileProvider", "Zoom=$zoomLevel, Tile range: X=[$tileXStart,$tileXEnd], Y=[$tileYStart,$tileYEnd], Scale=$scaleFactor")

        // Handle zoom level 0
        if (zoomLevel == 0) {
            val path = "world_map/0/0/0.png"
            try {
                assetManager.open(path).use { inputStream ->
                    val tileBitmap = BitmapFactory.decodeStream(inputStream)
                    canvas.drawBitmap(tileBitmap, 0f, 0f, null)
                    tileBitmap.recycle()
                    return compositeBitmap
                }
            } catch (e: Exception) {
                Log.e("MapTileProvider", "Failed to load tile at $path: ${e.message}")
                compositeBitmap.recycle()
                return null
            }
        }

        // Load and draw tiles for higher zoom levels
        var anyTileLoaded = false
        for (tileX in tileXStart..tileXEnd) {
            for (tileY in tileYStart..tileYEnd) {
                val path = "world_map/$zoomLevel/$tileX/$tileY.png"
                try {
                    assetManager.open(path).use { inputStream ->
                        val tileBitmap = BitmapFactory.decodeStream(inputStream)
                        val left = (tileX * tileSize + translateX).toFloat()
                        val top = (tileY * tileSize + translateY).toFloat()
                        Log.d("MapTileProvider", "Drawing tile $path at ($left, $top)")
                        canvas.drawBitmap(tileBitmap, left, top, null)
                        tileBitmap.recycle()
                        anyTileLoaded = true
                    }
                } catch (e: Exception) {
                    Log.e("MapTileProvider", "Failed to load tile at $path: ${e.message}")
                }
            }
        }

        return if (anyTileLoaded) compositeBitmap else {
            Log.w("MapTileProvider", "No tiles loaded for zoom=$zoomLevel")
            compositeBitmap.recycle()
            null
        }
    }
}