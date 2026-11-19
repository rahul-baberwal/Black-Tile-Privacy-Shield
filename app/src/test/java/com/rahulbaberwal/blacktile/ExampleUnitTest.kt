package com.rahulbaberwal.blacktile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for Black Tile data structures and geometry calculations.
 */
class ExampleUnitTest {

    @Test
    fun tile_instantiationAndMutation() {
        val tile = Tile(
            id = "test-id-1",
            x = 100,
            y = 200,
            w = 400,
            h = 300
        )

        assertEquals("test-id-1", tile.id)
        assertEquals(100, tile.x)
        assertEquals(200, tile.y)
        assertEquals(400, tile.w)
        assertEquals(300, tile.h)

        // Mutate position and dimensions
        tile.x = 150
        tile.y = 250
        tile.w = 500
        tile.h = 350

        assertEquals(150, tile.x)
        assertEquals(250, tile.y)
        assertEquals(500, tile.w)
        assertEquals(350, tile.h)
    }

    @Test
    fun tile_minBoundsValidation() {
        val minSize = 80
        val requestedW = 40
        val requestedH = 50

        val clampedW = requestedW.coerceAtLeast(minSize)
        val clampedH = requestedH.coerceAtLeast(minSize)

        assertEquals(80, clampedW)
        assertEquals(80, clampedH)
    }
}
