# Snake

Classic Snake written in plain Java with Swing. One file, no dependencies.

## Run it

You need a JDK (8 or newer).

```
javac SnakeGame.java
java SnakeGame
```

## Controls

| Key | Action |
| --- | --- |
| Arrow keys / WASD | Move |
| P | Pause |
| Space / Enter | Start or restart |

## How it works

- The snake is a `Deque<Point>` with the head at the front. Each tick adds a new head and drops the tail, unless it just ate.
- Key presses go into a small queue so two fast turns in a row both register, and you can't reverse straight into yourself.
- Every food makes the game a little faster, down to a minimum delay.
- Fill the whole board and you win.
