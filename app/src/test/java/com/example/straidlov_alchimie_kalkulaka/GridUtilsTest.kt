package com.example.straidlov_alchimie_kalkulaka

import com.example.straidlov_alchimie_kalkulaka.model.GridUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class GridUtilsTest {

    @Test
    fun testTotalCombinationsCount() {
        assertEquals(262144, GridUtils.TOTAL_COMBINATIONS)
    }

    @Test
    fun testEncodeAndDecodeBijection() {
        // Test all-0 grid
        val allZeros = IntArray(9) { 0 }
        val id0 = GridUtils.encodeGrid(allZeros)
        assertEquals(0, id0)
        assertArrayEquals(allZeros, GridUtils.decodeIndex(id0))

        // Test all-3 grid
        val allThrees = IntArray(9) { 3 }
        val maxId = GridUtils.encodeGrid(allThrees)
        assertEquals(262143, maxId)
        assertArrayEquals(allThrees, GridUtils.decodeIndex(maxId))

        // Test sample custom grid
        val sampleGrid = intArrayOf(0, 1, 2, 2, 1, 0, 1, 0, 2)
        val sampleId = GridUtils.encodeGrid(sampleGrid)
        assertArrayEquals(sampleGrid, GridUtils.decodeIndex(sampleId))
    }

    @Test
    fun testRotation90DegreesFourTimes() {
        val original = intArrayOf(0, 1, 2, 0, 2, 1, 1, 0, 2)
        var current = original

        // 4 rotations by 90 degrees should return to original grid
        for (i in 0 until 4) {
            current = GridUtils.rotateGrid90(current)
        }

        assertArrayEquals(original, current)
    }

    @Test
    fun testHorizontalAndVerticalMirroring() {
        val original = intArrayOf(0, 1, 2, 0, 2, 1, 1, 0, 2)

        // Double horizontal mirror = original
        val hMirrored = GridUtils.mirrorGridHorizontal(original)
        val hDoubleMirrored = GridUtils.mirrorGridHorizontal(hMirrored)
        assertArrayEquals(original, hDoubleMirrored)

        // Double vertical mirror = original
        val vMirrored = GridUtils.mirrorGridVertical(original)
        val vDoubleMirrored = GridUtils.mirrorGridVertical(vMirrored)
        assertArrayEquals(original, vDoubleMirrored)
    }

    @Test
    fun testPatternFormatting() {
        val grid = intArrayOf(0, 1, 2, 2, 0, 1, 1, 2, 0)
        val formatted = GridUtils.formatPatternString(grid)
        assertEquals("012-201-120", formatted)
    }

    @Test
    fun testTableCoordinateSystemUsesFirstCellAsMostSignificant() {
        val grid = intArrayOf(0, 0, 0, 0, 0, 2, 1, 0, 0)

        assertEquals(144, GridUtils.encodeGrid(grid))
        assertArrayEquals(grid, GridUtils.decodeIndex(144))
    }

    @Test
    fun testLegacyIdMigrationPreservesGrid() {
        val grid = intArrayOf(0, 0, 0, 0, 0, 2, 1, 0, 0)
        val legacyId = 6144

        assertEquals(144, GridUtils.migrateLegacyId(legacyId))
        assertArrayEquals(grid, GridUtils.decodeIndex(GridUtils.migrateLegacyId(legacyId)))
    }

    @Test
    fun testValuesAreClampedToZeroThroughThree() {
        val grid = intArrayOf(-1, 0, 1, 2, 3, 4, 8, 0, 3)
        val decoded = GridUtils.decodeIndex(GridUtils.encodeGrid(grid))
        assertArrayEquals(intArrayOf(0, 0, 1, 2, 3, 3, 3, 0, 3), decoded)
    }
}
