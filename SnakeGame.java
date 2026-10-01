import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Snake. Eat the food, don't hit the walls or yourself.
 * The snake speeds up a little every time it eats.
 *
 * Controls: arrow keys or WASD to move, P to pause, Space to start/restart.
 */
public class SnakeGame extends JPanel implements ActionListener {

    private static final int CELL = 24;          // pixels per grid square
    private static final int COLS = 24;
    private static final int ROWS = 20;
    private static final int HUD_HEIGHT = 44;    // score bar at the top

    private static final int START_DELAY = 130;  // ms between moves at the start
    private static final int MIN_DELAY = 60;     // fastest the game can get
    private static final int SPEEDUP_PER_FOOD = 3;

    private static final Color BG_HUD = new Color(18, 20, 28);
    private static final Color BG_DARK = new Color(26, 30, 40);
    private static final Color BG_LIGHT = new Color(30, 35, 46);
    private static final Color SNAKE_HEAD = new Color(130, 230, 130);
    private static final Color SNAKE_TAIL = new Color(40, 140, 85);
    private static final Color FOOD = new Color(240, 85, 85);
    private static final Color TEXT = new Color(235, 235, 240);

    private enum Direction {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        final int dx;
        final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        boolean isOpposite(Direction other) {
            return dx + other.dx == 0 && dy + other.dy == 0;
        }
    }

    private enum State { READY, PLAYING, PAUSED, GAME_OVER, WON }

    // Head of the snake is the first element, tail is the last.
    private final Deque<Point> snake = new ArrayDeque<>();

    // Turns get buffered here so two quick key presses in one tick both count,
    // and so you can't reverse into yourself by mashing keys.
    private final Deque<Direction> pendingTurns = new ArrayDeque<>();

    private final Random random = new Random();
    private final Timer timer = new Timer(START_DELAY, this);

    private Direction direction;
    private Point food;
    private State state = State.READY;
    private int score;
    private int best;

    public SnakeGame() {
        setPreferredSize(new Dimension(COLS * CELL, ROWS * CELL + HUD_HEIGHT));
        setBackground(BG_HUD);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode());
            }
        });
        reset();
    }

    // ---------- game logic ----------

    private void reset() {
        snake.clear();
        pendingTurns.clear();
        direction = Direction.RIGHT;
        score = 0;
        timer.setDelay(START_DELAY);

        int startX = COLS / 2;
        int startY = ROWS / 2;
        for (int i = 0; i < 3; i++) {
            snake.addLast(new Point(startX - i, startY));
        }
        spawnFood();
    }

    private void spawnFood() {
        List<Point> open = new ArrayList<>();
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                Point p = new Point(x, y);
                if (!snake.contains(p)) {
                    open.add(p);
                }
            }
        }
        if (open.isEmpty()) {
            // Board is full, which means you beat the game.
            food = null;
            state = State.WON;
            timer.stop();
            return;
        }
        food = open.get(random.nextInt(open.size()));
    }

    /** One game tick. Called by the timer. */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == State.PLAYING) {
            step();
        }
        repaint();
    }

    private void step() {
        if (!pendingTurns.isEmpty()) {
            direction = pendingTurns.poll();
        }

        Point head = snake.peekFirst();
        Point next = new Point(head.x + direction.dx, head.y + direction.dy);

        boolean hitWall = next.x < 0 || next.x >= COLS || next.y < 0 || next.y >= ROWS;
        if (hitWall) {
            endGame();
            return;
        }

        boolean eating = next.equals(food);

        // Moving into the spot the tail is about to leave is fine,
        // unless we're eating (then the tail stays put).
        boolean tailIsLeaving = !eating && next.equals(snake.peekLast());
        if (snake.contains(next) && !tailIsLeaving) {
            endGame();
            return;
        }

        if (!eating) {
            snake.removeLast();
        }
        snake.addFirst(next);

        if (eating) {
            score++;
            best = Math.max(best, score);
            timer.setDelay(Math.max(MIN_DELAY, START_DELAY - score * SPEEDUP_PER_FOOD));
            spawnFood();
        }
    }

    private void endGame() {
        state = State.GAME_OVER;
        timer.stop();
    }

    private void start() {
        state = State.PLAYING;
        timer.start();
    }

    // ---------- input ----------

    private void handleKey(int key) {
        Direction turn = directionFor(key);

        if (turn != null) {
            if (state == State.READY) {
                start();
            }
            if (state == State.PLAYING) {
                queueTurn(turn);
            }
        } else if (key == KeyEvent.VK_P) {
            togglePause();
        } else if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_ENTER) {
            if (state == State.READY) {
                start();
            } else if (state == State.PAUSED) {
                togglePause();
            } else if (state == State.GAME_OVER || state == State.WON) {
                reset();
                start();
            }
        }
        repaint();
    }

    private static Direction directionFor(int key) {
        switch (key) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                return Direction.UP;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                return Direction.DOWN;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                return Direction.LEFT;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                return Direction.RIGHT;
            default:
                return null;
        }
    }

    private void queueTurn(Direction turn) {
        // Compare against the last queued turn, not the current direction,
        // so a fast "up then left" works the way you'd expect.
        Direction last = pendingTurns.isEmpty() ? direction : pendingTurns.peekLast();
        if (pendingTurns.size() < 2 && turn != last && !turn.isOpposite(last)) {
            pendingTurns.addLast(turn);
        }
    }

    private void togglePause() {
        if (state == State.PLAYING) {
            state = State.PAUSED;
            timer.stop();
        } else if (state == State.PAUSED) {
            start();
        }
    }

    // ---------- drawing ----------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawHud(g2);

        g2.translate(0, HUD_HEIGHT);
        drawBoard(g2);
        drawFood(g2);
        drawSnake(g2);
        drawOverlay(g2);

        g2.dispose();
    }

    private void drawHud(Graphics2D g2) {
        g2.setColor(TEXT);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        FontMetrics fm = g2.getFontMetrics();
        int baseline = (HUD_HEIGHT + fm.getAscent() - fm.getDescent()) / 2;

        g2.drawString("Score: " + score, 14, baseline);

        String bestText = "Best: " + best;
        g2.drawString(bestText, getWidth() - fm.stringWidth(bestText) - 14, baseline);
    }

    private void drawBoard(Graphics2D g2) {
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                g2.setColor((x + y) % 2 == 0 ? BG_DARK : BG_LIGHT);
                g2.fillRect(x * CELL, y * CELL, CELL, CELL);
            }
        }
    }

    private void drawFood(Graphics2D g2) {
        if (food == null) {
            return;
        }
        int pad = 4;
        g2.setColor(FOOD);
        g2.fillOval(food.x * CELL + pad, food.y * CELL + pad, CELL - 2 * pad, CELL - 2 * pad);
    }

    private void drawSnake(Graphics2D g2) {
        int i = 0;
        int length = snake.size();
        for (Point p : snake) {
            // Fade from the head color to the tail color along the body.
            float t = length <= 1 ? 0f : (float) i / (length - 1);
            g2.setColor(blend(SNAKE_HEAD, SNAKE_TAIL, t));
            g2.fillRoundRect(p.x * CELL + 1, p.y * CELL + 1, CELL - 2, CELL - 2, 8, 8);
            i++;
        }
        drawEyes(g2, snake.peekFirst());
    }

    private void drawEyes(Graphics2D g2, Point head) {
        int centerX = head.x * CELL + CELL / 2;
        int centerY = head.y * CELL + CELL / 2;

        // Push the eyes toward the front of the head, one on each side.
        int forwardX = direction.dx * 4;
        int forwardY = direction.dy * 4;
        int sideX = -direction.dy * 5;
        int sideY = direction.dx * 5;

        int r = 3;
        g2.setColor(BG_HUD);
        g2.fillOval(centerX + forwardX + sideX - r, centerY + forwardY + sideY - r, 2 * r, 2 * r);
        g2.fillOval(centerX + forwardX - sideX - r, centerY + forwardY - sideY - r, 2 * r, 2 * r);
    }

    private void drawOverlay(Graphics2D g2) {
        String title;
        String subtitle;
        switch (state) {
            case READY:
                title = "SNAKE";
                subtitle = "Arrow keys or WASD to move. Press any of them to start.";
                break;
            case PAUSED:
                title = "Paused";
                subtitle = "Press P to keep going";
                break;
            case GAME_OVER:
                title = "Game Over";
                subtitle = "Score: " + score + "   Press Space to play again";
                break;
            case WON:
                title = "You Win!";
                subtitle = "You filled the whole board. Press Space to play again";
                break;
            default:
                return;
        }

        int width = COLS * CELL;
        int height = ROWS * CELL;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, width, height);

        g2.setColor(TEXT);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 42));
        drawCentered(g2, title, width, height / 2 - 10);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        drawCentered(g2, subtitle, width, height / 2 + 24);
    }

    private static void drawCentered(Graphics2D g2, String text, int width, int baseline) {
        int x = (width - g2.getFontMetrics().stringWidth(text)) / 2;
        g2.drawString(text, x, baseline);
    }

    private static Color blend(Color a, Color b, float t) {
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }

    // ---------- entry point ----------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new SnakeGame());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
