package com.example.sudokugame

import kotlin.random.Random

class SudokuGenerator {

    private val size = 9
    private val boxSize = 3

    // Generates a Sudoku puzzle based on the selected difficulty level
    fun generatePuzzle(difficulty: String): Array<IntArray> {
        val board = Array(size) { IntArray(size) }
        fillBoard(board)
        removeCells(board, difficulty)
        return board
    }

    // Fills the board with a valid Sudoku solution
    private fun fillBoard(board: Array<IntArray>): Boolean {
        val emptyCell = findEmptyCell(board) ?: return true
        val (row, col) = emptyCell

        val numbers = (1..size).toList().shuffled()
        for (num in numbers) {
            if (isValid(board, row, col, num)) {
                board[row][col] = num
                if (fillBoard(board)) return true
                board[row][col] = 0
            }
        }
        return false
    }

    // Finds the next empty cell on the board
    private fun findEmptyCell(board: Array<IntArray>): Pair<Int, Int>? {
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (board[row][col] == 0) return Pair(row, col)
            }
        }
        return null
    }

    // Checks if placing 'num' at (row, col) is valid
    private fun isValid(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        return !isInRow(board, row, num) &&
                !isInCol(board, col, num) &&
                !isInBox(board, row, col, num)
    }

    // Checks if 'num' is in the specified row
    private fun isInRow(board: Array<IntArray>, row: Int, num: Int) = board[row].contains(num)

    // Checks if 'num' is in the specified column
    private fun isInCol(board: Array<IntArray>, col: Int, num: Int) = board.any { it[col] == num }

    // Checks if 'num' is in the 3x3 box containing (row, col)
    private fun isInBox(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        val boxStartRow = row - row % boxSize
        val boxStartCol = col - col % boxSize
        for (r in boxStartRow until boxStartRow + boxSize) {
            for (c in boxStartCol until boxStartCol + boxSize) {
                if (board[r][c] == num) return true
            }
        }
        return false
    }

    // Removes cells based on difficulty level
    private fun removeCells(board: Array<IntArray>, difficulty: String) {
        val cellsToRemove = when (difficulty) {
            "Easy" -> 36
            "Medium" -> 30
            "Hard" -> 24
            else -> 30 // Default difficulty
        }
        var count = cellsToRemove

        while (count > 0) {
            val row = Random.nextInt(size)
            val col = Random.nextInt(size)
            if (board[row][col] != 0) {
                board[row][col] = 0
                count--
            }
        }
    }
}
