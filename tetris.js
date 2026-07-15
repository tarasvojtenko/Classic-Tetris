// tetris.js
const readline = require('readline');
const { createInterface } = require('readline');

const W = 10, H = 20;
const SHAPES = {
  I: [[1,1,1,1]],
  O: [[1,1],[1,1]],
  T: [[0,1,0],[1,1,1]],
  S: [[0,1,1],[1,1,0]],
  Z: [[1,1,0],[0,1,1]],
  J: [[1,0,0],[1,1,1]],
  L: [[0,0,1],[1,1,1]]
};
const SHAPE_NAMES = ['I','O','T','S','Z','J','L'];

class Tetris {
  constructor() {
    this.board = Array.from({length: H}, () => Array(W).fill(0));
    this.score = 0;
    this.lines = 0;
    this.level = 1;
    this.fallSpeed = 500; // ms
    this.gameOver = false;
    this.paused = false;
    this.bag = [];
    this.current = this.getPiece();
    this.next = this.getPiece();
    this.lastDrop = Date.now();
    this.rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout
    });
    readline.emitKeypressEvents(process.stdin);
    process.stdin.setRawMode(true);
    process.stdin.on('keypress', (str, key) => this.handleKey(str, key));
  }

  getPiece() {
    if (this.bag.length === 0) {
      this.bag = [...SHAPE_NAMES];
      for (let i = this.bag.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [this.bag[i], this.bag[j]] = [this.bag[j], this.bag[i]];
      }
    }
    const name = this.bag.pop();
    const matrix = SHAPES[name].map(row => [...row]);
    const color = Math.floor(Math.random() * 7) + 1;
    return { name, matrix, x: Math.floor((W - matrix[0].length) / 2), y: 0, color };
  }

  checkCollision(matrix, x, y) {
    for (let row = 0; row < matrix.length; row++) {
      for (let col = 0; col < matrix[0].length; col++) {
        if (matrix[row][col]) {
          const bx = x + col;
          const by = y + row;
          if (bx < 0 || bx >= W || by >= H || by < 0) return true;
          if (by >= 0 && this.board[by][bx]) return true;
        }
      }
    }
    return false;
  }

  lockPiece() {
    const p = this.current;
    for (let row = 0; row < p.matrix.length; row++) {
      for (let col = 0; col < p.matrix[0].length; col++) {
        if (p.matrix[row][col]) {
          const bx = p.x + col;
          const by = p.y + row;
          if (by >= 0) this.board[by][bx] = p.color;
        }
      }
    }
    this.clearLines();
    this.current = this.next;
    this.next = this.getPiece();
    if (this.checkCollision(this.current.matrix, this.current.x, this.current.y)) {
      this.gameOver = true;
    }
  }

  clearLines() {
    let cleared = 0;
    for (let y = H - 1; y >= 0; y--) {
      if (this.board[y].every(cell => cell !== 0)) {
        this.board.splice(y, 1);
        this.board.unshift(Array(W).fill(0));
        cleared++;
        y++;
      }
    }
    if (cleared) {
      this.lines += cleared;
      this.score += cleared * 100;
      this.level = Math.floor(this.lines / 10) + 1;
      this.fallSpeed = Math.max(100, 500 - (this.level - 1) * 40);
    }
  }

  rotate() {
    const p = this.current;
    const rotated = p.matrix[0].map((_, idx) => p.matrix.map(row => row[idx]).reverse());
    if (!this.checkCollision(rotated, p.x, p.y)) {
      p.matrix = rotated;
    }
  }

  move(dx, dy) {
    const p = this.current;
    if (!this.checkCollision(p.matrix, p.x + dx, p.y + dy)) {
      p.x += dx;
      p.y += dy;
      return true;
    }
    if (dy === 1) this.lockPiece();
    return false;
  }

  hardDrop() {
    while (this.move(0, 1)) {}
  }

  update() {
    if (this.paused || this.gameOver) return;
    const now = Date.now();
    if (now - this.lastDrop >= this.fallSpeed) {
      this.lastDrop = now;
      if (!this.move(0, 1)) this.lockPiece();
    }
  }

  draw() {
    console.clear();
    console.log(`Score: ${this.score}  Lines: ${this.lines}  Level: ${this.level}`);
    for (let y = 0; y < H; y++) {
      let row = '|';
      for (let x = 0; x < W; x++) {
        row += this.board[y][x] ? '[]' : '  ';
      }
      row += '|';
      console.log(row);
    }
    // Draw current piece (overlay)
    if (this.current) {
      const p = this.current;
      for (let row = 0; row < p.matrix.length; row++) {
        for (let col = 0; col < p.matrix[0].length; col++) {
          if (p.matrix[row][col]) {
            const bx = p.x + col;
            const by = p.y + row;
            // Move cursor and print
            process.stdout.write(`\x1b[${by+3};${bx*2+2}H`);
            process.stdout.write('[]');
          }
        }
      }
    }
    // Next piece
    console.log(`\x1b[3;${W*2+6}HNext:`);
    if (this.next) {
      for (let row = 0; row < this.next.matrix.length; row++) {
        for (let col = 0; col < this.next.matrix[0].length; col++) {
          if (this.next.matrix[row][col]) {
            process.stdout.write(`\x1b[${4+row};${W*2+8+col*2}H`);
            process.stdout.write('[]');
          }
        }
      }
    }
    console.log(`\x1b[${H+4};0HControls: ←/→ move, ↑ rotate, ↓ soft drop, Space hard drop, P pause, Q quit`);
    if (this.gameOver) console.log(`\x1b[${H+6};0HGAME OVER! Press Q to quit.`);
    else if (this.paused) console.log(`\x1b[${H+6};0HPAUSED. Press P to resume.`);
  }

  handleKey(str, key) {
    if (key.ctrl && key.name === 'c') process.exit();
    if (key.name === 'q') process.exit();
    if (key.name === 'p') { this.paused = !this.paused; return; }
    if (this.paused || this.gameOver) return;
    const name = key.name;
    if (name === 'left') this.move(-1, 0);
    else if (name === 'right') this.move(1, 0);
    else if (name === 'down') this.move(0, 1);
    else if (name === 'up') this.rotate();
    else if (name === 'space') this.hardDrop();
  }

  run() {
    this.lastDrop = Date.now();
    const interval = setInterval(() => {
      this.update();
      this.draw();
      if (this.gameOver) clearInterval(interval);
    }, 16);
  }
}

const game = new Tetris();
game.run();
