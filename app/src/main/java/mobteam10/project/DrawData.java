package mobteam10.project;

import java.io.Serializable;

/**
 * Created by JHG on 2017-11-18.
 */

public class DrawData implements Serializable {

    private static final long serialVersionUID = 8501L;

    private short x;
    private short y;
    private short color;

    public DrawData(short x, short y, short color)
    {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public short getX() {
        return x;
    }

    public void setX(short x) {
        this.x = x;
    }

    public short getY() {
        return y;
    }

    public void setY(short y) {
        this.y = y;
    }

    public short getColor() {
        return color;
    }

    public void setColor(short color) {
        this.color = color;
    }
}
