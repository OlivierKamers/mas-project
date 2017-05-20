package core.messages;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.geom.Point;

public class PositionBroadcast implements MessageContents {
    private Point position;
    private double freeCapacity;

    public PositionBroadcast(Point position, double freeCapacity) {
        this.position = position;
        this.freeCapacity = freeCapacity;
    }

    public Point getPosition() {
        return position;
    }

    public double getFreeCapacity() {
        return freeCapacity;
    }
}
