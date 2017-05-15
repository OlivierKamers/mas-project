import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * MySQLDataLoader class responsible for loading the historical data from a MySQL data source.
 */
public class MySQLDataLoader {
    static String CONNECTION_STRING = "jdbc:mysql://%s:%s/%s?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=CET";
    Connection connection;

    MySQLDataLoader() {
        try {
            connection = DriverManager.getConnection(String.format(CONNECTION_STRING, System.getenv("DB_HOST"), System.getenv("DB_PORT"), System.getenv("DB_NAME")), System.getenv("DB_USERNAME"), System.getenv("DB_PASS"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        MySQLDataLoader loader = new MySQLDataLoader();
        List<HistoricalData> data = loader.readAll();
//            List<HistoricalData> data = loader.read(LocalDateTime.of(2015, 1, 2, 0, 0, 0), LocalDateTime.of(2015, 1, 3, 0, 0, 0));
        System.out.println(data.size());
        for (HistoricalData d : data) {
            System.out.println(d);
        }
    }

    /**
     * Read all data from the database table.
     * TODO: this is very slow
     */
    List<HistoricalData> readAll() {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            String query = "SELECT * FROM pickups LIMIT 100;";
            ResultSet rst = statement.executeQuery(query);
            ArrayList<HistoricalData> result = new ArrayList<>();
            while (rst.next()) {
                result.add(parse(rst));
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Read N first data from the database table.
     */
    List<HistoricalData> readN(int N) {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("SELECT * FROM pickups LIMIT ?;");
            statement.setInt(1, N);
            ResultSet rst = statement.executeQuery();
            ArrayList<HistoricalData> result = new ArrayList<>();
            while (rst.next()) {
                result.add(parse(rst));
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Read data with tpep_pickup_datetime between two dates.
     */
    List<HistoricalData> read(LocalDateTime start, LocalDateTime end) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM pickups WHERE tpep_pickup_datetime >= ? AND tpep_pickup_datetime < ?;");
            statement.setObject(1, start);
            statement.setObject(2, end);
            ResultSet rst = statement.executeQuery();
            ArrayList<HistoricalData> result = new ArrayList<>();
            while (rst.next()) {
                result.add(parse(rst));
            }
            System.out.println(statement.toString() + " ==> " + result.size() + " pickups.");
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private HistoricalData parse(ResultSet rst) throws SQLException {
        return new HistoricalData(
                rst.getTimestamp("tpep_pickup_datetime").toLocalDateTime(),
                rst.getInt("passenger_count"),
                rst.getDouble("pickup_longitude"),
                rst.getDouble("pickup_latitude"),
                rst.getDouble("dropoff_longitude"),
                rst.getDouble("dropoff_latitude")
        );
    }
}
