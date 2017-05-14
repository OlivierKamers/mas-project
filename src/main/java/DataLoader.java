import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DataLoader class responsible for loading the csv file containing the historical data.
 */
public class DataLoader {
    private static String DELIMITER = ",";
    private BufferedReader bufferedReader;
    private LocalDateTime currentDateTime;
    private HistoricalData lastData;

    DataLoader(String path, LocalDateTime startTime) throws IOException {
        bufferedReader = new BufferedReader(new FileReader(path));
        bufferedReader.readLine(); // to skip header line
        currentDateTime = startTime;
    }

    public static void main(String[] args) {
        try {
            DataLoader loader = new DataLoader("src/main/resources/data/yellow_tripdata_2015-01_cleaned.csv", LocalDateTime.of(2015, 1, 1, 0, 0, 0));
//            List<HistoricalData> data = loader.read(Duration.ofMinutes(5));
            List<HistoricalData> data = loader.readAll();
            System.out.println(data.size());
//            for (HistoricalData historicalData : data) {
//                System.out.println(historicalData.toString());
//            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read all data from this data file.
     */
    List<HistoricalData> readAll() throws IOException, ParseException {
        ArrayList<HistoricalData> result = new ArrayList<>(11000000);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            result.add(HistoricalData.parse(line.split(DELIMITER)));
        }
        return result;
    }

    /**
     * Read the next pickups.
     *
     * @param duration The amount of time to move forward in the data file.
     */
    private List<HistoricalData> read(Duration duration) throws IOException, ParseException {
        boolean finished = false;
        ArrayList<HistoricalData> result = new ArrayList<>();
        if (lastData != null) {
            result.add(lastData);
        }
        while (!finished) {
            String line = bufferedReader.readLine();
            if (line == null) {
                finished = true;
            } else {
                String[] fields = line.split(DELIMITER);
                HistoricalData data = HistoricalData.parse(fields);
                if (data.getPickupTime().isBefore(currentDateTime.plus(duration))) {
                    result.add(data);
                } else {
                    finished = true;
                    lastData = data;
                    currentDateTime = data.getPickupTime();
                }
            }
        }
        return result;
    }
}
