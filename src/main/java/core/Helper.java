package core;

import com.github.rinde.rinsim.geom.Point;

import java.time.LocalDateTime;

public class Helper {

    public static final int ROADMODEL_BOUNDARIES_SCALE = 10;
    public static final Point ROADMODEL_MIN_POINT = new Point(-74.0193099976, -40.8774528503);
    public static final Point ROADMODEL_MAX_POINT = new Point(-73.9104537964, -40.7011375427);
    public static final LocalDateTime START_TIME = LocalDateTime.of(2015, 1, 1, 0, 0, 0);
    public static final LocalDateTime STOP_TIME = LocalDateTime.of(2015, 1, 1, 1, 0, 0);

    public static Point convertToPointInBoundaries(double lon, double lat) {
        return Point.multiply(
                new Point(
                        (lon - ROADMODEL_MIN_POINT.x),
                        (lat - ROADMODEL_MIN_POINT.y)
                ),
                ROADMODEL_BOUNDARIES_SCALE / (ROADMODEL_MAX_POINT.y - ROADMODEL_MIN_POINT.y)
        );
    }

    public static double getXScale() {
        return (ROADMODEL_MAX_POINT.x - ROADMODEL_MIN_POINT.x) / (ROADMODEL_MAX_POINT.y - ROADMODEL_MIN_POINT.y);
    }

    public static double getYScale() {
        return 1d;
    }

    public static Point convertToPointInBoundaries(Point p) {
        return Helper.convertToPointInBoundaries(p.x, p.y);
    }
}
