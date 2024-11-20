package game;

public class Word {
    private String text;
    private int x;
    private int y;
    private int speed;

    public Word(String text, int x, int speed) {
        this.text = text;
        this.x = x;
        this.y = 0;
        this.speed = speed;
    }

    public void move() {
        y += speed;
    }

    // getters and setters
    public String getText() {
        return text;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getSpeed() {
        return speed;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }
}
