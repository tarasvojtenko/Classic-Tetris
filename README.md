🧩 Classic Tetris – Multi‑Language Edition
A fully functional classic Tetris implementation in 7 programming languages.
Play the iconic block‑stacking game in your terminal with smooth controls, scoring, levels, next‑piece preview, and pause functionality.
Built in Python, Go, JavaScript (Node.js), C#, Java, Ruby, and Swift – perfect for learning game loops, input handling, and data structures.

✨ Features
Standard 10×20 playfield – with wall‑kicks for rotations.

7 classic tetrominoes – I, O, T, S, Z, J, L (with random bag generator).

Real‑time controls – Left/Right to move, Up to rotate, Down to soft drop, Space to hard drop.

Scoring system – 100×lines cleared, with level progression (every 10 lines).

Next piece preview – see the upcoming tetromino.

Pause/Resume – press P to toggle.

Game over detection – when a piece cannot spawn.

Cross‑platform – runs in any terminal (Windows, macOS, Linux).

🗂 Languages & Files
Language	File
Python	tetris.py
Go	tetris.go
JavaScript (Node)	tetris.js
C#	Tetris.cs
Java	Tetris.java
Ruby	tetris.rb
Swift	tetris.swift
🚀 How to Run
Each file is standalone – run it with the appropriate interpreter/compiler.
Dependencies:

Python: curses (built‑in on Unix; on Windows install windows-curses via pip).

Go: uses tcell – go get github.com/gdamore/tcell (or fallback to simpler console input).

JavaScript: uses readline and keypress – npm install keypress.

C#: built‑in Console and Console.ReadKey.

Java: built‑in java.awt and javax.swing (or console).

Ruby: built‑in io/console.

Swift: built‑in Foundation and terminal handling.

Language	Command
Python	python tetris.py
Go	go run tetris.go
JavaScript	node tetris.js
C#	dotnet run (or csc Tetris.cs && Tetris.exe)
Java	javac Tetris.java && java Tetris
Ruby	ruby tetris.rb
Swift	swift tetris.swift
🎮 Controls
Arrow Left / A – move left

Arrow Right / D – move right

Arrow Down / S – soft drop

Arrow Up / W – rotate

Space – hard drop

P – pause / resume

Q – quit

📜 License
MIT – use freely.

