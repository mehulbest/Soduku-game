package com.example.sudokugame

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sudokugame.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sudokuCells: Array<Array<EditText>>

    private val generator = SudokuGenerator()
    private val solver = SudokuSolver()

    private lateinit var board: Array<IntArray>
    private val history = mutableListOf<Array<IntArray>>() // Stack to store board states
    private var hintsLeft = 10 // Number of hints left
    private val hintGiven = mutableSetOf<Pair<Int, Int>>() // Track cells where hints have been provided

    private var timerHandler: Handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var secondsElapsed = 0
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createSudokuGrid()
        setupDifficultySpinner()
        generateNewPuzzle()

        binding.hintButton.setOnClickListener {
            if (hintsLeft > 0) {
                provideHint()
            } else {
                Toast.makeText(this, "No hints left!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.resetButton.setOnClickListener {
            generateNewPuzzle() // This now includes resetting and starting the timer
        }

        binding.solveButton.setOnClickListener {
            checkSolution()
            stopTimer() // Stop the timer when solving
        }

        binding.eraseAllButton.setOnClickListener {
            eraseAll()
        }

        binding.solutionButton.setOnClickListener {
            showSolution()
            stopTimer() // Optional: Stop the timer when showing solution
        }

        binding.homeButton.setOnClickListener {
            // Navigate back to StartActivity (main menu)
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            finish() // Optional: Finish current activity to remove it from the back stack
        }

        startTimer() // Start the timer when the activity is created
    }

    private fun setupDifficultySpinner() {
        val difficultyOptions = listOf("Easy", "Medium", "Hard")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficultyOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.difficultySpinner.adapter = adapter
        binding.difficultySpinner.setSelection(1) // Default to Medium difficulty

        binding.difficultySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                generateNewPuzzle() // Generate a new puzzle when the difficulty changes
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Optionally handle the case when no item is selected
            }
        }
    }

    private fun createSudokuGrid() {
        sudokuCells = Array(9) { row ->
            Array(9) { col ->
                val cell = EditText(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        columnSpec = GridLayout.spec(col, 1f)
                        rowSpec = GridLayout.spec(row, 1f)
                    }
                    inputType = InputType.TYPE_CLASS_NUMBER
                    textSize = 18f
                    setPadding(16, 16, 16, 16)
                    background = getCellBackground(row, col)

                    addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            val text = s.toString()
                            if (text.isNotEmpty() && (text.toIntOrNull() ?: 0) !in 1..9) {
                                s?.clear()
                            }
                        }

                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })
                }
                cell.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        saveBoardState()
                    }
                }
                binding.sudokuGrid.addView(cell)
                cell
            }
        }
    }

    private fun getCellBackground(row: Int, col: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.setStroke(1, Color.BLACK) // Set border color and width

        // Change border thickness for the 3x3 boxes
        if (row % 3 == 0) drawable.setStroke(2, Color.BLACK)
        if (col % 3 == 0) drawable.setStroke(2, Color.BLACK)

        return drawable
    }

    private fun generateNewPuzzle() {
        val difficulty = binding.difficultySpinner.selectedItem.toString()
        board = generator.generatePuzzle(difficulty)
        updateUIWithBoard()
        history.clear() // Clear history on new puzzle generation
        hintsLeft = 10 // Reset hints to 10 for new game
        binding.hintButton.text = "Hint ($hintsLeft)"
        hintGiven.clear() // Clear previous hints
        resetTimer() // Restart the timer for a new game
    }

    private fun updateUIWithBoard() {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                sudokuCells[row][col].setText(board[row][col].takeIf { it != 0 }?.toString() ?: "")
                sudokuCells[row][col].isEnabled = board[row][col] == 0
                sudokuCells[row][col].setBackgroundColor(Color.WHITE) // Reset background color
            }
        }
    }

    private fun saveBoardState() {
        val boardCopy = Array(9) { IntArray(9) }
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                boardCopy[row][col] = sudokuCells[row][col].text.toString().toIntOrNull() ?: 0
            }
        }
        history.add(boardCopy)
    }

    private fun undoLastAction() {
        if (history.isNotEmpty()) {
            val previousState = history.removeAt(history.size - 1)
            for (row in 0 until 9) {
                for (col in 0 until 9) {
                    sudokuCells[row][col].setText(previousState[row][col].takeIf { it != 0 }?.toString() ?: "")
                }
            }
        } else {
            Toast.makeText(this, "Nothing to undo!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun provideHint() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (sudokuCells[row][col].text.isEmpty() && !hintGiven.contains(Pair(row, col))) {
                    emptyCells.add(Pair(row, col))
                }
            }
        }

        if (emptyCells.isNotEmpty()) {
            val (row, col) = emptyCells.random()
            val correctValue = solver.getCorrectValue(board, row, col)
            sudokuCells[row][col].setText(correctValue.toString())
            hintGiven.add(Pair(row, col))
            hintsLeft--
            binding.hintButton.text = "Hint ($hintsLeft)"
        } else {
            Toast.makeText(this, "No empty cells available for hint!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkSolution() {
        // Create a copy of the current board and solve it
        val solvedBoard = Array(9) { IntArray(9) }
        for (i in board.indices) {
            solvedBoard[i] = board[i].copyOf()
        }

        if (!solver.solve(solvedBoard)) {
            Toast.makeText(this, "No solution exists for this puzzle!", Toast.LENGTH_SHORT).show()
            return
        }

        var isSolved = true

        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val userValue = sudokuCells[row][col].text.toString().toIntOrNull() ?: 0
                val correctValue = solvedBoard[row][col]

                if (userValue != correctValue) {
                    sudokuCells[row][col].setBackgroundColor(Color.RED) // Incorrect
                    isSolved = false
                } else {
                    sudokuCells[row][col].setBackgroundColor(Color.GREEN) // Correct
                }
            }
        }

        if (isSolved) {
            Toast.makeText(this, "Congratulations! Puzzle Solved!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "There are some mistakes. Keep trying!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun eraseAll() {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col] == 0) {
                    sudokuCells[row][col].setText("")
                }
            }
        }
    }

    private fun showSolution() {
        val solvedBoard = Array(9) { IntArray(9) }
        for (i in board.indices) {
            solvedBoard[i] = board[i].copyOf()
        }

        if (solver.solve(solvedBoard)) {
            // Display the solved board
            for (row in 0 until 9) {
                for (col in 0 until 9) {
                    if (board[row][col] == 0) {
                        sudokuCells[row][col].setText(solvedBoard[row][col].toString())
                        sudokuCells[row][col].setBackgroundColor(Color.YELLOW)
                    } else {
                        sudokuCells[row][col].setText(board[row][col].toString())
                        sudokuCells[row][col].setBackgroundColor(Color.LTGRAY)
                    }
                }
            }
        } else {
            Toast.makeText(this, "No solution exists for this puzzle!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTimer() {
        if (isTimerRunning) return // Prevent multiple instances of the timer from running

        isTimerRunning = true
        timerRunnable = object : Runnable {
            override fun run() {
                if (!isTimerRunning) return

                secondsElapsed++
                val minutes = secondsElapsed / 60
                val seconds = secondsElapsed % 60
                binding.timerTextView.text = String.format("%02d:%02d", minutes, seconds)

                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.postDelayed(timerRunnable!!, 1000) // Start after 1 second delay
    }

    private fun stopTimer() {
        if (isTimerRunning) {
            timerHandler.removeCallbacks(timerRunnable!!)
            isTimerRunning = false
        }
    }

    private fun resetTimer() {
        stopTimer() // Ensure any existing timer is stopped
        secondsElapsed = 0 // Reset the seconds elapsed
        binding.timerTextView.text = String.format("%02d:%02d", 0, 0) // Reset timer display
        startTimer() // Start the timer again
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer() // Stop the timer to prevent memory leaks
    }
}
