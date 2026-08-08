package com.hdclark.simbikepark

import kotlin.math.max
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

object TrailMath {
    fun starts(pieces: List<TrailPiece>): List<GridPoint> {
        var x = 0
        var y = 0
        return pieces.map { piece ->
            GridPoint(x, y).also {
                when (piece.heading.mod(4)) {
                    0 -> x++
                    1 -> y++
                    2 -> x--
                    else -> y--
                }
            }
        }
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

    private fun skillFor(r: RiderProfile, type: FeatureType): Float = when (type) {
        FeatureType.TABLE_TOP -> (r.air + r.tech) / 200f
        FeatureType.GAP_JUMP, FeatureType.LAUNCHER_LOG, FeatureType.CROCODILE_PIT -> (r.air + r.nerve) / 200f
        FeatureType.DIRT_JUMP, FeatureType.DROP -> (r.air + r.tech + r.nerve) / 300f
        FeatureType.WOODEN_SKINNY, FeatureType.TEETER_TOTTER -> (r.balance + r.nerve) / 200f
        FeatureType.SALOON_DOORS -> (r.tech + r.nerve) / 200f
        FeatureType.TOILET_BOWL, FeatureType.BERM -> (r.balance + r.tech) / 200f
        FeatureType.ROCK_ROLL, FeatureType.ROCKS, FeatureType.ROOTS -> (r.tech + r.balance) / 200f
    }

    private fun crashLine(type: FeatureType?, random: Random): String {
        val generic = listOf("invented a new dismount", "became briefly horizontal", "tested gravity successfully", "donated a water bottle to the forest")
        return when (type) {
            FeatureType.CROCODILE_PIT -> "angered the crocodile union 🐊"
            FeatureType.TOILET_BOWL -> "got flushed with remarkable commitment 🚽"
            FeatureType.SALOON_DOORS -> "forgot this is not a western 🤠"
            FeatureType.TEETER_TOTTER -> "lost an argument with a plank"
            FeatureType.WOODEN_SKINNY -> "ran out of skinny before running out of confidence"
            FeatureType.GAP_JUMP -> "discovered the gap portion of the gap jump"
            FeatureType.LAUNCHER_LOG -> "achieved low Earth orbit"
            else -> generic[random.nextInt(generic.size)]
        }
    }
}
