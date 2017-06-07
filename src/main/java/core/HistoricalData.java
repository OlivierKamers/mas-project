package core;

import com.github.rinde.rinsim.geom.Point;

import java.time.LocalDateTime;

public class HistoricalData {

    private long id;
    private LocalDateTime pickupTime;
    private int passengerCount;
    private double pickupLongitude;
    private double pickupLatitude;
    private Point pickupPoint;
    private double dropoffLongitude;
    private double dropoffLatitude;
    private Point dropoffPoint;

    HistoricalData(long id, LocalDateTime pickupTime, int passengerCount, double pickupLongitude, double pickupLatitude, double dropoffLongitude, double dropoffLatitude) {
        this.id = id;
        this.pickupTime = pickupTime;
        this.passengerCount = passengerCount;
        this.pickupLongitude = pickupLongitude;
        this.pickupLatitude = -pickupLatitude;
        this.pickupPoint = Helper.convertToPointInBoundaries(pickupLongitude, getPickupLatitude());
        this.dropoffLongitude = dropoffLongitude;
        this.dropoffLatitude = -dropoffLatitude;
        this.dropoffPoint = Helper.convertToPointInBoundaries(dropoffLongitude, getDropoffLatitude());
    }

    long getId() {
        return id;
    }

    private LocalDateTime getPickupTime() {
        return pickupTime;
    }

    double getPickupLongitude() {
        return pickupLongitude;
    }

    double getPickupLatitude() {
        return pickupLatitude;
    }

    double getDropoffLongitude() {
        return dropoffLongitude;
    }

    double getDropoffLatitude() {
        return dropoffLatitude;
    }

    int getPassengerCount() {
        return passengerCount;
    }

    Point getPickupPoint() {
        return pickupPoint;
    }

    Point getDropoffPoint() {
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
