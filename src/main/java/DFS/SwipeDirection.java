package DFS;

public enum SwipeDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    public SwipeDirection getReverse(SwipeDirection direction) {
        switch (direction) {
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case LEFT:
                return RIGHT;
            case RIGHT:
                return LEFT;
        }
        return null;
    }
}
