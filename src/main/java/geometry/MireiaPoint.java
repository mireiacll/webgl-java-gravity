package geometry;

import math.MireiaVec2;

public class MireiaPoint {
    private MireiaVec2 position;
    private int id;

    public MireiaPoint(MireiaVec2 position) {
        this.position = (position != null) ? position : new MireiaVec2();
        this.id = -1;
    }

    public MireiaPoint(float x, float y) {
        this(new MireiaVec2(x, y));
    }

    public MireiaVec2 getPosition() { return position; }
    public void setPosition(MireiaVec2 position) { this.position = position; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public float[] toArray() {
        return position.toArray();
    }
}