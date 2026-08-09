package com.hdclark.simbikepark

import org.junit.Assert.*
import org.junit.Test

class TrailMathTest {
    @Test fun `adjacent tiles share the exact same connector`() {
        val pieces = listOf(
            TrailPiece(FeatureType.TABLE_TOP, 0),
            TrailPiece(FeatureType.BERM, 1),
            TrailPiece(FeatureType.ROOTS, 1),
            TrailPiece(FeatureType.ROCKS, 0)
        )
        val placements = TrailMath.placements(pieces)
        assertEquals(
            listOf(GridPoint(0,0), GridPoint(1,0), GridPoint(1,1), GridPoint(1,2)),
            placements.map { it.cell }
        )
        for (i in 0 until placements.lastIndex) {
            assertEquals(placements[i].exitHeading, placements[i + 1].entryHeading)
            assertEquals(TrailMath.exitConnector(placements[i]), TrailMath.entryConnector(placements[i + 1]))
        }
    }

    @Test fun `legacy reverse headings are repaired to straight connectors`() {
        val repaired = TrailMath.sanitized(
            listOf(
                TrailPiece(FeatureType.TABLE_TOP, 0),
                TrailPiece(FeatureType.GAP_JUMP, 2),
                TrailPiece(FeatureType.BERM, 1)
            )
        )
        assertEquals(listOf(0, 0, 1), repaired.map { it.heading })
        assertFalse(TrailMath.isReverse(repaired[0].heading, repaired[1].heading))
    }

    @Test fun `every feature has a distinct rider motion signature`() {
        val samples = FeatureType.entries.associateWith { type ->
            listOf(.23f, .47f, .71f).map { FeatureDynamics.pose(type, it, 1) }
        }
        assertEquals(FeatureType.entries.size, samples.values.toSet().size)
        samples.values.flatten().forEach { pose ->
            assertTrue(pose.scale > 0f)
        }
    }

    @Test fun `every feature crashes at an obstacle interaction point`() {
        FeatureType.entries.forEach { type ->
            assertTrue(FeatureDynamics.crashProgress(type) in .45f.. .75f)
        }
    }

    @Test fun `simulator always releases three distinct riders`() {
        val pieces = FeatureType.entries.map { TrailPiece(it, 0) }
        val plans = TrackSimulator.plan(pieces, 1234)
        assertEquals(3, plans.size)
        assertEquals(3, plans.map { it.rider.name }.toSet().size)
        assertTrue(TrackSimulator.durationSeconds(pieces.size, plans) > 9f)
    }
}
