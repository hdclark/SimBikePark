package com.hdclark.simbikepark

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

private val grass = Color(0xFF54C96B)
private val grassAlt = Color(0xFF61D178)
private val dirt = Color(0xFFE8893C)
private val trail = Color(0xFFFFB55B)
private val dirtSide = Color(0xFF9D572D)
private val wood = Color(0xFFC98345)
private val darkWood = Color(0xFF784321)
private val rock = Color(0xFF8C96A0)
private val water = Color(0xFF4DB5E8)
private val ink = Color(0xFF173B2B)

@Composable
fun IsometricPark(
    pieces: List<TrailPiece>,
    plans: List<SimulationPlan>,
    elapsed: Float,
    running: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val tileW = (size.width / 4.6f).coerceIn(90f, 170f)
        val tileH = tileW * .48f
        val placements = TrailMath.placements(pieces)
        val finishCell = TrailMath.end(pieces)
        val allCells = placements.map { it.cell } + finishCell
        val projected = allCells.map { project(it.x.toFloat(), it.y.toFloat(), tileW, tileH) }
        val centerShift = if (projected.isEmpty()) Offset(size.width / 2f, size.height / 2f) else {
            val minX = projected.minOf { it.x }
            val maxX = projected.maxOf { it.x }
            val minY = projected.minOf { it.y }
            val maxY = projected.maxOf { it.y }
            Offset(size.width / 2f - (minX + maxX) / 2f, size.height / 2f - (minY + maxY) / 2f)
        }

        val minGX = (allCells.minOfOrNull { it.x } ?: 0) - 4
        val maxGX = (allCells.maxOfOrNull { it.x } ?: 0) + 4
        val minGY = (allCells.minOfOrNull { it.y } ?: 0) - 4
        val maxGY = (allCells.maxOfOrNull { it.y } ?: 0) + 4
        drawBackdrop(tileW, tileH, centerShift, minGX, maxGX, minGY, maxGY)

        placements.withIndex()
            .sortedWith(compareBy({ it.value.cell.x + it.value.cell.y }, { it.value.cell.x }))
            .forEach { indexed ->
                val placement = indexed.value
                val piece = pieces[indexed.index]
                val center = project(placement.cell.x.toFloat(), placement.cell.y.toFloat(), tileW, tileH) + centerShift
                drawFeature(center, tileW, tileH, piece.type, placement, elapsed + indexed.index * .17f)
            }

        if (pieces.isNotEmpty()) {
            val finish = project(finishCell.x.toFloat(), finishCell.y.toFloat(), tileW, tileH) + centerShift
            drawFinish(finish, tileW, tileH, pieces.last().heading.mod(4))
        }
        if (running) drawRiders(plans, pieces, placements, elapsed, tileW, tileH, centerShift)
    }
}

private fun DrawScope.drawBackdrop(w: Float, h: Float, shift: Offset, minX: Int, maxX: Int, minY: Int, maxY: Int) {
    for (x in minX..maxX) for (y in minY..maxY) {
        val p = project(x.toFloat(), y.toFloat(), w, h) + shift
        diamond(p, w, h, if ((x + y) % 2 == 0) grass else grassAlt)
    }
}

private fun project(x: Float, y: Float, w: Float, h: Float) = Offset((x - y) * w / 2f, (x + y) * h / 2f)

private fun gridVector(heading: Int, w: Float, h: Float): Offset {
    val d = TrailMath.direction(heading)
    return project(d.x.toFloat(), d.y.toFloat(), w, h)
}

private fun scaled(v: Offset, amount: Float) = Offset(v.x * amount, v.y * amount)
private fun plus(a: Offset, b: Offset) = Offset(a.x + b.x, a.y + b.y)
private fun minus(a: Offset, b: Offset) = Offset(a.x - b.x, a.y - b.y)
private fun length(v: Offset) = sqrt(v.x * v.x + v.y * v.y)
private fun unit(v: Offset): Offset {
    val l = length(v).coerceAtLeast(.001f)
    return Offset(v.x / l, v.y / l)
}
private fun normal(v: Offset) = Offset(-v.y, v.x)
private fun point(c: Offset, along: Offset, across: Offset, a: Float, b: Float) =
    Offset(c.x + along.x * a + across.x * b, c.y + along.y * a + across.y * b)

private fun DrawScope.diamond(c: Offset, w: Float, h: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - h / 2f)
        lineTo(c.x + w / 2f, c.y)
        lineTo(c.x, c.y + h / 2f)
        lineTo(c.x - w / 2f, c.y)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.block(c: Offset, w: Float, h: Float, height: Float, top: Color, side: Color) {
    val topC = c - Offset(0f, height)
    val right = Path().apply {
        moveTo(topC.x + w / 2f, topC.y)
        lineTo(c.x + w / 2f, c.y)
        lineTo(c.x, c.y + h / 2f)
        lineTo(topC.x, topC.y + h / 2f)
        close()
    }
    val left = Path().apply {
        moveTo(topC.x - w / 2f, topC.y)
        lineTo(c.x - w / 2f, c.y)
        lineTo(c.x, c.y + h / 2f)
        lineTo(topC.x, topC.y + h / 2f)
        close()
    }
    drawPath(right, side)
    drawPath(left, side.copy(alpha = .82f))
    diamond(topC, w, h, top)
}

private fun trackGeometry(c: Offset, w: Float, h: Float, placement: TrailPlacement): Triple<Offset, Offset, Offset> {
    val top = c - Offset(0f, 8f)
    val entry = plus(top, scaled(gridVector(placement.entryHeading, w, h), -.5f))
    val exit = plus(top, scaled(gridVector(placement.exitHeading, w, h), .5f))
    return Triple(entry, top, exit)
}

private fun DrawScope.drawTrackSurface(c: Offset, w: Float, h: Float, placement: TrailPlacement) {
    val (entry, center, exit) = trackGeometry(c, w, h, placement)
    val path = Path().apply {
        moveTo(entry.x, entry.y)
        quadraticBezierTo(center.x, center.y, exit.x, exit.y)
    }
    drawPath(path, trail, style = Stroke(width = h * .34f, cap = StrokeCap.Round))
    drawPath(path, dirtSide.copy(alpha = .35f), style = Stroke(width = h * .055f, cap = StrokeCap.Round))
}

private fun basisFor(placement: TrailPlacement, w: Float, h: Float): Pair<Offset, Offset> {
    val incoming = unit(gridVector(placement.entryHeading, w, h))
    val outgoing = unit(gridVector(placement.exitHeading, w, h))
    val sum = plus(incoming, outgoing)
    val along = if (length(sum) < .1f) outgoing else unit(sum)
    return along to normal(along)
}

private fun DrawScope.quad(c: Offset, along: Offset, across: Offset, halfLength: Float, halfWidth: Float, color: Color) {
    val p = Path().apply {
        val a = point(c, along, across, -halfLength, -halfWidth)
        val b = point(c, along, across, halfLength, -halfWidth)
        val d = point(c, along, across, -halfLength, halfWidth)
        val e = point(c, along, across, halfLength, halfWidth)
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(e.x, e.y); lineTo(d.x, d.y); close()
    }
    drawPath(p, color)
}

private fun DrawScope.drawFeature(c: Offset, w: Float, h: Float, type: FeatureType, placement: TrailPlacement, t: Float) {
    block(c, w, h, 8f, dirt, dirtSide)
    drawTrackSurface(c, w, h, placement)
    val top = c - Offset(0f, 8f)
    val (along, across) = basisFor(placement, w, h)
    val len = w * .23f
    val narrow = h * .11f

    when (type) {
        FeatureType.TABLE_TOP -> {
            quad(top - Offset(0f, 8f), along, across, len * .72f, h * .16f, Color(0xFFF8C06B))
            drawLine(dirtSide, point(top, along, across, -len * .74f, -h * .16f), point(top, along, across, -len * .74f, h * .16f), 5f)
            if (sin(t.toDouble()) > .45) emoji("✨", top - Offset(0f, 24f), w * .13f)
        }
        FeatureType.GAP_JUMP -> {
            quad(point(top, along, across, -len * .55f, 0f), along, across, len * .30f, h * .19f, Color(0xFFF2A34D))
            quad(point(top, along, across, len * .60f, 0f), along, across, len * .27f, h * .19f, Color(0xFFF2A34D))
            drawCircle(ink.copy(alpha = .55f), h * .13f, top + Offset(0f, 2f))
            drawCircle(ink.copy(alpha = .13f + .08f * abs(sin((t * 2f).toDouble()).toFloat())), h * .18f, top)
        }
        FeatureType.DIRT_JUMP -> {
            val p = Path().apply {
                val a = point(top, along, across, -len, -h * .18f)
                val b = point(top - Offset(0f, 15f), along, across, 0f, -h * .19f)
                val c2 = point(top, along, across, len, -h * .18f)
                val d = point(top, along, across, len, h * .18f)
                val e = point(top - Offset(0f, 15f), along, across, 0f, h * .19f)
                val f = point(top, along, across, -len, h * .18f)
                moveTo(a.x,a.y); lineTo(b.x,b.y); lineTo(c2.x,c2.y); lineTo(d.x,d.y); lineTo(e.x,e.y); lineTo(f.x,f.y); close()
            }
            drawPath(p, Color(0xFFF2A34D))
        }
        FeatureType.WOODEN_SKINNY -> {
            drawLine(darkWood, point(top, along, across, -len, 0f), point(top, along, across, len, 0f), h * .18f, cap = StrokeCap.Round)
            drawLine(wood, point(top - Offset(0f, 3f), along, across, -len, 0f), point(top - Offset(0f, 3f), along, across, len, 0f), h * .11f, cap = StrokeCap.Round)
        }
        FeatureType.LAUNCHER_LOG -> {
            quad(point(top - Offset(0f, 8f), along, across, -len * .25f, 0f), along, across, len * .65f, h * .18f, Color(0xFFF5A64C))
            drawLine(darkWood, point(top - Offset(0f, 12f), along, across, len * .35f, -h * .23f), point(top - Offset(0f, 12f), along, across, len * .35f, h * .23f), h * .16f, cap = StrokeCap.Round)
            if (sin((t * 3f).toDouble()) > .3) emoji("🚀", point(top, along, across, len * .55f, -h * .25f) - Offset(0f, 18f), w * .14f)
        }
        FeatureType.DROP -> {
            quad(point(top - Offset(0f, 14f), along, across, -len * .28f, 0f), along, across, len * .60f, h * .22f, Color(0xFFF6B05C))
            val lip = point(top - Offset(0f, 11f), along, across, len * .35f, 0f)
            drawLine(ink.copy(alpha=.55f), point(lip, along, across, 0f, -h*.24f), point(lip, along, across, 0f, h*.24f), 6f)
            emoji("⬇️", point(top, along, across, len * .62f, 0f) - Offset(0f, 18f), w * .13f)
        }
        FeatureType.TEETER_TOTTER -> {
            val tilt = sin((t * 2.2f).toDouble()).toFloat() * 9f
            val a = point(top - Offset(0f, tilt), along, across, -len, 0f)
            val b = point(top + Offset(0f, tilt), along, across, len, 0f)
            drawLine(darkWood, a, b, h * .17f, cap = StrokeCap.Round)
            drawCircle(ink, h * .065f, top)
        }
        FeatureType.SALOON_DOORS -> {
            val postA = point(top, along, across, 0f, -h * .28f)
            val postB = point(top, along, across, 0f, h * .28f)
            drawLine(darkWood, postA - Offset(0f, 30f), postA + Offset(0f, 7f), 7f)
            drawLine(darkWood, postB - Offset(0f, 30f), postB + Offset(0f, 7f), 7f)
            val swing = abs(sin((t * 3f).toDouble()).toFloat()) * h * .13f
            drawLine(wood, postA - Offset(0f, 20f), point(top - Offset(0f, 20f), along, across, 0f, -swing), 8f)
            drawLine(wood, postB - Offset(0f, 20f), point(top - Offset(0f, 20f), along, across, 0f, swing), 8f)
        }
        FeatureType.TOILET_BOWL -> {
            drawOval(Color(0xFFF3F3E8), topLeft = top - Offset(w * .25f, h * .28f), size = Size(w * .50f, h * .56f))
            drawOval(water, topLeft = top - Offset(w * .16f, h * .17f), size = Size(w * .32f, h * .34f))
            drawArc(Color.White.copy(alpha=.8f), t * 90f, 115f, false, topLeft = top - Offset(w*.13f,h*.14f), size=Size(w*.26f,h*.28f), style=Stroke(4f))
        }
        FeatureType.ROCK_ROLL -> {
            for (i in -2..2) {
                val center = point(top, along, across, i * len * .34f, if (i % 2 == 0) -narrow else narrow)
                drawCircle(rock.copy(alpha=.86f + (i+2)*.025f), h * (.11f + (i+2)*.008f), center)
            }
        }
        FeatureType.CROCODILE_PIT -> {
            val pit = Path().apply {
                val a = point(top, along, across, -len, 0f)
                val b = point(top, along, across, 0f, -h*.30f)
                val c2 = point(top, along, across, len, 0f)
                val d = point(top, along, across, 0f, h*.30f)
                moveTo(a.x,a.y); lineTo(b.x,b.y); lineTo(c2.x,c2.y); lineTo(d.x,d.y); close()
            }
            drawPath(pit, water)
            emoji(if (sin((t*4f).toDouble()) > 0) "🐊" else "🦷", top + Offset(0f, 4f), w * .25f)
        }
        FeatureType.ROCKS -> {
            listOf(-.70f,-.25f,.20f,.62f).forEachIndexed { i, a ->
                val center = point(top, along, across, len * a, if (i % 2 == 0) -narrow else narrow)
                drawCircle(rock.copy(alpha=.82f + i*.05f), h * (.09f + i*.012f), center)
            }
        }
        FeatureType.ROOTS -> {
            for (i in -3..3) {
                val center = point(top, along, across, i * len * .25f, 0f)
                drawLine(darkWood, point(center, along, across, 0f, -h*.27f), point(center, along, across, 0f, h*.27f), 4f)
            }
        }
        FeatureType.BERM -> {
            val bank = if (placement.turn < 0) -1f else 1f
            val center = point(top, along, across, 0f, bank * h * .08f)
            drawArc(Color(0xFFFFD06B), if (bank > 0) 195f else 15f, 150f, false,
                topLeft = center - Offset(w*.28f,h*.40f), size = Size(w*.56f,h*.80f), style=Stroke(16f, cap=StrokeCap.Round))
            val dust = 3f + 3f * abs(sin((t*2f).toDouble()).toFloat())
            drawCircle(Color.White.copy(alpha=.28f), dust, point(top, along, across, -len*.55f, -bank*h*.20f))
        }
    }
}

private fun DrawScope.drawFinish(c: Offset, w: Float, h: Float, incomingHeading: Int) {
    block(c, w, h, 8f, Color(0xFFE7E7E7), Color(0xFF777777))
    val top = c - Offset(0f, 8f)
    val entry = plus(top, scaled(gridVector(incomingHeading, w, h), -.5f))
    drawLine(Color.White, entry, top, h * .30f, cap = StrokeCap.Round)
    drawLine(ink.copy(alpha=.35f), entry, top, h * .05f, cap = StrokeCap.Round)
    emoji("🏁", top - Offset(0f, 28f), w * .26f)
}

private fun quadraticPoint(a: Offset, control: Offset, b: Offset, t: Float): Offset {
    val q = 1f - t
    return Offset(
        q*q*a.x + 2f*q*t*control.x + t*t*b.x,
        q*q*a.y + 2f*q*t*control.y + t*t*b.y
    )
}

private fun quadraticTangent(a: Offset, control: Offset, b: Offset, t: Float): Offset = unit(
    Offset(
        2f*(1f-t)*(control.x-a.x) + 2f*t*(b.x-control.x),
        2f*(1f-t)*(control.y-a.y) + 2f*t*(b.y-control.y)
    )
)

private fun DrawScope.drawRiders(
    plans: List<SimulationPlan>,
    pieces: List<TrailPiece>,
    placements: List<TrailPlacement>,
    elapsed: Float,
    w: Float,
    h: Float,
    shift: Offset
) {
    if (pieces.isEmpty()) return
    val finishCell = TrailMath.end(pieces)
    val finishCenter = project(finishCell.x.toFloat(), finishCell.y.toFloat(), w, h) + shift - Offset(0f, 8f)
    val lastExitHeading = pieces.last().heading.mod(4)
    val finishEntry = plus(finishCenter, scaled(gridVector(lastExitHeading, w, h), -.5f))

    plans.forEachIndexed { idx, plan ->
        val raw = ((elapsed - idx * .12f).coerceAtLeast(0f) * plan.rider.speed * TrackSimulator.PROGRESS_RATE)
        val crashPiece = plan.crashPiece
        val crashPos = crashPiece?.let { it + FeatureDynamics.crashProgress(pieces[it].type) }
        val progress = if (crashPos != null) minOf(raw, crashPos) else minOf(raw, pieces.size + .72f)

        if (progress >= pieces.size) {
            val q = ((progress - pieces.size) / .70f).coerceIn(0f, 1f)
            val pos = lerp(finishEntry, finishCenter, q) - Offset(0f, 14f + plan.laneOffset*18f)
            drawBikeAndRider(pos, 0f, 1f, plan.rider)
            return@forEachIndexed
        }

        val pieceIndex = floor(progress).toInt().coerceIn(0, pieces.lastIndex)
        val local = (progress - pieceIndex).coerceIn(0f, 1f)
        val placement = placements[pieceIndex]
        val center = project(placement.cell.x.toFloat(), placement.cell.y.toFloat(), w, h) + shift
        val (entry, control, exit) = trackGeometry(center, w, h, placement)
        val base = quadraticPoint(entry, control, exit, local)
        val tangent = quadraticTangent(entry, control, exit, local)
        val across = normal(tangent)
        val pose = FeatureDynamics.pose(pieces[pieceIndex].type, local, placement.turn)
        val lane = scaled(across, plan.laneOffset * 18f)
        val featureOffset = scaled(across, pose.lateral)
        val pos = plus(plus(base, lane), featureOffset) - Offset(0f, 14f + pose.lift)
        val crashed = crashPos != null && raw >= crashPos

        if (crashed) {
            val age = ((raw - crashPos!!) / TrackSimulator.PROGRESS_RATE).coerceIn(0f, 1.6f)
            drawFeatureCrash(pieces[pieceIndex].type, pos, tangent, across, age, w, plan.rider)
        } else {
            drawInteractionEffect(pieces[pieceIndex].type, pos, local, w)
            drawBikeAndRider(pos, RiderVisuals.uprightLean(pose.rotation), pose.scale, plan.rider)
        }
    }
}

private fun angleOf(v: Offset): Float = atan2(v.y.toDouble(), v.x.toDouble()).toFloat() * 180f / PI.toFloat()
private fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x-a.x)*t, a.y + (b.y-a.y)*t)

private fun DrawScope.drawBikeAndRider(pos: Offset, angle: Float, scale: Float, rider: RiderProfile) {
    rotate(angle, pivot = pos) {
        emoji("🚲", pos + Offset(0f, 7f * scale), 25f * scale)
        emoji(rider.emoji, pos - Offset(0f, 12f * scale), 22f * scale)
    }
}

private fun DrawScope.drawInteractionEffect(type: FeatureType, pos: Offset, progress: Float, w: Float) {
    if (progress < .18f || progress > .82f) return
    when (type) {
        FeatureType.TABLE_TOP -> if (progress in .35f.. .65f) emoji("✨", pos - Offset(w*.10f, 18f), w*.10f)
        FeatureType.GAP_JUMP -> emoji("💨", pos + Offset(-w*.13f, 14f), w*.11f)
        FeatureType.DIRT_JUMP -> emoji("☁️", pos + Offset(-w*.11f, 16f), w*.10f)
        FeatureType.WOODEN_SKINNY -> emoji("〰️", pos + Offset(0f, 18f), w*.10f)
        FeatureType.LAUNCHER_LOG -> emoji("🚀", pos + Offset(-w*.12f, 10f), w*.10f)
        FeatureType.DROP -> if (progress > .48f) emoji("😱", pos - Offset(0f, 22f), w*.10f)
        FeatureType.TEETER_TOTTER -> emoji("⚖️", pos + Offset(w*.12f, 10f), w*.09f)
        FeatureType.SALOON_DOORS -> if (progress in .42f.. .58f) emoji("🤠", pos - Offset(0f, 24f), w*.10f)
        FeatureType.TOILET_BOWL -> emoji("🌀", pos + Offset(w*.11f, 5f), w*.10f)
        FeatureType.ROCK_ROLL -> emoji("💢", pos + Offset(-w*.11f, 8f), w*.09f)
        FeatureType.CROCODILE_PIT -> emoji("🐊", pos + Offset(0f, 27f), w*.11f)
        FeatureType.ROCKS -> emoji("💥", pos + Offset(w*.10f, 10f), w*.08f)
        FeatureType.ROOTS -> emoji("〽️", pos + Offset(-w*.10f, 8f), w*.09f)
        FeatureType.BERM -> emoji("💨", pos + Offset(-w*.12f, 12f), w*.10f)
    }
}

private fun DrawScope.drawFeatureCrash(
    type: FeatureType,
    pos: Offset,
    tangent: Offset,
    across: Offset,
    age: Float,
    w: Float,
    rider: RiderProfile
) {
    val down = Offset(0f, age * age * 24f)
    val forward = scaled(tangent, age * 18f)
    val sideways = scaled(across, age * 26f)
    val wobble = sin((age * 10f).toDouble()).toFloat()
    when (type) {
        FeatureType.TABLE_TOP -> {
            val p = plus(plus(pos, forward), down)
            emoji("💥", p - Offset(0f, 16f), w*.17f)
            drawBikeAndRider(p, age*220f, 1f, rider)
        }
        FeatureType.GAP_JUMP -> {
            val p = pos + Offset(0f, 44f*age*age)
            emoji("🕳️", p + Offset(0f, 18f), w*.16f)
            drawBikeAndRider(p, age*310f, .95f, rider)
        }
        FeatureType.DIRT_JUMP -> {
            val p = plus(plus(pos, forward), Offset(0f, 28f*age*age))
            emoji("☁️", p + Offset(-18f, 12f), w*.17f)
            drawBikeAndRider(p, 140f*age, 1f, rider)
        }
        FeatureType.WOODEN_SKINNY -> {
            val p = plus(pos, scaled(sideways, 1.25f)) + down
            emoji("🪵", pos + Offset(0f, 22f), w*.14f)
            drawBikeAndRider(p, 95f*age*wobble, 1f, rider)
        }
        FeatureType.LAUNCHER_LOG -> {
            val p = plus(pos, scaled(tangent, age*30f)) + Offset(0f, -52f*age + 42f*age*age)
            emoji("🛰️", p - Offset(0f, 22f), w*.13f)
            drawBikeAndRider(p, age*420f, .95f, rider)
        }
        FeatureType.DROP -> {
            val p = plus(pos, scaled(tangent, age*10f)) + Offset(0f, 58f*age*age)
            emoji("⬇️", p + Offset(20f, 0f), w*.12f)
            drawBikeAndRider(p, 175f*age, 1f, rider)
        }
        FeatureType.TEETER_TOTTER -> {
            val arc = -34f * sin((age.coerceAtMost(1f) * PI).toDouble()).toFloat()
            val p = plus(pos, scaled(tangent, age*34f)) + Offset(0f, arc + 18f*age)
            emoji("⚖️", pos + Offset(0f, 22f), w*.12f)
            drawBikeAndRider(p, -210f*age, 1f, rider)
        }
        FeatureType.SALOON_DOORS -> {
            val p = plus(pos, scaled(tangent, -age*20f)) + Offset(0f, 12f*age)
            emoji("🚪", plus(pos, scaled(across, 24f)), w*.16f)
            drawBikeAndRider(p, wobble*45f, .92f, rider)
        }
        FeatureType.TOILET_BOWL -> {
            val circle = Offset(cos((age*7f).toDouble()).toFloat()*22f, sin((age*7f).toDouble()).toFloat()*12f + age*18f)
            val p = plus(pos, circle)
            emoji("🌀", pos, w*.18f)
            drawBikeAndRider(p, age*640f, .90f, rider)
        }
        FeatureType.ROCK_ROLL -> {
            val p = plus(pos, scaled(tangent, age*20f)) + Offset(0f, -abs(wobble)*14f + age*24f)
            emoji("🪨", plus(pos, scaled(across, -20f)), w*.13f)
            drawBikeAndRider(p, wobble*130f, 1f, rider)
        }
        FeatureType.CROCODILE_PIT -> {
            val p = plus(pos, scaled(across, wobble*10f)) + Offset(0f, -20f*age + 36f*age*age)
            emoji("🐊", pos + Offset(0f, 24f), w*.19f)
            emoji("🦷", p + Offset(18f, 8f), w*.11f)
            drawBikeAndRider(p, age*250f, .92f, rider)
        }
        FeatureType.ROCKS -> {
            val p = plus(plus(pos, scaled(across, wobble*22f)), forward) + down
            emoji("💢", p + Offset(-18f, 8f), w*.12f)
            drawBikeAndRider(p, wobble*150f, 1f, rider)
        }
        FeatureType.ROOTS -> {
            val riderPos = plus(pos, scaled(across, wobble*16f)) + down
            val bikePos = plus(riderPos, scaled(tangent, age*28f)) + Offset(0f, 12f)
            emoji("🚲", bikePos, w*.17f)
            emoji(rider.emoji, riderPos - Offset(0f, 10f), w*.16f)
            emoji("🌳", pos + Offset(-18f, 18f), w*.12f)
        }
        FeatureType.BERM -> {
            val p = plus(pos, scaled(across, age*58f)) + Offset(0f, age*22f)
            emoji("☁️", plus(pos, scaled(across, age*30f)), w*.16f)
            drawBikeAndRider(p, 110f*age, 1f, rider)
        }
    }
}

private fun DrawScope.emoji(text: String, at: Offset, sizePx: Float) {
    drawContext.canvas.nativeCanvas.apply {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx
            textAlign = Paint.Align.CENTER
            color = Color.Black.toArgb()
        }
        drawText(text, at.x, at.y, p)
    }
}
