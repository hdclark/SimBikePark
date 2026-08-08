package com.hdclark.simbikepark

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import kotlin.math.floor
import kotlin.math.sin

private val grass = Color(0xFF54C96B)
private val dirt = Color(0xFFE8893C)
private val dirtSide = Color(0xFF9D572D)
private val wood = Color(0xFFC98345)
private val darkWood = Color(0xFF784321)
private val rock = Color(0xFF8C96A0)
private val water = Color(0xFF4DB5E8)
private val ink = Color(0xFF173B2B)

@Composable
fun IsometricPark(pieces: List<TrailPiece>, plans: List<SimulationPlan>, elapsed: Float, running: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val tileW = (size.width / 4.6f).coerceIn(90f, 170f)
        val tileH = tileW * .48f
        val starts = TrailMath.starts(pieces)
        val projected = starts.map { project(it.x.toFloat(), it.y.toFloat(), tileW, tileH) }
        val centerShift = if (projected.isEmpty()) Offset(size.width / 2f, size.height / 2f)
        else {
            val minX = projected.minOf { it.x }; val maxX = projected.maxOf { it.x }
            val minY = projected.minOf { it.y }; val maxY = projected.maxOf { it.y }
            Offset(size.width / 2f - (minX + maxX) / 2f, size.height / 2f - (minY + maxY) / 2f)
        }

        drawBackdrop(tileW, tileH, centerShift)
        starts.zip(pieces).forEachIndexed { index, (point, piece) ->
            val p = project(point.x.toFloat(), point.y.toFloat(), tileW, tileH) + centerShift
            drawFeature(p, tileW, tileH, piece.type, elapsed + index * .17f)
        }
        if (pieces.isNotEmpty()) {
            val last = starts.last()
            val dir = pieces.last().heading.mod(4)
            val end = when (dir) { 0 -> GridPoint(last.x + 1, last.y); 1 -> GridPoint(last.x, last.y + 1); 2 -> GridPoint(last.x - 1, last.y); else -> GridPoint(last.x, last.y - 1) }
            drawFinish(project(end.x.toFloat(), end.y.toFloat(), tileW, tileH) + centerShift, tileW, tileH)
        }
        if (running) drawRiders(plans, pieces, starts, elapsed, tileW, tileH, centerShift)
    }
}

private fun DrawScope.drawBackdrop(w: Float, h: Float, shift: Offset) {
    for (x in -5..5) for (y in -5..5) {
        val p = project(x.toFloat(), y.toFloat(), w, h) + shift
        diamond(p, w, h, if ((x + y) % 2 == 0) grass else Color(0xFF61D178))
    }
}

private fun project(x: Float, y: Float, w: Float, h: Float) = Offset((x - y) * w / 2f, (x + y) * h / 2f)

private fun DrawScope.diamond(c: Offset, w: Float, h: Float, color: Color) {
    val p = Path().apply { moveTo(c.x, c.y - h/2); lineTo(c.x+w/2,c.y); lineTo(c.x,c.y+h/2); lineTo(c.x-w/2,c.y); close() }
    drawPath(p, color)
}

private fun DrawScope.block(c: Offset, w: Float, h: Float, height: Float, top: Color, side: Color) {
    val topC = c - Offset(0f, height)
    diamond(topC, w, h, top)
    val right = Path().apply { moveTo(topC.x+w/2,topC.y); lineTo(c.x+w/2,c.y); lineTo(c.x,c.y+h/2); lineTo(topC.x,topC.y+h/2); close() }
    val left = Path().apply { moveTo(topC.x-w/2,topC.y); lineTo(c.x-w/2,c.y); lineTo(c.x,c.y+h/2); lineTo(topC.x,topC.y+h/2); close() }
    drawPath(right, side); drawPath(left, side.copy(alpha=.82f)); diamond(topC,w,h,top)
}

private fun DrawScope.drawFeature(c: Offset, w: Float, h: Float, type: FeatureType, t: Float) {
    block(c, w*.92f, h*.92f, 9f, dirt, dirtSide)
    val top = c - Offset(0f,9f)
    when(type) {
        FeatureType.TABLE_TOP -> block(top, w*.65f,h*.65f,18f,Color(0xFFF2A34D),dirtSide)
        FeatureType.GAP_JUMP -> { block(top-Offset(w*.22f,0f),w*.28f,h*.58f,20f,dirt,dirtSide); block(top+Offset(w*.22f,0f),w*.28f,h*.58f,14f,dirt,dirtSide); drawCircle(ink.copy(alpha=.45f),w*.09f,top) }
        FeatureType.DIRT_JUMP -> { val p=Path().apply{moveTo(top.x-w*.28f,top.y+h*.12f);lineTo(top.x,top.y-h*.28f);lineTo(top.x+w*.28f,top.y+h*.12f);close()};drawPath(p,Color(0xFFF2A34D)) }
        FeatureType.WOODEN_SKINNY -> block(top,w*.72f,h*.18f,15f,wood,darkWood)
        FeatureType.LAUNCHER_LOG -> { block(top,w*.58f,h*.48f,25f,dirt,dirtSide); drawCircle(darkWood,w*.10f,top+Offset(w*.18f,-18f)) }
        FeatureType.DROP -> block(top-Offset(0f,8f),w*.70f,h*.70f,32f,Color(0xFFF6B05C),dirtSide)
        FeatureType.TEETER_TOTTER -> { val tilt=sin(t*2f)*10f; drawLine(darkWood,top-Offset(w*.33f,tilt),top+Offset(w*.33f,tilt),strokeWidth=12f); drawCircle(ink,w*.055f,top) }
        FeatureType.SALOON_DOORS -> { drawLine(darkWood,top-Offset(w*.28f,32f),top-Offset(w*.28f,-12f),8f); drawLine(darkWood,top+Offset(w*.28f,-32f),top+Offset(w*.28f,12f),8f); emoji("🚪",top-Offset(0f,18f),w*.25f) }
        FeatureType.TOILET_BOWL -> { drawOval(Color(0xFFF3F3E8), topLeft = top-Offset(w*.27f,h*.22f), size = Size(w*.54f,h*.44f)); drawOval(water, topLeft = top-Offset(w*.17f,h*.13f), size = Size(w*.34f,h*.26f)) }
        FeatureType.ROCK_ROLL -> { drawCircle(rock,w*.19f,top-Offset(w*.08f,8f)); drawCircle(rock.copy(.9f),w*.15f,top+Offset(w*.16f,3f)) }
        FeatureType.CROCODILE_PIT -> { diamond(top,w*.72f,h*.65f,water); emoji("🐊",top,w*.30f) }
        FeatureType.ROCKS -> listOf(Offset(-.22f,-.04f),Offset(.08f,-.13f),Offset(.25f,.08f)).forEachIndexed{i,o->drawCircle(rock.copy(alpha=.86f+i*.05f),w*(.07f+i*.015f),top+Offset(o.x*w,o.y*h))}
        FeatureType.ROOTS -> for(i in -2..2) drawLine(darkWood,top+Offset(-w*.3f,i*h*.07f),top+Offset(w*.3f,(i+1)*h*.06f),5f)
        FeatureType.BERM -> drawArc(Color(0xFFFFC15C),195f,150f,false, topLeft = top-Offset(w*.30f,h*.42f), size = Size(w*.60f,h*.84f), style=Stroke(18f))
    }
}

private fun DrawScope.drawFinish(c: Offset,w:Float,h:Float){ block(c,w*.52f,h*.52f,5f,Color(0xFFE7E7E7),Color(0xFF777777)); emoji("🏁",c-Offset(0f,28f),w*.26f) }

private fun DrawScope.drawRiders(plans: List<SimulationPlan>, pieces: List<TrailPiece>, starts: List<GridPoint>, elapsed: Float, w: Float, h: Float, shift: Offset) {
    if (pieces.isEmpty()) return
    plans.forEachIndexed { idx, plan ->
        val raw = ((elapsed - idx*.10f).coerceAtLeast(0f) * plan.rider.speed * .58f)
        val crashPos = plan.crashPiece?.let { it + .55f }
        val p = if (crashPos != null) minOf(raw, crashPos) else minOf(raw, pieces.size + .6f)
        val pieceIndex = floor(p).toInt().coerceIn(0, pieces.lastIndex)
        val local = (p-pieceIndex).coerceIn(0f,1f)
        val a=starts[pieceIndex]; val heading=pieces[pieceIndex].heading.mod(4)
        val dx=when(heading){0->1f;2->-1f;else->0f}; val dy=when(heading){1->1f;3->-1f;else->0f}
        val pos=project(a.x+dx*local,a.y+dy*local,w,h)+shift+Offset(0f,-24f-plan.laneOffset*20f)
        val crashed = crashPos != null && raw >= crashPos
        if (crashed) {
            val bounce = sin((elapsed + idx)*7f)*18f
            emoji("💥",pos+Offset(0f,bounce-10f),w*.25f)
            emoji("🚲",pos+Offset(22f,-bounce*.4f),w*.19f)
            emoji(plan.rider.emoji,pos+Offset(-18f,bounce*.5f),w*.18f)
        } else {
            emoji("🚲",pos+Offset(0f,8f),w*.18f); emoji(plan.rider.emoji,pos-Offset(0f,13f),w*.16f)
        }
    }
}

private fun DrawScope.emoji(text:String,at:Offset,sizePx:Float){
    drawContext.canvas.nativeCanvas.apply{
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{ textSize=sizePx; textAlign=Paint.Align.CENTER; color=Color.Black.toArgb() }
        drawText(text,at.x,at.y,p)
    }
}
