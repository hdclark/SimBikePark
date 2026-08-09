package com.hdclark.simbikepark

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

enum class FeatureType(val title: String, val emoji: String, val difficulty: Float) {
    TABLE_TOP("Table Top", "🟫", .36f),
    GAP_JUMP("Gap Jump", "⚠️", .62f),
    DIRT_JUMP("Dirt Jump", "⛰️", .48f),
    WOODEN_SKINNY("Wooden Skinny", "🪵", .55f),
    LAUNCHER_LOG("Launcher Log", "🚀", .68f),
    DROP("Drop", "⬇️", .58f),
    TEETER_TOTTER("Teeter-Totter", "⚖️", .52f),
    SALOON_DOORS("Saloon Doors", "🚪", .46f),
    TOILET_BOWL("Toilet Bowl", "🚽", .64f),
    ROCK_ROLL("Rock Roll", "🪨", .57f),
    CROCODILE_PIT("Crocodile Pit", "🐊", .78f),
    ROCKS("Rocks", "🪨", .42f),
    ROOTS("Roots", "🌳", .40f),
    BERM("Berm", "🌀", .38f)
}

data class TrailPiece(val type: FeatureType, val heading: Int)
data class GridPoint(val x: Int, val y: Int)
data class GridPosition(val x: Float, val y: Float)
data class TrailPlacement(val cell: GridPoint, val entryHeading: Int, val exitHeading: Int) {
    val turn: Int get() = TrailMath.turnDelta(entryHeading, exitHeading)
}

data class RiderProfile(
    val name: String,
    val emoji: String,
    val air: Int,
    val balance: Int,
    val nerve: Int,
    val tech: Int,
    val speed: Float,
    val motto: String
)

data class SimulationPlan(
    val rider: RiderProfile,
    val crashPiece: Int?,
    val crashFlavor: String,
    val laneOffset: Float
)

data class FeaturePose(
    val lift: Float = 0f,
    val lateral: Float = 0f,
    val rotation: Float = 0f,
    val scale: Float = 1f
)

object TrailMath {
    fun direction(heading: Int): GridPoint = when (heading.mod(4)) {
        0 -> GridPoint(1, 0)
        1 -> GridPoint(0, 1)
        2 -> GridPoint(-1, 0)
        else -> GridPoint(0, -1)
    }

    fun placements(pieces: List<TrailPiece>): List<TrailPlacement> {
        var cell = GridPoint(0, 0)
        return pieces.mapIndexed { index, piece ->
            val exit = piece.heading.mod(4)
            val entry = if (index == 0) exit else pieces[index - 1].heading.mod(4)
            TrailPlacement(cell, entry, exit).also {
                val d = direction(exit)
                cell = GridPoint(cell.x + d.x, cell.y + d.y)
            }
        }
    }

    fun starts(pieces: List<TrailPiece>): List<GridPoint> = placements(pieces).map { it.cell }

    fun entryConnector(placement: TrailPlacement): GridPosition {
        val d = direction(placement.entryHeading)
        return GridPosition(placement.cell.x - d.x * .5f, placement.cell.y - d.y * .5f)
    }

    fun exitConnector(placement: TrailPlacement): GridPosition {
        val d = direction(placement.exitHeading)
        return GridPosition(placement.cell.x + d.x * .5f, placement.cell.y + d.y * .5f)
    }

    fun end(pieces: List<TrailPiece>): GridPoint {
        if (pieces.isEmpty()) return GridPoint(0, 0)
        val last = placements(pieces).last()
        val d = direction(last.exitHeading)
        return GridPoint(last.cell.x + d.x, last.cell.y + d.y)
    }

    fun turnDelta(entryHeading: Int, exitHeading: Int): Int = when ((exitHeading - entryHeading).mod(4)) {
        1 -> 1
        3 -> -1
        else -> 0
    }

    fun isReverse(entryHeading: Int, exitHeading: Int): Boolean =
        (exitHeading - entryHeading).mod(4) == 2

    fun sanitized(pieces: List<TrailPiece>): List<TrailPiece> {
        if (pieces.isEmpty()) return emptyList()
        var previous = pieces.first().heading.mod(4)
        return pieces.mapIndexed { index, piece ->
            val requested = piece.heading.mod(4)
            val heading = if (index > 0 && isReverse(previous, requested)) previous else requested
            previous = heading
            piece.copy(heading = heading)
        }
    }
}

object FeatureDynamics {
    private val pi = PI.toFloat()
    private fun wave(progress: Float, halfTurns: Float): Float =
        sin((progress * pi * halfTurns).toDouble()).toFloat()

    fun pose(type: FeatureType, progress: Float, turn: Int = 0): FeaturePose {
        val p = progress.coerceIn(0f, 1f)
        val envelope = wave(p, 1f).coerceAtLeast(0f)
        return when (type) {
            FeatureType.TABLE_TOP -> {
                val lift = when {
                    p < .25f -> 16f * p / .25f
                    p < .75f -> 16f
                    else -> 16f * (1f - p) / .25f
                }
                FeaturePose(lift = lift, rotation = -4f * wave(p, 2f))
            }
            FeatureType.GAP_JUMP -> FeaturePose(
                lift = 48f * 4f * p * (1f - p),
                rotation = -18f + 36f * p,
                scale = .96f + .08f * envelope
            )
            FeatureType.DIRT_JUMP -> FeaturePose(
                lift = 30f * envelope,
                rotation = -12f * wave(p, 1f)
            )
            FeatureType.WOODEN_SKINNY -> FeaturePose(
                lateral = 7f * wave(p, 6f) * envelope,
                rotation = 14f * wave(p, 4f) * envelope
            )
            FeatureType.LAUNCHER_LOG -> FeaturePose(
                lift = 58f * 4f * p * (1f - p),
                rotation = -32f * envelope,
                scale = 1f + .08f * envelope
            )
            FeatureType.DROP -> {
                val q = ((p - .42f) / .58f).coerceIn(0f, 1f)
                FeaturePose(
                    lift = if (p < .42f) 5f * wave(p / .42f, 1f) else -20f * wave(q, 1f),
                    rotation = 30f * wave(q, 1f)
                )
            }
            FeatureType.TEETER_TOTTER -> FeaturePose(
                lift = 11f * wave(p, 2f),
                rotation = 22f * wave(p, 2f)
            )
            FeatureType.SALOON_DOORS -> {
                val squeeze = envelope * envelope * envelope * envelope
                FeaturePose(
                    lift = 3f * wave(p, 4f) * envelope,
                    rotation = 9f * wave(p, 4f) * envelope,
                    scale = 1f - .14f * squeeze
                )
            }
            FeatureType.TOILET_BOWL -> FeaturePose(
                lift = 8f * wave(p, 4f) * envelope,
                lateral = 18f * wave(p, 2f) * envelope,
                rotation = 540f * p
            )
            FeatureType.ROCK_ROLL -> FeaturePose(
                lift = 12f * abs(wave(p, 3f)) * envelope,
                lateral = 3f * wave(p, 3f) * envelope,
                rotation = 20f * wave(p, 3f) * envelope
            )
            FeatureType.CROCODILE_PIT -> FeaturePose(
                lift = 44f * 4f * p * (1f - p),
                lateral = 3f * wave(p, 2f) * envelope,
                rotation = -10f + 20f * p
            )
            FeatureType.ROCKS -> FeaturePose(
                lift = 9f * abs(wave(p, 6f)) * envelope,
                lateral = 5f * wave(p, 5f) * envelope,
                rotation = 15f * wave(p, 6f) * envelope
            )
            FeatureType.ROOTS -> FeaturePose(
                lift = 5f * abs(wave(p, 10f)) * envelope,
                lateral = 2.5f * wave(p, 8f) * envelope,
                rotation = 7f * wave(p, 10f) * envelope
            )
            FeatureType.BERM -> {
                val bank = if (turn < 0) -1f else 1f
                FeaturePose(
                    lift = 4f * envelope,
                    lateral = 14f * bank * envelope,
                    rotation = -32f * bank * envelope,
                    scale = 1f + .04f * envelope
                )
            }
        }
    }

    fun crashProgress(type: FeatureType): Float = when (type) {
        FeatureType.TABLE_TOP -> .62f
        FeatureType.GAP_JUMP -> .50f
        FeatureType.DIRT_JUMP -> .58f
        FeatureType.WOODEN_SKINNY -> .72f
        FeatureType.LAUNCHER_LOG -> .56f
        FeatureType.DROP -> .61f
        FeatureType.TEETER_TOTTER -> .58f
        FeatureType.SALOON_DOORS -> .50f
        FeatureType.TOILET_BOWL -> .64f
        FeatureType.ROCK_ROLL -> .62f
        FeatureType.CROCODILE_PIT -> .50f
        FeatureType.ROCKS -> .66f
        FeatureType.ROOTS -> .60f
        FeatureType.BERM -> .72f
    }
}

object Riders {
    val roster = listOf(
        RiderProfile("Sendy Sam", "😎", 91, 43, 95, 62, 1.08f, "Brakes are just handlebars for quitters."),
        RiderProfile("Steady Betty", "🤓", 54, 93, 68, 86, .92f, "I brought a torque wrench."),
        RiderProfile("Nervous Nev", "😬", 66, 70, 34, 74, .88f, "Is there a chicken line?"),
        RiderProfile("Rocket Rita", "🤩", 88, 58, 86, 72, 1.13f, "More speed fixes most geometry."),
        RiderProfile("Rooty Rudy", "🥸", 47, 84, 72, 96, .98f, "Suspension is a state of mind."),
        RiderProfile("Tiny Tina", "😁", 72, 88, 81, 79, 1.02f, "I can definitely clear that. Probably.")
    )
}

object TrackSimulator {
    const val PROGRESS_RATE = .72f

    fun durationSeconds(pieceCount: Int, plans: List<SimulationPlan>): Float {
        val slowest = plans.minOfOrNull { it.rider.speed } ?: .88f
        return (((pieceCount + .72f) / (PROGRESS_RATE * slowest)) + .65f).coerceAtLeast(3f)
    }

    fun plan(pieces: List<TrailPiece>, seed: Int): List<SimulationPlan> {
        if (pieces.isEmpty()) return emptyList()
        val random = Random(seed)
        return Riders.roster.shuffled(random).take(3).mapIndexed { lane, rider ->
            var crash: Int? = null
            for ((index, piece) in pieces.withIndex()) {
                val skill = skillFor(rider, piece.type)
                val chance = (.05f + max(0f, piece.type.difficulty - skill) * .82f).coerceIn(.03f, .82f)
                if (random.nextFloat() < chance) {
                    crash = index
                    break
                }
            }
            SimulationPlan(
                rider = rider,
                crashPiece = crash,
                crashFlavor = crashLine(crash?.let { pieces.getOrNull(it)?.type }, random),
                laneOffset = (lane - 1) * .18f
            )
        }
    }

    internal fun skillFor(r: RiderProfile, type: FeatureType): Float {
        val speed = ((r.speed - .80f) / .38f * 100f).coerceIn(0f, 100f)
        val score = when (type) {
            FeatureType.TABLE_TOP -> .55f*r.air + .30f*r.tech + .15f*r.balance
            FeatureType.GAP_JUMP -> .55f*r.air + .35f*r.nerve + .10f*r.tech
            FeatureType.DIRT_JUMP -> .45f*r.air + .35f*r.tech + .20f*r.balance
            FeatureType.WOODEN_SKINNY -> .70f*r.balance + .20f*r.tech + .10f*r.nerve
            FeatureType.LAUNCHER_LOG -> .55f*r.air + .30f*r.nerve + .15f*speed
            FeatureType.DROP -> .35f*r.air + .35f*r.nerve + .30f*r.tech
            FeatureType.TEETER_TOTTER -> .55f*r.balance + .30f*r.tech + .15f*r.nerve
            FeatureType.SALOON_DOORS -> .45f*r.tech + .35f*r.nerve + .20f*r.balance
            FeatureType.TOILET_BOWL -> .50f*r.balance + .35f*r.tech + .15f*r.nerve
            FeatureType.ROCK_ROLL -> .55f*r.tech + .30f*r.balance + .15f*r.nerve
            FeatureType.CROCODILE_PIT -> .45f*r.air + .45f*r.nerve + .10f*speed
            FeatureType.ROCKS -> .55f*r.tech + .35f*r.balance + .10f*r.nerve
            FeatureType.ROOTS -> .50f*r.tech + .40f*r.balance + .10f*speed
            FeatureType.BERM -> .50f*r.balance + .25f*r.tech + .25f*speed
        }
        return score / 100f
    }

    private fun crashLine(type: FeatureType?, random: Random): String {
        val generic = listOf("invented a new dismount", "became briefly horizontal", "tested gravity successfully", "donated a water bottle to the forest")
        return when (type) {
            FeatureType.TABLE_TOP -> "table-topped the bike and picnic-tabled the landing"
            FeatureType.GAP_JUMP -> "discovered the gap portion of the gap jump"
            FeatureType.DIRT_JUMP -> "case-studied the landing with their rear wheel"
            FeatureType.WOODEN_SKINNY -> "ran out of skinny before running out of confidence"
            FeatureType.LAUNCHER_LOG -> "achieved low Earth orbit"
            FeatureType.DROP -> "sent the front wheel on a solo expedition"
            FeatureType.TEETER_TOTTER -> "lost an argument with a plank"
            FeatureType.SALOON_DOORS -> "forgot this is not a western 🤠"
            FeatureType.TOILET_BOWL -> "got flushed with remarkable commitment 🚽"
            FeatureType.ROCK_ROLL -> "became the rock roll's percussion section"
            FeatureType.CROCODILE_PIT -> "angered the crocodile union 🐊"
            FeatureType.ROCKS -> "pinballed through geology"
            FeatureType.ROOTS -> "was audited by the root network"
            FeatureType.BERM -> "high-sided into a cloud of optimism"
            null -> generic[random.nextInt(generic.size)]
        }
    }
}
