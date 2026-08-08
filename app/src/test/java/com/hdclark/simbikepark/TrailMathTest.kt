package com.hdclark.simbikepark

import org.junit.Assert.*
import org.junit.Test

class TrailMathTest {
    @Test fun `trail geometry follows sequential headings`() {
        val pieces = listOf(
            TrailPiece(FeatureType.TABLE_TOP, 0),
            TrailPiece(FeatureType.BERM, 1),
            TrailPiece(FeatureType.ROOTS, 2),
            TrailPiece(FeatureType.ROCKS, 3)
        )
        assertEquals(listOf(GridPoint(0,0), GridPoint(1,0), GridPoint(1,1), GridPoint(0,1)), TrailMath.starts(pieces))
    }

    @Test fun `simulator always releases three riders`() {
        val pieces = FeatureType.entries.map { TrailPiece(it, 0) }
        val plans = TrackSimulator.plan(pieces, 1234)
        assertEquals(3, plans.size)
        assertEquals(3, plans.map { it.rider.name }.toSet().size)
    }
}
