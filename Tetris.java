// Tetris.java
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.util.List;

public class Tetris extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 10, HEIGHT = 20;
    private int[][] board = new int[HEIGHT][WIDTH];
    private int score, lines, level;
    private boolean gameOver, paused;
    private Timer timer;
    private int fallDelay = 500;
    private Piece current, next;
    private Random rand = new Random();
    private List<String> bag = new ArrayList<>();
    private String[] shapeNames = {"I","O","T","S","Z","J","L"};
    private Map<String, int[][]> shapes = new HashMap<>();
    private int blockSize = 30;

    class Piece {
        int[][] matrix;
        int x, y;
        int color;
        String name;
    }

    public Tetris() {
        setPreferredSize(new Dimension(WIDTH*blockSize + 200, HEIGHT*blockSize));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        shapes.put("I", new int[][]{{1,1,1,1}});
        shapes.put("O", new int[][]{{1,1},{1,1}});
        shapes.put("T", new int[][]{{0,1,0},{1,1,1}});
        shapes.put("S", new int[][]{{0,1,1},{1,1,0}});
        shapes.put("Z", new int[][]{{1,1,0},{0,1,1}});
        shapes.put("J", new int[][]{{1,0,0},{1,1,1}});
        shapes.put("L", new int[][]{{0,0,1},{1,1,1}});
        current = getPiece();
        next = getPiece();
        timer = new Timer(16, this);
        timer.start();
        gameOver = false;
        paused = false;
        level = 1;
        lines = 0;
        score = 0;
    }

    private Piece getPiece() {
        if (bag.isEmpty()) {
            bag = new ArrayList<>(Arrays.asList(shapeNames));
            Collections.shuffle(bag, rand);
        }
        String name = bag.remove(0);
        int[][] matrix = shapes.get(name);
        int[][] copy = new int[matrix.length][matrix[0].length];
        for (int i=0; i<matrix.length; i++) System.arraycopy(matrix[i], 0, copy[i], 0, matrix[0].length);
        Piece p = new Piece();
        p.matrix = copy;
        p.x = WIDTH/2 - copy[0].length/2;
        p.y = 0;
        p.color = rand.nextInt(7)+1;
        p.name = name;
        return p;
    }

    private boolean checkCollision(int[][] matrix, int x, int y) {
        for (int row=0; row<matrix.length; row++)
            for (int col=0; col<matrix[0].length; col++)
                if (matrix[row][col]!=0) {
                    int bx=x+col, by=y+row;
                    if (bx<0 || bx>=WIDTH || by>=HEIGHT || by<0) return true;
                    if (by>=0 && board[by][bx]!=0) return true;
                }
        return false;
    }

    private void lockPiece() {
        Piece p = current;
        for (int row=0; row<p.matrix.length; row++)
            for (int col=0; col<p.matrix[0].length; col++)
                if (p.matrix[row][col]!=0) {
                    int bx=p.x+col, by=p.y+row;
                    if (by>=0) board[by][bx]=p.color;
                }
        clearLines();
        current = next;
        next = getPiece();
        if (checkCollision(current.matrix, current.x, current.y)) gameOver = true;
    }

    private void clearLines() {
        int cleared=0;
        for (int y=HEIGHT-1; y>=0; y--) {
            boolean full=true;
            for (int x=0; x<WIDTH; x++) if (board[y][x]==0) { full=false; break; }
            if (full) {
                for (int yy=y; yy>0; yy--) board[yy]=board[yy-1].clone();
                board[0]=new int[WIDTH];
                cleared++;
                y++;
            }
        }
        if (cleared>0) {
            lines+=cleared;
            score+=cleared*100;
            level=lines/10+1;
            fallDelay=Math.max(100, 500-(level-1)*40);
        }
    }

    private void rotate() {
        Piece p = current;
        int rows=p.matrix.length, cols=p.matrix[0].length;
        int[][] rotated = new int[cols][rows];
        for (int i=0; i<rows; i++)
            for (int j=0; j<cols; j++)
                rotated[j][rows-1-i] = p.matrix[i][j];
        if (!checkCollision(rotated, p.x, p.y)) p.matrix = rotated;
    }

    private boolean move(int dx, int dy) {
        Piece p = current;
        if (!checkCollision(p.matrix, p.x+dx, p.y+dy)) {
            p.x += dx; p.y += dy; return true;
        }
        if (dy==1) lockPiece();
        return false;
    }

    private void hardDrop() { while (move(0,1)) {} }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!paused && !gameOver) {
            if (!move(0,1)) lockPiece();
        }
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key==KeyEvent.VK_Q) System.exit(0);
        if (key==KeyEvent.VK_P) { paused = !paused; return; }
        if (paused || gameOver) return;
        if (key==KeyEvent.VK_LEFT) move(-1,0);
        else if (key==KeyEvent.VK_RIGHT) move(1,0);
        else if (key==KeyEvent.VK_DOWN) move(0,1);
        else if (key==KeyEvent.VK_UP) rotate();
        else if (key==KeyEvent.VK_SPACE) hardDrop();
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw board
        for (int y=0; y<HEIGHT; y++)
            for (int x=0; x<WIDTH; x++) {
                if (board[y][x]!=0) {
                    g.setColor(new Color(board[y][x]*40, board[y][x]*60, board[y][x]*80));
                    g.fillRect(x*blockSize, y*blockSize, blockSize, blockSize);
                }
                g.setColor(Color.GRAY);
                g.drawRect(x*blockSize, y*blockSize, blockSize, blockSize);
            }
        // Draw current piece
        if (current!=null) {
            Piece p = current;
            for (int row=0; row<p.matrix.length; row++)
                for (int col=0; col<p.matrix[0].length; col++)
                    if (p.matrix[row][col]!=0) {
                        g.setColor(new Color(p.color*40, p.color*60, p.color*80));
                        int bx=(p.x+col)*blockSize, by=(p.y+row)*blockSize;
                        g.fillRect(bx, by, blockSize, blockSize);
                        g.setColor(Color.GRAY);
                        g.drawRect(bx, by, blockSize, blockSize);
                    }
        }
        // Next piece
        g.setColor(Color.WHITE);
        g.drawString("Next:", WIDTH*blockSize+10, 30);
        if (next!=null) {
            for (int row=0; row<next.matrix.length; row++)
                for (int col=0; col<next.matrix[0].length; col++)
                    if (next.matrix[row][col]!=0) {
                        g.setColor(new Color(next.color*40, next.color*60, next.color*80));
                        int bx=WIDTH*blockSize+10+col*blockSize;
                        int by=40+row*blockSize;
                        g.fillRect(bx, by, blockSize, blockSize);
                        g.setColor(Color.GRAY);
                        g.drawRect(bx, by, blockSize, blockSize);
                    }
        }
        g.setColor(Color.WHITE);
        g.drawString("Score: "+score, WIDTH*blockSize+10, 200);
        g.drawString("Lines: "+lines, WIDTH*blockSize+10, 230);
        g.drawString("Level: "+level, WIDTH*blockSize+10, 260);
        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("GAME OVER", 100, 300);
        }
        if (paused) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("PAUSED", 100, 300);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tetris");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new Tetris());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
