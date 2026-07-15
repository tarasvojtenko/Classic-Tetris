# tetris.rb
require 'io/console'
require 'timeout'
require 'set'

WIDTH = 10
HEIGHT = 20
SHAPES = {
  'I' => [[1,1,1,1]],
  'O' => [[1,1],[1,1]],
  'T' => [[0,1,0],[1,1,1]],
  'S' => [[0,1,1],[1,1,0]],
  'Z' => [[1,1,0],[0,1,1]],
  'J' => [[1,0,0],[1,1,1]],
  'L' => [[0,0,1],[1,1,1]]
}
SHAPE_NAMES = SHAPES.keys.freeze

class Tetris
  def initialize
    @board = Array.new(HEIGHT) { Array.new(WIDTH, 0) }
    @score = 0
    @lines = 0
    @level = 1
    @fall_speed = 0.5
    @game_over = false
    @paused = false
    @bag = []
    @current = get_piece
    @next = get_piece
    @last_drop = Time.now
    @running = true
    @input_thread = nil
  end

  def get_piece
    if @bag.empty?
      @bag = SHAPE_NAMES.shuffle
    end
    name = @bag.pop
    matrix = SHAPES[name].map(&:dup)
    color = rand(1..7)
    Piece.new(matrix, WIDTH/2 - matrix[0].length/2, 0, color, name)
  end

  Piece = Struct.new(:matrix, :x, :y, :color, :name)

  def check_collision(matrix, x, y)
    matrix.each_with_index do |row, row_idx|
      row.each_with_index do |cell, col_idx|
        if cell != 0
          bx = x + col_idx
          by = y + row_idx
          return true if bx < 0 || bx >= WIDTH || by >= HEIGHT || by < 0
          return true if by >= 0 && @board[by][bx] != 0
        end
      end
    end
    false
  end

  def lock_piece
    p = @current
    p.matrix.each_with_index do |row, row_idx|
      row.each_with_index do |cell, col_idx|
        if cell != 0
          bx = p.x + col_idx
          by = p.y + row_idx
          @board[by][bx] = p.color if by >= 0
        end
      end
    end
    clear_lines
    @current = @next
    @next = get_piece
    if check_collision(@current.matrix, @current.x, @current.y)
      @game_over = true
    end
  end

  def clear_lines
    cleared = 0
    y = HEIGHT - 1
    while y >= 0
      if @board[y].all? { |cell| cell != 0 }
        @board.delete_at(y)
        @board.unshift(Array.new(WIDTH, 0))
        cleared += 1
        y += 1
      end
      y -= 1
    end
    if cleared > 0
      @lines += cleared
      @score += cleared * 100
      @level = @lines / 10 + 1
      @fall_speed = [0.1, 0.5 - (@level - 1) * 0.04].max
    end
  end

  def rotate
    p = @current
    rows = p.matrix.length
    cols = p.matrix[0].length
    rotated = Array.new(cols) { Array.new(rows, 0) }
    rows.times do |i|
      cols.times do |j|
        rotated[j][rows - 1 - i] = p.matrix[i][j]
      end
    end
    unless check_collision(rotated, p.x, p.y)
      p.matrix = rotated
    end
  end

  def move(dx, dy)
    p = @current
    unless check_collision(p.matrix, p.x + dx, p.y + dy)
      p.x += dx
      p.y += dy
      return true
    end
    if dy == 1
      lock_piece
    end
    false
  end

  def hard_drop
    while move(0, 1)
    end
  end

  def draw
    system('clear') || system('cls')
    puts "Score: #{@score}  Lines: #{@lines}  Level: #{@level}"
    HEIGHT.times do |y|
      print '|'
      WIDTH.times do |x|
        print @board[y][x] != 0 ? '[]' : '  '
      end
      puts '|'
    end
    # Draw current piece overlay
    if @current
      p = @current
      p.matrix.each_with_index do |row, row_idx|
        row.each_with_index do |cell, col_idx|
          if cell != 0
            bx = (p.x + col_idx) * 2 + 2
            by = p.y + row_idx + 3
            print "\e[#{by};#{bx}H[]"
          end
        end
      end
    end
    # Next piece
    print "\e[3;#{WIDTH*2+6}HNext:"
    if @next
      @next.matrix.each_with_index do |row, row_idx|
        row.each_with_index do |cell, col_idx|
          if cell != 0
            print "\e[#{4+row_idx};#{WIDTH*2+8+col_idx*2}H[]"
          end
        end
      end
    end
    print "\e[#{HEIGHT+4};0HControls: ←/→ move, ↑ rotate, ↓ soft drop, Space hard drop, P pause, Q quit"
    if @game_over
      print "\e[#{HEIGHT+6};0HGAME OVER! Press Q to quit."
    elsif @paused
      print "\e[#{HEIGHT+6};0HPAUSED. Press P to resume."
    end
  end

  def handle_input
    Thread.new do
      while @running
        char = STDIN.getch
        case char
        when 'q', 'Q' then @running = false
        when 'p', 'P' then @paused = !@paused
        else
          next if @paused || @game_over
          case char
          when "\e" # escape sequence
            c = STDIN.read_nonblock(2) rescue nil
            if c == '[A' then rotate
            elsif c == '[B' then move(0, 1)
            elsif c == '[C' then move(1, 0)
            elsif c == '[D' then move(-1, 0)
            end
          when ' ' then hard_drop
          end
        end
      end
    end
  end

  def run
    @input_thread = handle_input
    while @running && !@game_over
      now = Time.now
      if now - @last_drop >= @fall_speed
        @last_drop = now
        move(0, 1) unless @paused
      end
      draw
      sleep 0.016
    end
    puts "\e[#{HEIGHT+8};0HGoodbye!"
  end
end

Tetris.new.run
