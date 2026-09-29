package com.example.straidlov_alchimie_kalkulaka.model

object GridUtils {
    const val STATE_COUNT = 4 // Possible substance amounts: 0, 1, 2, 3
    const val TOTAL_COMBINATIONS = 262144 // 4^9

    /**
     * Encodes a 3x3 grid (9 items, values 0..3) into the table's index.
     *
     * The first cell is the most significant digit, matching the table's
     * base-4 code order. For example, 000002100 has index 144 (table order 145).
     */
    fun encodeGrid(grid: IntArray): Int {
        var index = 0
        for (i in 0 until 9) {
            val cellVal = (grid.getOrElse(i) { 0 }).coerceIn(0, STATE_COUNT - 1)
            index = index * STATE_COUNT + cellVal
        }
        return index.coerceIn(0, TOTAL_COMBINATIONS - 1)
    }

    /**
     * Decodes a table index into a 3x3 grid with values 0..3.
     */
    fun decodeIndex(index: Int): IntArray {
        val result = IntArray(9)
        var temp = index.coerceIn(0, TOTAL_COMBINATIONS - 1)
        for (i in 8 downTo 0) {
            result[i] = temp % STATE_COUNT
            temp /= STATE_COUNT
        }
        return result
    }

    /**
     * Converts IDs written by versions that used the old little-endian order.
     */
    fun migrateLegacyId(legacyId: Int): Int {
        return encodeGrid(decodeLegacyIndex(legacyId))
    }

    private fun decodeLegacyIndex(index: Int): IntArray {
        val result = IntArray(9)
        var temp = index.coerceIn(0, TOTAL_COMBINATIONS - 1)
        for (i in 0 until 9) {
            result[i] = temp % STATE_COUNT
            temp /= STATE_COUNT
        }
        return result
    }

    /**
     * Rotates a 3x3 grid clockwise by 90 degrees.
     * Grid mapping:
     * 0 1 2     6 3 0
     * 3 4 5  -> 7 4 1
     * 6 7 8     8 5 2
     */
    fun rotateGrid90(grid: IntArray): IntArray {
        val rotated = IntArray(9)
        val rotationMap = intArrayOf(6, 3, 0, 7, 4, 1, 8, 5, 2)
        for (i in 0 until 9) {
            rotated[i] = grid[rotationMap[i]]
        }
        return rotated
    }

    /**
     * Mirrors grid horizontally (left <-> right).
     * 0 1 2     2 1 0
     * 3 4 5  -> 5 4 3
     * 6 7 8     8 7 6
     */
    fun mirrorGridHorizontal(grid: IntArray): IntArray {
        val mirrored = IntArray(9)
        val mirrorMap = intArrayOf(2, 1, 0, 5, 4, 3, 8, 7, 6)
        for (i in 0 until 9) {
            mirrored[i] = grid[mirrorMap[i]]
        }
        return mirrored
    }

    /**
     * Mirrors grid vertically (top <-> bottom).
     * 0 1 2     6 7 8
     * 3 4 5  -> 3 4 5
     * 6 7 8     0 1 2
     */
    fun mirrorGridVertical(grid: IntArray): IntArray {
        val mirrored = IntArray(9)
        val mirrorMap = intArrayOf(6, 7, 8, 3, 4, 5, 0, 1, 2)
        for (i in 0 until 9) {
            mirrored[i] = grid[mirrorMap[i]]
        }
        return mirrored
    }

    /**
     * Formats combination pattern as a human readable string (e.g., "012-120-201").
     */
    fun formatPatternString(grid: IntArray): String {
        val sb = StringBuilder()
        for (i in 0 until 9) {
            if (i > 0 && i % 3 == 0) sb.append("-")
            sb.append(grid[i])
        }
        return sb.toString()
    }

    /**
     * Gives visually structured grids a higher priority when recommending
     * recipes. This is intentionally a heuristic: the game rules are not
     * known to the app, so it ranks human-friendly shapes rather than claiming
     * that a recipe will work.
     */
    fun recommendationScore(grid: IntArray): Int {
        if (grid.size != 9) return Int.MIN_VALUE

        val distinctValues = grid.toSet().size
        var score = when (distinctValues) {
            1 -> -1000 // Avoid recommending an empty/uniform board first.
            2 -> 12
            3 -> 18
            else -> 14
        }

        // Prefer contiguous areas and repeating runs.
        for (row in 0 until 3) {
            for (col in 0 until 2) {
                if (grid[row * 3 + col] == grid[row * 3 + col + 1]) score += 4
            }
        }
        for (col in 0 until 3) {
            for (row in 0 until 2) {
                if (grid[row * 3 + col] == grid[(row + 1) * 3 + col]) score += 4
            }
        }

        // Reward common visual symmetries: mirrors, diagonals and rotations.
        if (grid.contentEquals(mirrorGridHorizontal(grid))) score += 28
        if (grid.contentEquals(mirrorGridVertical(grid))) score += 28
        if (grid.contentEquals(diagonalMirror(grid, mainDiagonal = true))) score += 24
        if (grid.contentEquals(diagonalMirror(grid, mainDiagonal = false))) score += 24
        if (grid.contentEquals(rotateGrid90(grid))) score += 32
        if (grid.contentEquals(rotateGrid90(rotateGrid90(grid)))) score += 20

        // Repeated rows/columns often form recognizable stripes or blocks.
        for (a in 0 until 2) {
            if (grid.sliceArray(a * 3 until a * 3 + 3)
                    .contentEquals(grid.sliceArray((a + 1) * 3 until (a + 1) * 3 + 3))) {
                score += 14
            }
            if (intArrayOf(grid[a], grid[a + 3], grid[a + 6])
                    .contentEquals(intArrayOf(grid[a + 1], grid[a + 4], grid[a + 7]))) {
                score += 10
            }
        }
        return score
    }

    private fun diagonalMirror(grid: IntArray, mainDiagonal: Boolean): IntArray {
        val mirrored = IntArray(9)
        for (row in 0 until 3) {
            for (col in 0 until 3) {
                val sourceIndex = if (mainDiagonal) col * 3 + row else (2 - col) * 3 + (2 - row)
                mirrored[row * 3 + col] = grid[sourceIndex]
            }
        }
        return mirrored
    }
}
