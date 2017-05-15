import com.github.rinde.rinsim.geom.Point;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HistoricalData {
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private LocalDateTime pickupTime;
    private int passengerCount;
    private double pickupLongitude;
    private double pickupLatitude;
    private Point pickupPoint;
    private double dropoffLongitude;
    private double dropoffLatitude;
    private Point dropoffPoint;

    public HistoricalData(LocalDateTime pickupTime, int passengerCount, double pickupLongitude, double pickupLatitude, double dropoffLongitude, double dropoffLatitude) {
        this.pickupTime = pickupTime;
        this.passengerCount = passengerCount;
        this.pickupLongitude = pickupLongitude;
        this.pickupLatitude = -pickupLatitude;
        this.pickupPoint = new Point(pickupLongitude, -pickupLatitude);
        this.dropoffLongitude = dropoffLongitude;
        this.dropoffLatitude = -dropoffLatitude;
        this.dropoffPoint = new Point(dropoffLongitude, -dropoffLatitude);
    }

    public static HistoricalData parse(String pickupTime, String passengerCount, String pickupLongitude, String pickupLatitude, String dropoffLongitude, String dropoffLatitude) throws ParseException {
        return new HistoricalData(
                LocalDateTime.parse(pickupTime.replaceAll("\"", ""), dateTimeFormatter),
                Integer.parseInt(passengerCount),
                Double.parseDouble(pickupLongitude),
                Double.parseDouble(pickupLatitude),
                Double.parseDouble(dropoffLongitude),
                Double.parseDouble(dropoffLatitude)
        );
    }

    public static HistoricalData parse(String[] fields) throws ParseException {
        return parse(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]);
    }

    public LocalDateTime getPickupTime() {
        return pickupTime;
    }

    public double getPickupLongitude() {
        return pickupLongitude;
    }

    public double getPickupLatitude() {
        return pickupLatitude;
    }

    public double getDropoffLongitude() {
        return dropoffLongitude;
    }

    public double getDropoffLatitude() {
        return dropoffLatitude;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public Point getPickupPoint() {
        return pickupPoint;
    }

    public Point getDropoffPoint() {
        return dropoffPoint;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[')
                .append(getPickupTime().toString())
                .append("] ")
                .append(getPassengerCount())
                .append(" passenger(s) ")
                .append(getPickupPoint())
                .append(" --> ")
                .append(getDropoffPoint())
        ;
        return sb.toString();
    }
}
