package com.example.myapplication3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class MapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var tileProvider: MapTileProvider? = null
    private var scaleFactor = 1f // Relative to zoomLevel’s base scale
    private var translateX = 0f
    private var translateY = 0f
    private var currentZoomLevel = 0
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private val tileSize = 256f
    private var mapWidth = tileSize // Width of map at zoomLevel 0
    private var mapHeight = tileSize // Height of map at zoomLevel 0

    init {
        scaleType = ScaleType.FIT_CENTER
        post {
            adjustInitialPosition()
            updateMap()
        }
    }

    fun setTileProvider(provider: MapTileProvider) {
        tileProvider = provider
        updateMap()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        adjustInitialPosition()
        updateMap()
    }

    private fun adjustInitialPosition() {
        if (width > 0 && height > 0) {
            // Center the map at zoomLevel 0
            mapWidth = tileSize * 2.0.pow(currentZoomLevel.toDouble()).toFloat()
            mapHeight = tileSize * 2.0.pow(currentZoomLevel.toDouble()).toFloat()
            translateX = (width - mapWidth * scaleFactor) / 2f
            translateY = (height - mapHeight * scaleFactor) / 2f
            Log.d("MapView", "Initial position: translateX=$translateX, translateY=$translateY, scaleFactor=$scaleFactor")
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateMap()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    translateX += dx
                    translateY += dy
                    constrainTranslation()
                    lastTouchX = event.x
                    lastTouchY = event.y
                    updateMap()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return true
    }

    private fun constrainTranslation() {
        // Calculate map size at current zoom level
        mapWidth = tileSize * 2.0.pow(currentZoomLevel.toDouble()).toFloat()
        mapHeight = tileSize * 2.0.pow(currentZoomLevel.toDouble()).toFloat()
        val scaledWidth = mapWidth * scaleFactor
        val scaledHeight = mapHeight * scaleFactor
        // Constrain translation to keep map within view
        val minX = (width - scaledWidth).coerceAtLeast(0f)
        val minY = (height - scaledHeight).coerceAtLeast(0f)
        translateX = minX.coerceAtLeast(translateX.coerceAtMost(0f))
        translateY = minY.coerceAtLeast(translateY.coerceAtMost(0f))
        Log.d("MapView", "Constrained: translateX=$translateX, translateY=$translateY, mapSize=($scaledWidth,$scaledHeight)")
    }

    private fun updateMap() {

        val newZoomLevel = calculateZoomLevel()
        if (newZoomLevel != currentZoomLevel) {
            val previousScale = scaleFactor
            currentZoomLevel = newZoomLevel
            // Adjust scaleFactor to align with zoomLevel
            scaleFactor = 2.0.pow(currentZoomLevel.toDouble()).toFloat()
            // Adjust translation to zoom around center
            val focusX = width / 2f
            val focusY = height / 2f
            translateX = focusX - (focusX - translateX) * (scaleFactor / previousScale)
            translateY = focusY - (focusY - translateY) * (scaleFactor / previousScale)
            constrainTranslation()
        }

        // Pass scaleFactor = 1f to MapTileProvider since scaling is handled in MapView
        val bitmap = tileProvider?.getTile(currentZoomLevel, translateX / scaleFactor, translateY / scaleFactor, width, height, 1f)
        setImageBitmap(bitmap)
        Log.d("MapView", "Updated: zoomLevel=$currentZoomLevel, scaleFactor=$scaleFactor, translate=($translateX,$translateY)")
    }

    private fun calculateZoomLevel(): Int {
        // Map scaleFactor to zoomLevel, assuming scaleFactor doubles per zoom level
        val zoom = (ln(scaleFactor.toDouble()) / ln(2.0)).toInt()
        return max(0, min(7, zoom)) // Max zoom level 7
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val previousScaleFactor = scaleFactor
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(1f, min(scaleFactor, 128f)) // Limit scale range
            // Adjust translation to zoom around focal point
            val focusX = detector.focusX
            val focusY = detector.focusY
            translateX = focusX - (focusX - translateX) * (scaleFactor / previousScaleFactor)
            translateY = focusY - (focusY - translateY) * (scaleFactor / previousScaleFactor)
            updateMap()
            return true
        }
    }
}