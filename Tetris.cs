// Tetris.cs
using System;
using System.Collections.Generic;
using System.Threading;

class Tetris
{
    const int Width = 10;
    const int Height = 20;
    static int[,] board = new int[Height, Width];
    static int score, lines, level;
    static bool gameOver, paused;
    static double fallSpeed = 0.5;
    static double fallTime = 0;
    static Random rand = new Random();
    static List<string> bag = new List<string>();
    static Piece current, next;
    static DateTime lastDrop = DateTime.Now;

    struct Piece
    {
        public int[,] matrix;
        public int x, y;
        public int color;
        public string name;
    }

    static Dictionary<string, int[,]> shapes = new Dictionary<string, int[,]>
    {
        {"I", new int[,]{{1,1,1,1}}},
        {"O", new int[,]{{1,1},{1,1}}},
        {"T", new int[,]{{0,1,0},{1,1,1}}},
        {"S", new int[,]{{0,1,1},{1,1,0}}},
        {"Z", new int[,]{{1,1,0},{0,1,1}}},
        {"J", new int[,]{{1,0,0},{1,1,1}}},
        {"L", new int[,]{{0,0,1},{1,1,1}}}
    };
    static string[] shapeNames = new[] { "I", "O", "T", "S", "Z", "J", "L" };

    static Piece GetPiece()
    {
        if (bag.Count == 0)
        {
            bag = new List<string>(shapeNames);
            for (int i = bag.Count - 1; i > 0; i--)
            {
                int j = rand.Next(i + 1);
                string tmp = bag[i]; bag[i] = bag[j]; bag[j] = tmp;
            }
        }
        string name = bag[0];
        bag.RemoveAt(0);
        int[,] matrix = (int[,])shapes[name].Clone();
        int color = rand.Next(1, 8);
        return new Piece
        {
            matrix = matrix,
            x = Width / 2 - matrix.GetLength(1) / 2,
            y = 0,
            color = color,
            name = name
        };
    }

    static bool CheckCollision(int[,] matrix, int x, int y)
    {
        for (int row = 0; row < matrix.GetLength(0); row++)
            for (int col = 0; col < matrix.GetLength(1); col++)
                if (matrix[row, col] != 0)
                {
                    int bx = x + col;
                    int by = y + row;
                    if (bx < 0 || bx >= Width || by >= Height || by < 0) return true;
                    if (by >= 0 && board[by, bx] != 0) return true;
                }
        return false;
    }

    static void LockPiece()
    {
        var p = current;
        for (int row = 0; row < p.matrix.GetLength(0); row++)
            for (int col = 0; col < p.matrix.GetLength(1); col++)
                if (p.matrix[row, col] != 0)
                {
                    int bx = p.x + col;
                    int by = p.y + row;
                    if (by >= 0) board[by, bx] = p.color;
                }
        ClearLines();
        current = next;
        next = GetPiece();
        if (CheckCollision(current.matrix, current.x, current.y))
            gameOver = true;
    }

    static void ClearLines()
    {
        int cleared = 0;
        for (int y = Height - 1; y >= 0; y--)
        {
            bool full = true;
            for (int x = 0; x < Width; x++)
                if (board[y, x] == 0) { full = false; break; }
            if (full)
            {
                for (int yy = y; yy > 0; yy--)
                    for (int x = 0; x < Width; x++)
                        board[yy, x] = board[yy - 1, x];
                for (int x = 0; x < Width; x++) board[0, x] = 0;
                cleared++;
                y++; // re-check same row
            }
        }
        if (cleared > 0)
        {
            lines += cleared;
            score += cleared * 100;
            level = lines / 10 + 1;
            fallSpeed = Math.Max(0.1, 0.5 - (level - 1) * 0.04);
        }
    }

    static void Rotate()
    {
        var p = current;
        int rows = p.matrix.GetLength(0);
        int cols = p.matrix.GetLength(1);
        int[,] rotated = new int[cols, rows];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                rotated[j, rows - 1 - i] = p.matrix[i, j];
        if (!CheckCollision(rotated, p.x, p.y))
            p.matrix = rotated;
    }

    static bool Move(int dx, int dy)
    {
        var p = current;
        if (!CheckCollision(p.matrix, p.x + dx, p.y + dy))
        {
            p.x += dx;
            p.y += dy;
            return true;
        }
        if (dy == 1) LockPiece();
        return false;
    }

    static void HardDrop()
    {
        while (Move(0, 1)) { }
    }

    static void Update()
    {
        if (paused || gameOver) return;
        var now = DateTime.Now;
        double elapsed = (now - lastDrop).TotalSeconds;
        if (elapsed >= fallSpeed)
        {
            lastDrop = now;
            if (!Move(0, 1)) LockPiece();
        }
    }

    static void Draw()
    {
        Console.Clear();
        Console.WriteLine($"Score: {score}  Lines: {lines}  Level: {level}");
        for (int y = 0; y < Height; y++)
        {
            Console.Write('|');
            for (int x = 0; x < Width; x++)
                Console.Write(board[y, x] != 0 ? "[]" : "  ");
            Console.WriteLine('|');
        }
        // Draw current piece
        if (current.matrix != null)
        {
            var p = current;
            for (int row = 0; row < p.matrix.GetLength(0); row++)
                for (int col = 0; col < p.matrix.GetLength(1); col++)
                    if (p.matrix[row, col] != 0)
                    {
                        int bx = p.x + col;
                        int by = p.y + row;
                        Console.SetCursorPosition(bx * 2 + 2, by + 3);
                        Console.Write("[]");
                    }
        }
        Console.SetCursorPosition(Width * 2 + 6, 3);
        Console.Write("Next:");
        if (next.matrix != null)
        {
            for (int row = 0; row < next.matrix.GetLength(0); row++)
                for (int col = 0; col < next.matrix.GetLength(1); col++)
                    if (next.matrix[row, col] != 0)
                    {
                        Console.SetCursorPosition(Width * 2 + 8 + col * 2, 4 + row);
                        Console.Write("[]");
                    }
        }
        Console.SetCursorPosition(0, Height + 4);
        Console.WriteLine("Controls: ←/→ move, ↑ rotate, ↓ soft drop, Space hard drop, P pause, Q quit");
        if (gameOver) Console.WriteLine("GAME OVER! Press Q to quit.");
        else if (paused) Console.WriteLine("PAUSED. Press P to resume.");
    }

    static void Main()
    {
        Console.CursorVisible = false;
        current = GetPiece();
        next = GetPiece();
        lastDrop = DateTime.Now;
        while (!gameOver)
        {
            if (Console.KeyAvailable)
            {
                var key = Console.ReadKey(true).Key;
                if (key == ConsoleKey.Q) break;
                if (key == ConsoleKey.P) { paused = !paused; continue; }
                if (paused || gameOver) continue;
                if (key == ConsoleKey.LeftArrow) Move(-1, 0);
                else if (key == ConsoleKey.RightArrow) Move(1, 0);
                else if (key == ConsoleKey.DownArrow) Move(0, 1);
                else if (key == ConsoleKey.UpArrow) Rotate();
                else if (key == ConsoleKey.Spacebar) HardDrop();
            }
            Update();
            Draw();
            Thread.Sleep(16);
        }
        Console.SetCursorPosition(0, Height + 8);
        Console.WriteLine("Game Over. Press any key to exit.");
        Console.ReadKey();
    }
}
