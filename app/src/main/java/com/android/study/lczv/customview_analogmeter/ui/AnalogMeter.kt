package com.android.study.lczv.customview_analogmeter.ui

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import com.android.study.lczv.customview_analogmeter.R
import java.util.*


class AnalogMeter : View {

    private var bgColor = Color.WHITE
    private var viewWidth = 0
    private var viewHeight = 0

    private var needleColor = Color.BLACK
    private var needleTickness = 1.0f
    private var needleLength = 100f
    private var needleStartPoint: PointF = PointF()
    private var needleEndPoint: PointF = PointF()
    private var needleCurrentValue = 0f
    private var needleAngle = 270f

    private var interval = 1
    private var intervalSubdivisions = 2
    private var intervalsColor = Color.BLACK

    private var minValue = 0
    private var maxValue = 9
    private var valuesColor = Color.BLACK
    private var valuesRadius = 400f

    private var startAngle = 180

    // Smoothness
    private var needleValueChangeSmoothness = 4
    private var needleNewValue = needleCurrentValue

    // Interval (in degrees) in which the needle will vibrate
    private var needleVibrationInterval = 2

    /* Rate in milliseconds, in which the the view will redraw itself.
    Lower values cause the animations to be run more smoothly, at possible cost of performance*/
    private var needleUpdateRate = 5L

    val timer: Timer

    val paint = Paint()

    val rnd = Random()

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                postInvalidate()
            }
        }, 0, needleUpdateRate)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0) {
        init(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0, 0) {
        init(attrs)
    }

    constructor(context: Context?) : this(context, null, 0, 0) {
        init(null)
    }

    fun init(set: AttributeSet?) {

        // Init drawables config
        val array: TypedArray = context.obtainStyledAttributes(set, R.styleable.AnalogMeter)

        try {
            needleColor = array.getColor(R.styleable.AnalogMeter_needle_color, needleColor)
            interval = array.getInt(R.styleable.AnalogMeter_interval, interval)
            intervalSubdivisions = array.getInt(R.styleable.AnalogMeter_interval_subdivisions,intervalSubdivisions)
            minValue = array.getInt(R.styleable.AnalogMeter_min_value, minValue)
            maxValue = array.getInt(R.styleable.AnalogMeter_max_value, maxValue)
            needleCurrentValue = array.getFloat(R.styleable.AnalogMeter_current_value,needleCurrentValue)
            needleTickness = array.getFloat(R.styleable.AnalogMeter_needle_tickness, needleTickness)
            needleLength = array.getFloat(R.styleable.AnalogMeter_needle_length, needleLength)
            valuesColor = array.getColor(R.styleable.AnalogMeter_values_color, valuesColor)
            intervalsColor = array.getColor(R.styleable.AnalogMeter_intervals_color, intervalsColor)
            bgColor = array.getColor(R.styleable.AnalogMeter_background_color, bgColor)
        } finally {
            array.recycle()
        }

        setNeedleValue(needleCurrentValue.toInt())
        paint.flags = Paint.ANTI_ALIAS_FLAG

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        /* Resets the style of the paint object at each iteration, as
        some methods change it's properties
         */
        paint.reset()

        canvas?.drawColor(bgColor)
        drawPointer(canvas, paint)
        drawNeedleBase(canvas, paint)
        drawIntervals(canvas, paint)
        drawValues(canvas, paint)

        vibrateNeedle()

        // Makes the needle smoothly rotates toward the new value settled by the user
        if (needleAngle + needleValueChangeSmoothness < valueToAngle(needleNewValue)) {
            needleAngle += needleValueChangeSmoothness
        } else if (needleAngle - needleValueChangeSmoothness > valueToAngle(needleNewValue)) {
            needleAngle -= needleValueChangeSmoothness
        }

    }

    private fun drawPointer(canvas: Canvas?, paint: Paint?) {
        paint?.color = needleColor
        paint?.strokeWidth = needleTickness

        canvas?.drawLine(needleStartPoint.x, needleStartPoint.y, needleEndPoint.x, needleEndPoint.y, paint)
    }

    private fun vibrateNeedle() {

        var randomValue = 0

        // Generates a random turn angle interval
        if (needleVibrationInterval != 0) {
            randomValue = rnd.nextInt(needleVibrationInterval)
        }

        // Decides in which direction the needle should turn
        if (rnd.nextBoolean()) {
            needleEndPoint.x = ((Math.cos(Math.toRadians(needleAngle.toDouble() + randomValue)) * needleLength + needleStartPoint.x)).toFloat()
            needleEndPoint.y = ((Math.sin(Math.toRadians(needleAngle.toDouble() + randomValue)) * needleLength + needleStartPoint.y)).toFloat()
        } else {
            needleEndPoint.x = ((Math.cos(Math.toRadians(needleAngle.toDouble() - randomValue)) * needleLength + needleStartPoint.x)).toFloat()
            needleEndPoint.y = ((Math.sin(Math.toRadians(needleAngle.toDouble() - randomValue)) * needleLength + needleStartPoint.y)).toFloat()
        }

    }

    private fun drawValues(canvas: Canvas?, paint: Paint?) {

        paint?.color = valuesColor
        paint?.textSize = 64f
        paint?.strokeWidth = 10f

        // Position of the labels
        var offsetX = needleStartPoint.x - 20
        var offsetY = 520

        var valueX = 0f
        var valueY = 0f

        // Between [180 , 360]
        var angle = startAngle
        var angleInterval = 180

        for (i in minValue until maxValue + 1 step interval) {
            valueX = ((Math.cos(Math.toRadians(angle.toDouble())) * valuesRadius + offsetX)).toFloat()
            valueY = ((Math.sin(Math.toRadians(angle.toDouble())) * valuesRadius + offsetY)).toFloat()

            // Increase the angle of rotation in increments
            angle += (angleInterval / maxValue)

            canvas?.drawText(i.toString(), valueX, valueY, paint)
        }
    }

    private fun drawIntervals(canvas: Canvas?, paint: Paint?) {

        paint?.color = intervalsColor

        // Position of the labels
        var offsetX = needleStartPoint.x
        var offsetY = 520

        var valueXStart = 0f
        var valueYStart = 0f

        var valueXEnd = 0f
        var valueYEnd = 0f

        val majorIntervalsLength = .2f
        val minorIntervalsLength = .25f
        val intervalsDistanceFromCenter = .7f

        // Between [180 , 360]
        var angle = startAngle
        val angleInterval = 180

        for (i in minValue until (maxValue * intervalSubdivisions) + 1 step interval) {
            valueXStart = ((Math.cos(Math.toRadians(angle.toDouble())) * needleLength * intervalsDistanceFromCenter + offsetX)).toFloat()
            valueYStart = ((Math.sin(Math.toRadians(angle.toDouble())) * needleLength * intervalsDistanceFromCenter + offsetY)).toFloat()

            // Draw the major intervals
            if (i % intervalSubdivisions == 0) {
                paint?.strokeWidth = 10f
                valueXEnd = ((Math.cos(Math.toRadians(angle.toDouble())) * needleLength * majorIntervalsLength + valueXStart)).toFloat()
                valueYEnd = ((Math.sin(Math.toRadians(angle.toDouble())) * needleLength * majorIntervalsLength + valueYStart)).toFloat()
            }
            // Draw the minor intervals
            else {
                paint?.strokeWidth = 5f
                valueXEnd = ((Math.cos(Math.toRadians(angle.toDouble())) * needleLength * minorIntervalsLength + valueXStart)).toFloat()
                valueYEnd = ((Math.sin(Math.toRadians(angle.toDouble())) * needleLength * minorIntervalsLength + valueYStart)).toFloat()
            }


            canvas?.drawLine(valueXStart, valueYStart, valueXEnd, valueYEnd, paint)

            // Increase the angle of rotation in increments
            angle += ((angleInterval / maxValue) / intervalSubdivisions)

        }
    }

    private fun drawNeedleBase(canvas: Canvas?, paint: Paint?) {
        paint?.color = needleColor
        canvas?.drawOval(needleStartPoint.x - 30, needleStartPoint.y - 30, needleStartPoint.x + 30, needleStartPoint.y + 30, paint)
    }

    fun drawReflection(canvas: Canvas?, paint: Paint?) {
        paint?.color = Color.LTGRAY
        paint?.alpha = 32
        paint?.style = Paint.Style.FILL
        canvas?.drawOval(0f, needleStartPoint.y - 200, viewWidth.toFloat(), needleStartPoint.y + 200, paint)
    }

    fun drawLines(canvas: Canvas?, paint: Paint?) {
        paint?.color = valuesColor
        paint?.alpha = 128
        paint?.style = Paint.Style.STROKE

    }

    fun setNeedleValue(value: Int) {
        needleNewValue = value.toFloat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        // Recalculates the origin of the needle
        needleStartPoint.x = (viewWidth / 2).toFloat()
        needleStartPoint.y = viewHeight.toFloat()


        setMeasuredDimension(viewWidth, viewHeight)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun valueToAngle(value: Float): Float {
        return startAngle + ((startAngle / maxValue) * value)
    }

}