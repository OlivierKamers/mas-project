package core;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * MySQLDataLoader class responsible for loading the historical data from a MySQL data source.
 * Table structure:
 * CREATE TABLE `pickups` (
 * `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
 * `tpep_pickup_datetime` datetime DEFAULT NULL,
 * `passenger_count` int(2) DEFAULT NULL,
 * `pickup_longitude` decimal(8,6) DEFAULT NULL,
 * `pickup_latitude` decimal(8,6) DEFAULT NULL,
 * `dropoff_longitude` decimal(8,6) DEFAULT NULL,
 * `dropoff_latitude` decimal(8,6) DEFAULT NULL,
 * PRIMARY KEY (`id`),
 * KEY `idx_tpep_pickup_datetime` (`tpep_pickup_datetime`)
 * ) ENGINE=InnoDB AUTO_INCREMENT=20617002 DEFAULT CHARSET=utf8;
 */
public class MySQLDataLoader {
    private static String CONNECTION_STRING = "jdbc:mysql://%s:%s/%s?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=CET";
    private Connection connection;

    MySQLDataLoader() {
        try {
            connection = DriverManager.getConnection(String.format(CONNECTION_STRING, System.getenv("DB_HOST"), System.getenv("DB_PORT"), System.getenv("DB_NAME")), System.getenv("DB_USERNAME"), System.getenv("DB_PASS"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        MySQLDataLoader loader = new MySQLDataLoader();
        List<core.HistoricalData> data = loader.read(LocalDateTime.of(2015, 1, 2, 0, 0, 0), LocalDateTime.of(2015, 1, 3, 0, 0, 0));
        System.out.println(data.size());
        for (HistoricalData d : data) {
            System.out.println(d);
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
            rst.close();
            statement.close();
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private HistoricalData parse(ResultSet rst) throws SQLException {
        return new HistoricalData(
                rst.getLong("id"),
                rst.getTimestamp("tpep_pickup_datetime").toLocalDateTime(),
                rst.getInt("passenger_count"),
                rst.getDouble("pickup_longitude"),
                rst.getDouble("pickup_latitude"),
                rst.getDouble("dropoff_longitude"),
                rst.getDouble("dropoff_latitude")
        );
    }
}
