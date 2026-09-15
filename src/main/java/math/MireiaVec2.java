package math;

public class MireiaVec2 {
    private float x, y;

    public MireiaVec2() {
        this(0, 0);
    }

    public MireiaVec2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // Getters and setters
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float dot(MireiaVec2 other) {
        return this.x * other.x + this.y * other.y;
    }

    public float cross(MireiaVec2 other) {
        return this.x * other.y - this.y * other.x;
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public float distance2D(MireiaVec2 other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public float squaredDistance2D(MireiaVec2 other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        return dx * dx + dy * dy;
    }

    public float[] toArray() {
        return new float[]{x, y};
    }
}
