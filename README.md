# mas-project

The project is structured as follows:
```
.
├── figures
├── mas-project.iml
├── pom.xml
├── project.iml
├── scripts
│   ├── clean_taxi_data.R
│   ├── data
│   ├── json_experiments.py
│   ├── osm2dot.py
│   ├── overpass-manhattan
│   ├── requirements.txt
│   └── scripts.Rproj
├── src
│   └── main
│       ├── java
│       │   └── core
│       │       ├── Customer.java
│       │       ├── DiscreteField.java
│       │       ├── DiscreteFieldRenderer.java
│       │       ├── FieldGenerator.java
│       │       ├── Helper.java
│       │       ├── HistoricalData.java
│       │       ├── MasProject.java
│       │       ├── MySQLDataLoader.java
│       │       ├── Taxi.java
│       │       ├── TaxiRenderer.java
│       │       ├── messages
│       │       │   ├── ContractAccept.java
│       │       │   ├── ContractBid.java
│       │       │   ├── ContractDeal.java
│       │       │   ├── ContractRequest.java
│       │       │   ├── PositionBroadcast.java
│       │       │   ├── TradeAccept.java
│       │       │   ├── TradeDeal.java
│       │       │   └── TradeRequest.java
│       │       └── statistics
│       │           ├── StatisticsDTO.java
│       │           ├── StatsPanel.java
│       │           └── StatsTracker.java
│       └── resources
├── stats
└── target
```

The scripts for data preprocessing and analysis of experiment results are stored in the `scripts` directory. 
It has a subdirectory `data` for storing the *.csv files with historical data.
The cleaned data is stored in the resources directory. It should be imported in a MySQL database. 
For instructions see the `MySQLDataLoader` class.

The experiments can be run using the MASProject class with the following command line arguments:
```
usage: MAS-project
 -f,--field              Enable field
 -F,--frange <arg>       Range for field analysis
 -g,--gui                Run with GUI
 -i,--influence <arg>    Taxi repulsion influence range
 -l,--idlelimit <arg>    Distance limit for idle driving
 -m,--mtxstep <arg>      Matrix Subdivision Step
 -r,--resolution <arg>   Minutes per time frame
 -s,--sample <arg>       Data sampling factor
 -t,--trade              Enable trading
 ```
