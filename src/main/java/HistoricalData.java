import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class HistoricalData {
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private LocalDateTime pickupTime;
    private Number passengerCount;
    private Number pickupLongitude;
    private Number pickupLatitude;
    private Number dropoffLongitude;
    private Number dropoffLatitude;

    public HistoricalData(LocalDateTime pickupTime, Number passengerCount, Number pickupLongitude, Number pickupLatitude, Number dropoffLongitude, Number dropoffLatitude) {
        this.pickupTime = pickupTime;
        this.passengerCount = passengerCount;
        this.pickupLongitude = pickupLongitude;
        this.pickupLatitude = pickupLatitude;
        this.dropoffLongitude = dropoffLongitude;
        this.dropoffLatitude = dropoffLatitude;
    }

    public static HistoricalData parse(String pickupTime, String passengerCount, String pickupLongitude, String pickupLatitude, String dropoffLongitude, String dropoffLatitude) throws ParseException {
        return new HistoricalData(
                LocalDateTime.parse(pickupTime.replaceAll("\"", ""), dateTimeFormatter),
                NumberFormat.getNumberInstance(Locale.US).parse(passengerCount),
                NumberFormat.getNumberInstance(Locale.US).parse(pickupLongitude),
                NumberFormat.getNumberInstance(Locale.US).parse(pickupLatitude),
                NumberFormat.getNumberInstance(Locale.US).parse(dropoffLongitude),
                NumberFormat.getNumberInstance(Locale.US).parse(dropoffLatitude)
        );
    }

    public static HistoricalData parse(String[] fields) throws ParseException {
        return parse(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]);
    }

    public LocalDateTime getPickupTime() {
        return pickupTime;
    }

    public Number getPickupLongitude() {
        return pickupLongitude;
    }

    public Number getPickupLatitude() {
        return pickupLatitude;
    }

    public Number getDropoffLongitude() {
        return dropoffLongitude;
    }

    public Number getDropoffLatitude() {
        return dropoffLatitude;
    }

    public Number getPassengerCount() {
        return passengerCount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[')
                .append(getPickupTime().toString())
                .append("] ")
                .append(getPassengerCount().toString())
                .append(" passenger(s) (")
                .append(getPickupLongitude().toString())
                .append(", ")
                .append(getPickupLatitude().toString())
                .append(") --> (")
                .append(getDropoffLongitude().toString())
                .append(", ")
                .append(getDropoffLatitude().toString())
                .append(')')
        ;
        return sb.toString();
    }
}
