package com.example.draw

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt

class DrawingView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var mDrawPaths = ArrayList<CustomPath>() // path
    private var mDrawPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null // style and color
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0f
    private var color = Color.BLACK
    private var canvas: Canvas? = null

    init {
        setUpDrawing()
    }

    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPaint!!.color = color
        mDrawPath = CustomPath(0, 0f)
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint()
        canvas = Canvas()
    }

    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path() {
    }

    // is called when size of view is changed
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // create bitmap
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas!!.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
    }

    // for drawing (painting)
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // draw bitmap

        for(path in mDrawPaths) {
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            // draw path
            canvas!!.drawPath(path!!, mDrawPaint!!)
        }

        if(mDrawPath != null) {
            mDrawPaint!!.strokeWidth = mBrushSize
            mDrawPaint!!.color = color
            // draw path
            canvas!!.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

    // for pathing
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event!!.x
        val touchY = event!!.y
        when(event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize
                mDrawPath!!.moveTo(touchX, touchY)
                mDrawPath!!.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE -> {
                mDrawPath!!.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                mDrawPaths.add(mDrawPath!!)
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setBrushSize(size: Float) {
        val brushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                size, resources.displayMetrics)
        mBrushSize = brushSize
    }

    fun setColor(color: Int) {
        this.color = color
    }

    fun undo() {
        if(mDrawPaths.size > 0) {
            mDrawPaths.removeLast()
            invalidate()
        }
    }

}