package com.reaxkey.ime

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.widget.Button
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/** Owns one space gesture. It never edits text; cursor movement is delegated to the editor. */
internal class SpaceCursorGesture(
    private val button: Button,
    private val handler: Handler,
    private val density: Float,
    private val connection: () -> InputConnection?,
    private val tapSpace: () -> Unit,
    private val haptics: () -> Boolean,
    private val onActivated: () -> Unit,
    private val onRelease: () -> Unit
) : View.OnTouchListener {
    private enum class Phase { IDLE, PRESSED, CURSOR }
    private var phase = Phase.IDLE
    private var originX = 0f
    private var originY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var lastTime = 0L
    private var accumulatedX = 0f
    private var accumulatedY = 0f
    private val normalBackground = button.background
    private val normalText = button.text
    private val bubble = CursorBubbleDrawable()
    private val mover = CursorMover(connection)
    val isInteracting get() = phase != Phase.IDLE
    private val hold = Runnable {
        if(phase == Phase.PRESSED && connection() != null) {
            phase = Phase.CURSOR
            onActivated()
            button.text = ""
            button.contentDescription = button.context.getString(com.reaxkey.ui.R.string.cursor_mode)
            button.background = bubble
            bubble.activate()
            if(haptics()) button.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancel(false)
                phase = Phase.PRESSED
                originX = event.x; originY = event.y; lastX = event.x; lastY = event.y
                lastTime = SystemClock.uptimeMillis()
                handler.postDelayed(hold, 330)
            }
            MotionEvent.ACTION_MOVE -> {
                if(phase == Phase.PRESSED && hypot(event.x-originX,event.y-originY) > 12f*density) handler.removeCallbacks(hold)
                if(phase == Phase.CURSOR) {
                    if(connection() == null) { cancel(); return true }
                    val now = SystemClock.uptimeMillis()
                    val dx = event.x-lastX; val dy = event.y-lastY
                    val speed = hypot(dx,dy)/((now-lastTime).coerceAtLeast(1))
                    val step = (if(speed > 1.3f) 8f else 11f)*density
                    accumulatedX += dx; accumulatedY += dy
                    val stepsX = (accumulatedX/step).toInt().coerceIn(-5,5)
                    val stepsY = (accumulatedY/step).toInt().coerceIn(-5,5)
                    if(stepsX != 0 || stepsY != 0) {
                        val moved=try {mover.move(stepsX,stepsY)} catch (_:RuntimeException) {false}
                        if(!moved) {cancel();return true}
                        accumulatedX -= stepsX*step; accumulatedY -= stepsY*step
                    }
                    bubble.setDisplacement((event.x-originX)/(button.width.coerceAtLeast(1)),
                        (event.y-originY)/(button.height.coerceAtLeast(1)))
                    lastTime = now
                }
                lastX=event.x;lastY=event.y
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(hold)
                if(phase == Phase.PRESSED) tapSpace()
                cancel(phase == Phase.CURSOR)
                onRelease()
            }
            MotionEvent.ACTION_CANCEL -> {cancel(phase == Phase.CURSOR);onRelease()}
        }
        return true
    }

    fun cancel(animate: Boolean = false) {
        handler.removeCallbacks(hold)
        if(phase == Phase.CURSOR) {
            if(animate && button.isAttachedToWindow) bubble.settle { if(phase==Phase.IDLE) restore() } else restore()
        } else if(button.background===bubble) {
            bubble.stop();restore()
        }
        phase = Phase.IDLE
        accumulatedX=0f;accumulatedY=0f;mover.reset()
    }
    private fun restore() {
        button.background=normalBackground
        button.text=normalText
        button.contentDescription=normalText
    }
}

/** The host editor controls wrapped-line geometry. Newline fallback is used only when native up/down did not move. */
internal class CursorMover(private val connection: () -> InputConnection?) {
    private var desiredColumn: Int? = null
    fun reset() { desiredColumn=null }
    fun move(horizontal: Int, vertical: Int): Boolean {
        val ic=connection() ?: return false
        if(horizontal != 0) desiredColumn=null
        repeat(abs(horizontal)) { if(!send(ic,if(horizontal < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT)) moveHorizontalFallback(ic,horizontal < 0) }
        repeat(abs(vertical)) { moveVertical(ic,vertical < 0) }
        return true
    }
    private fun send(ic: InputConnection, key: Int): Boolean {
        val now=SystemClock.uptimeMillis()
        val down=ic.sendKeyEvent(KeyEvent(now,now,KeyEvent.ACTION_DOWN,key,0))
        val up=ic.sendKeyEvent(KeyEvent(now,now,KeyEvent.ACTION_UP,key,0))
        return down || up
    }
    private fun moveHorizontalFallback(ic: InputConnection, left: Boolean) {
        val selection=ic.getExtractedText(ExtractedTextRequest().apply {hintMaxChars=2048;hintMaxLines=40},0) ?: return
        val content=selection.text?.toString() ?: return
        val position=(selection.selectionEnd-selection.startOffset).coerceIn(0,content.length)
        val next=if(left && position>0) Character.offsetByCodePoints(content,position,-1)
            else if(!left && position<content.length) Character.offsetByCodePoints(content,position,1) else position
        ic.setSelection(selection.startOffset+next,selection.startOffset+next)
    }
    private fun moveVertical(ic: InputConnection, up: Boolean) {
        val request=ExtractedTextRequest().apply {hintMaxChars=2048;hintMaxLines=40}
        val before=ic.getExtractedText(request,0)
        send(ic,if(up) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN)
        val after=ic.getExtractedText(request,0)
        if(before == null || after == null || before.selectionStart != after.selectionStart || before.selectionEnd != after.selectionEnd) return
        val text=before.text?.toString() ?: return
        val position=(before.selectionStart-before.startOffset).coerceIn(0,text.length)
        val start=text.lastIndexOf('\n',position-1)+1
        val column=desiredColumn ?: (position-start).also { desiredColumn=it }
        val target=if(up) {
            if(start==0) return
            val previousEnd=start-1
            val previousStart=text.lastIndexOf('\n',previousEnd-1)+1
            previousStart+min(column,previousEnd-previousStart)
        } else {
            val nextStart=text.indexOf('\n',position).takeIf { it>=0 }?.plus(1) ?: return
            val nextEnd=text.indexOf('\n',nextStart).takeIf { it>=0 } ?: text.length
            nextStart+min(column,nextEnd-nextStart)
        }
        ic.setSelection(before.startOffset+target,before.startOffset+target)
    }
}

private class CursorBubbleDrawable : Drawable() {
    private val fill=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=0xCCBEC8CB.toInt()}
    private val rim=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=0xB0FFFFFF.toInt();style=Paint.Style.STROKE;strokeWidth=2f}
    private val shine=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=0x55FFFFFF}
    private val rect=RectF()
    private var x=0f;private var y=0f
    private var morph=1f
    private var settling: ValueAnimator?=null
    fun setDisplacement(rawX: Float, rawY: Float) { x=rawX.coerceIn(-1f,1f);y=rawY.coerceIn(-1f,1f);invalidateSelf() }
    fun activate() {
        settling?.cancel();morph=0f
        settling=ValueAnimator.ofFloat(0f,1f).apply {duration=140
            addUpdateListener {morph=it.animatedValue as Float;invalidateSelf()};start()}
    }
    fun stop() {settling?.cancel();settling=null;morph=1f;setDisplacement(0f,0f)}
    fun settle(end: () -> Unit) {
        settling?.cancel()
        val fromX=x;val fromY=y;val fromMorph=morph
        settling=ValueAnimator.ofFloat(1f,0f).apply {
            duration=140
            addUpdateListener { val t=it.animatedValue as Float;morph=fromMorph*t;setDisplacement(fromX*t,fromY*t) }
            addListener(object:android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation:android.animation.Animator) {end()}
            })
            start()
        }
    }
    override fun draw(canvas: Canvas) {
        val bounds=bounds
        val diameter=min(bounds.width(),bounds.height())*0.88f
        val radius=diameter/2f
        val dx=x*radius*0.17f;val dy=y*radius*0.17f
        val stretch=0.07f*min(1f,hypot(x,y))
        val rx=radius*(1f+stretch*abs(x))+(bounds.width()*0.44f-radius)*(1f-morph)
        val ry=radius*(1f+stretch*abs(y))
        val cx=bounds.exactCenterX()+dx;val cy=bounds.exactCenterY()+dy
        rect.set(cx-rx,cy-ry,cx+rx,cy+ry)
        canvas.drawRoundRect(rect,ry,ry,fill);canvas.drawRoundRect(rect,ry,ry,rim)
        canvas.drawArc(rect.left+rx*.20f,rect.top+ry*.12f,rect.right-rx*.20f,rect.bottom-ry*.28f,195f,105f,false,shine)
    }
    override fun setAlpha(alpha:Int) {fill.alpha=alpha;invalidateSelf()}
    override fun setColorFilter(filter:android.graphics.ColorFilter?) {fill.colorFilter=filter;invalidateSelf()}
    @Deprecated("Deprecated in Java") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
}
