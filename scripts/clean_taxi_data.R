library(data.table)
`%between%`<-function(x,rng) x>rng[1] & x<rng[2]
MIN_LONG = -74.0193099976
MAX_LONG = -73.9104537964
MIN_LATI = 40.7011375427
MAX_LATI = 40.8774528503
load_data <- function(filename) {
  d <- fread(filename,
             header = TRUE,
             sep = ',',
             select = c('tpep_pickup_datetime','passenger_count','pickup_longitude','pickup_latitude','dropoff_longitude','dropoff_latitude')
             )
  print('Filtering coordinates')
  d_filtered <- d[d$pickup_longitude %between% c(MIN_LONG,MAX_LONG) & d$dropoff_longitude %between% c(MIN_LONG,MAX_LONG) & d$pickup_latitude %between% c(MIN_LATI, MAX_LATI) & d$dropoff_latitude %between% c(MIN_LATI, MAX_LATI) , ]
  print('Rounding coordinates')
  cols <- c('pickup_longitude','pickup_latitude','dropoff_longitude','dropoff_latitude')
  d_filtered[,(cols) := round(.SD,6), .SDcols=cols]
  print('Sorting by pickup datetime')
  d_filtered <- d_filtered[order(as.POSIXct(d_filtered$tpep_pickup_datetime, format="%Y-%m-%d %H:%M:%S")),]
  print('Writing csv')
  write.csv(d_filtered, gsub(".csv", "_cleaned.csv", filename), row.names=FALSE)
}