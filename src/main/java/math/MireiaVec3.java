package math;

public class MireiaVec3 {
    private float x, y, z;

    public MireiaVec3() {
        this(0, 0, 0);
    }
    
    public MireiaVec3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Getters and setters
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getZ() { return z; }
    public void setZ(float z) { this.z = z; }

    public MireiaVec3 add(MireiaVec3 other) {
        return new MireiaVec3(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public MireiaVec3 subtract(MireiaVec3 other) {
        return new MireiaVec3(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public MireiaVec3 scale(float scalar) {
        return new MireiaVec3(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public float dot(MireiaVec3 other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public MireiaVec3 cross(MireiaVec3 other) {
        return new MireiaVec3(
            this.y * other.z - this.z * other.y,
            this.z * other.x - this.x * other.z,
            this.x * other.y - this.y * other.x
        );
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public MireiaVec3 normalize() {
        float len = length();
        if (len == 0) return new MireiaVec3(0, 0, 0);
        return new MireiaVec3(x / len, y / len, z / len);
    }

    public float[] toArray() {
        return new float[]{x, y, z};
    }
}
