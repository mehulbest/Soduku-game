package com.example.sudokugame

class SudokuSolver {

    // Checks if the entire Sudoku board is solved
    fun isSolved(board: Array<IntArray>): Boolean {
        // Check each cell for validity
        return board.all { row -> row.all { it != 0 } } && isValid(board)
    }

    // Checks if the board configuration is valid
    private fun isValid(board: Array<IntArray>): Boolean {
        for (row in 0 until 9) {
            if (!isValidUnit(board[row])) return false
        }
        for (col in 0 until 9) {
            if (!isValidUnit(board.map { it[col] }.toIntArray())) return false
        }
        for (boxRow in 0 until 9 step 3) {
            for (boxCol in 0 until 9 step 3) {
                val box = IntArray(9)
                var index = 0
                for (i in 0 until 3) {
                    for (j in 0 until 3) {
                        box[index++] = board[boxRow + i][boxCol + j]
                    }
                }
                if (!isValidUnit(box)) return false
            }
        }
        return true
    }

    // Checks if a unit (row, column, or 3x3 box) is valid
    private fun isValidUnit(unit: IntArray): Boolean {
        val seen = BooleanArray(10)
        for (num in unit) {
            if (num != 0) {
                if (seen[num]) return false
                seen[num] = true
            }
        }
        return true
    }

    // Solves the Sudoku puzzle
    fun solve(board: Array<IntArray>): Boolean {
        val emptyCell = findEmptyCell(board) ?: return true

        val (row, col) = emptyCell
        for (num in 1..9) {
            if (isSafe(board, row, col, num)) {
                board[row][col] = num
                if (solve(board)) return true
                board[row][col] = 0
            }
        }
        return false
    }

    // Finds the first empty cell (cell with 0)
    private fun findEmptyCell(board: Array<IntArray>): Pair<Int, Int>? {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col] == 0) return Pair(row, col)
            }
        }
        return null
    }

    // Checks if it's safe to place a number in the cell
    private fun isSafe(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (i in 0 until 9) {
            if (board[row][i] == num || board[i][col] == num) return false
        }
        val startRow = row - row % 3
        val startCol = col - col % 3
        for (i in startRow until startRow + 3) {
            for (j in startCol until startCol + 3) {
                if (board[i][j] == num) return false
            }
        }
        return true
    }

    // Solves a specific cell based on the current board state
    fun getCorrectValue(board: Array<IntArray>, row: Int, col: Int): Int {
        val boardCopy = board.map { it.copyOf() }.toTypedArray()
        if (solve(boardCopy)) {
            return boardCopy[row][col]
        } else {
            throw IllegalStateException("No solution exists for the given board.")
        }
    }
}
