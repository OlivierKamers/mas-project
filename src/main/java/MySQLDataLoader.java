import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * DataLoader class responsible for loading the csv file containing the historical data.
 */
public class MySQLDataLoader {
    static String CONNECTION_STRING = "jdbc:mysql://%s:%s/%s?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    Connection connection;

    MySQLDataLoader() throws SQLException {
        connection = DriverManager.getConnection(String.format(CONNECTION_STRING, System.getenv("DB_HOST"), System.getenv("DB_PORT"), System.getenv("DB_NAME")), System.getenv("DB_USERNAME"), System.getenv("DB_PASS"));
    }

    public static void main(String[] args) {
        try {
            MySQLDataLoader loader = new MySQLDataLoader();
//            List<HistoricalData> data = loader.readAll();
            List<HistoricalData> data = loader.read(LocalDateTime.of(2015, 1, 2, 0, 0, 0), LocalDateTime.of(2015, 1, 3, 0, 0, 0));
            System.out.println(data.size());
//            for (HistoricalData d : data) {
//                System.out.println(d);
//            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read all data from the database table.
     * TODO: this is very slow
     */
    List<HistoricalData> readAll() throws SQLException {
        Statement statement = connection.createStatement();
        String query = "SELECT * FROM pickups;";
        ResultSet rst = statement.executeQuery(query);
        ArrayList<HistoricalData> result = new ArrayList<>();
        while (rst.next()) {
            result.add(parse(rst));
        }
        return result;
    }

    /**
     * Read data with tpep_pickup_datetime between two dates.
     */
    List<HistoricalData> read(LocalDateTime start, LocalDateTime end) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM pickups WHERE tpep_pickup_datetime BETWEEN ? AND ?;");
        statement.setObject(1, start);
        statement.setObject(2, end);
        System.out.println(statement.toString());
        ResultSet rst = statement.executeQuery();
        ArrayList<HistoricalData> result = new ArrayList<>();
        while (rst.next()) {
            result.add(parse(rst));
        }
        return result;
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