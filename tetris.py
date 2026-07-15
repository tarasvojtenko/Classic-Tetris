# tetris.py
import curses
import random
import time

# Tetromino shapes
SHAPES = {
    'I': [[1,1,1,1]],
    'O': [[1,1],[1,1]],
    'T': [[0,1,0],[1,1,1]],
    'S': [[0,1,1],[1,1,0]],
    'Z': [[1,1,0],[0,1,1]],
    'J': [[1,0,0],[1,1,1]],
    'L': [[0,0,1],[1,1,1]]
}
COLORS = {'I': 1, 'O': 2, 'T': 3, 'S': 4, 'Z': 5, 'J': 6, 'L': 7}

class Tetris:
    def __init__(self, stdscr):
        self.stdscr = stdscr
        self.width = 10
        self.height = 20
        self.board = [[0]*self.width for _ in range(self.height)]
        self.score = 0
        self.lines = 0
        self.level = 1
        self.game_over = False
        self.paused = False
        self.fall_time = 0
        self.fall_speed = 0.5  # seconds per drop
        self.current_piece = None
        self.next_piece = None
        self.bag = []
        self.init_colors()
        self.spawn_piece()
        self.next_piece = self.get_piece()

    def init_colors(self):
        curses.start_color()
        curses.init_pair(1, curses.COLOR_CYAN, curses.COLOR_BLACK)
        curses.init_pair(2, curses.COLOR_YELLOW, curses.COLOR_BLACK)
        curses.init_pair(3, curses.COLOR_MAGENTA, curses.COLOR_BLACK)
        curses.init_pair(4, curses.COLOR_GREEN, curses.COLOR_BLACK)
        curses.init_pair(5, curses.COLOR_RED, curses.COLOR_BLACK)
        curses.init_pair(6, curses.COLOR_BLUE, curses.COLOR_BLACK)
        curses.init_pair(7, curses.COLOR_WHITE, curses.COLOR_BLACK)

    def get_piece(self):
        if not self.bag:
            self.bag = list(SHAPES.keys())
            random.shuffle(self.bag)
        shape = self.bag.pop()
        return {'shape': shape, 'matrix': SHAPES[shape], 'x': self.width//2 - len(SHAPES[shape][0])//2, 'y': 0, 'color': COLORS[shape]}

    def spawn_piece(self):
        self.current_piece = self.next_piece or self.get_piece()
        self.next_piece = self.get_piece()
        if self.check_collision(self.current_piece['matrix'], self.current_piece['x'], self.current_piece['y']):
            self.game_over = True

    def check_collision(self, matrix, x, y):
        for row_idx, row in enumerate(matrix):
            for col_idx, cell in enumerate(row):
                if cell:
                    board_x = x + col_idx
                    board_y = y + row_idx
                    if board_x < 0 or board_x >= self.width or board_y >= self.height or board_y < 0:
                        return True
                    if board_y >= 0 and self.board[board_y][board_x]:
                        return True
        return False

    def lock_piece(self):
        piece = self.current_piece
        for row_idx, row in enumerate(piece['matrix']):
            for col_idx, cell in enumerate(row):
                if cell:
                    board_x = piece['x'] + col_idx
                    board_y = piece['y'] + row_idx
                    if board_y >= 0:
                        self.board[board_y][board_x] = piece['color']
        self.clear_lines()
        self.spawn_piece()

    def clear_lines(self):
        lines_cleared = 0
        for y in range(self.height-1, -1, -1):
            if all(self.board[y]):
                del self.board[y]
                self.board.insert(0, [0]*self.width)
                lines_cleared += 1
        if lines_cleared:
            self.lines += lines_cleared
            self.score += lines_cleared * 100
            self.level = self.lines // 10 + 1
            self.fall_speed = max(0.1, 0.5 - (self.level-1) * 0.04)

    def rotate(self):
        piece = self.current_piece
        matrix = piece['matrix']
        rotated = [list(row) for row in zip(*matrix[::-1])]  # clockwise
        if not self.check_collision(rotated, piece['x'], piece['y']):
            piece['matrix'] = rotated

    def move(self, dx, dy):
        piece = self.current_piece
        if not self.check_collision(piece['matrix'], piece['x']+dx, piece['y']+dy):
            piece['x'] += dx
            piece['y'] += dy
            return True
        elif dy == 1:  # hard drop or landing
            self.lock_piece()
        return False

    def hard_drop(self):
        while not self.move(0, 1):
            pass

    def update(self):
        if self.paused or self.game_over:
            return
        self.fall_time += 0.016  # approx 60 FPS
        if self.fall_time >= self.fall_speed:
            self.fall_time = 0
            if not self.move(0, 1):
                self.lock_piece()

    def draw(self):
        self.stdscr.clear()
        h, w = self.stdscr.getmaxyx()
        # Draw board
        for y in range(self.height):
            for x in range(self.width):
                color = self.board[y][x]
                if color:
                    self.stdscr.addch(y+1, x*2+2, '  ', curses.color_pair(color))
                else:
                    self.stdscr.addch(y+1, x*2+2, '..')
        # Draw current piece
        if self.current_piece:
            piece = self.current_piece
            for row_idx, row in enumerate(piece['matrix']):
                for col_idx, cell in enumerate(row):
                    if cell:
                        board_x = (piece['x'] + col_idx) * 2 + 2
                        board_y = piece['y'] + row_idx + 1
                        if 0 <= board_y < self.height+1 and 0 <= board_x < self.width*2+2:
                            self.stdscr.addch(board_y, board_x, '  ', curses.color_pair(piece['color']))
        # Draw info
        self.stdscr.addstr(0, 2, f"Score: {self.score}  Lines: {self.lines}  Level: {self.level}")
        # Next piece
        if self.next_piece:
            self.stdscr.addstr(1, self.width*2+5, "Next:")
            for row_idx, row in enumerate(self.next_piece['matrix']):
                for col_idx, cell in enumerate(row):
                    if cell:
                        self.stdscr.addch(2+row_idx, self.width*2+7+col_idx*2, '  ', curses.color_pair(self.next_piece['color']))
        # Controls
        self.stdscr.addstr(self.height+2, 2, "Controls: ←/→ move, ↑ rotate, ↓ soft drop, Space hard drop, P pause, Q quit")
        if self.game_over:
            self.stdscr.addstr(self.height+4, 2, "GAME OVER! Press Q to quit.")
        elif self.paused:
            self.stdscr.addstr(self.height+4, 2, "PAUSED. Press P to resume.")
        self.stdscr.refresh()

    def run(self):
        self.stdscr.nodelay(1)
        self.stdscr.timeout(16)  # ~60 FPS
        while not self.game_over:
            key = self.stdscr.getch()
            if key == ord('q') or key == ord('Q'):
                break
            elif key == ord('p') or key == ord('P'):
                self.paused = not self.paused
            if not self.paused and not self.game_over:
                if key == curses.KEY_LEFT or key == ord('a') or key == ord('A'):
                    self.move(-1, 0)
                elif key == curses.KEY_RIGHT or key == ord('d') or key == ord('D'):
                    self.move(1, 0)
                elif key == curses.KEY_DOWN or key == ord('s') or key == ord('S'):
                    self.move(0, 1)
                elif key == curses.KEY_UP or key == ord('w') or key == ord('W'):
                    self.rotate()
                elif key == ord(' '):
                    self.hard_drop()
            self.update()
            self.draw()
        curses.endwin()

def main(stdscr):
    game = Tetris(stdscr)
    game.run()

if __name__ == "__main__":
    curses.wrapper(main)
