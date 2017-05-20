/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package core;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.core.model.pdp.PDPModelEvent;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.GenericRoadModel.RoadEventType;
import com.github.rinde.rinsim.core.model.road.MoveEvent;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.core.model.time.Clock.ClockEventType;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.StatisticsProvider;
import com.github.rinde.rinsim.scenario.ScenarioController.ScenarioEvent;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.google.auto.value.AutoValue;

import java.util.Map;

import static com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType.*;
import static com.github.rinde.rinsim.core.model.road.GenericRoadModel.RoadEventType.MOVE;
import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STARTED;
import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STOPPED;
import static com.github.rinde.rinsim.scenario.ScenarioController.EventType.SCENARIO_EVENT;
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.Maps.newLinkedHashMap;


public final class StatsTracker extends AbstractModelVoid implements StatisticsProvider {
    private final EventDispatcher eventDispatcher;
    private final TheListener theListener;
    private final Clock clock;
    private final RoadModel roadModel;

    StatsTracker(Clock c, RoadModel rm, PDPModel pm) {
        clock = c;
        roadModel = rm;

        eventDispatcher = new EventDispatcher(StatisticsEventType.values());
        theListener = new TheListener();
        roadModel.getEventAPI().addListener(theListener, MOVE);
        clock.getEventAPI().addListener(theListener, STARTED, STOPPED);

        pm.getEventAPI()
                .addListener(theListener, START_PICKUP, END_PICKUP, START_DELIVERY,
                        END_DELIVERY, NEW_PARCEL, NEW_VEHICLE);
    }

    /**
     * @return A new {@link Builder} instance.
     */
    public static Builder builder() {
        return new AutoValue_StatsTracker_Builder();
    }

    EventAPI getEventAPI() {
        return eventDispatcher.getPublicEventAPI();
    }

    /**
     * @return A {@link StatisticsDTO} with the current simulation stats.
     */
    @Override
    public StatisticsDTO getStatistics() {
        final int vehicleBack = theListener.lastArrivalTimeAtDepot.size();
        long overTime = 0;
        if (theListener.simFinish) {
            for (final Long time : theListener.lastArrivalTimeAtDepot.values()) {
                if (time - theListener.scenarioEndTime > 0) {
                    overTime += time - theListener.scenarioEndTime;
                }
            }
        }

        long compTime = theListener.computationTime;
        if (compTime == 0) {
            compTime = System.currentTimeMillis() - theListener.startTimeReal;
        }

        return new StatisticsDTO(
                theListener.totalDistance,
                theListener.totalPickups, theListener.totalDeliveries,
                theListener.totalParcels, theListener.acceptedParcels,
                theListener.pickupTardiness, theListener.deliveryTardiness, compTime,
                clock.getCurrentTime(), theListener.simFinish, vehicleBack,
                overTime, theListener.totalVehicles, theListener.distanceMap.size(),
                clock.getTimeUnit(), roadModel.getDistanceUnit(),
                roadModel.getSpeedUnit());
    }

    @Override
    public <U> U get(Class<U> clazz) {
        return clazz.cast(this);
    }

    enum StatisticsEventType {
        PICKUP_TARDINESS, DELIVERY_TARDINESS, ALL_VEHICLES_AT_DEPOT;
    }

    static class StatisticsEvent extends Event {
        final Parcel parcel;
        final Vehicle vehicle;
        final long tardiness;
        final long time;

        StatisticsEvent(Enum<?> type, Object pIssuer, Parcel p, Vehicle v,
                        long tar, long tim) {
            super(type, pIssuer);
            parcel = p;
            vehicle = v;
            tardiness = tar;
            time = tim;
        }
    }

    /**
     * Builder for creating {@link StatsTracker} instance.
     *
     * @author Rinde van Lon
     */
    @AutoValue
    public abstract static class Builder
            extends AbstractModelBuilder<StatsTracker, Object> {
        private static final long serialVersionUID = -4339759920383479477L;

        Builder() {
            setDependencies(
                    Clock.class,
                    RoadModel.class,
                    PDPModel.class);
            setProvidingTypes(StatisticsProvider.class);
        }

        @Override
        public StatsTracker build(DependencyProvider dependencyProvider) {
            final Clock clck = dependencyProvider.get(Clock.class);
            final RoadModel rm = dependencyProvider.get(RoadModel.class);
            final PDPModel pm = dependencyProvider.get(PDPModel.class);
            return new StatsTracker(clck, rm, pm);
        }
    }

    class TheListener implements Listener {

        private static final double MOVE_THRESHOLD = 0.0001;
        final Map<MovingRoadUser, Double> distanceMap;
        final Map<MovingRoadUser, Long> lastArrivalTimeAtDepot;
        // parcels
        int totalParcels;
        int acceptedParcels;
        // vehicles
        int totalVehicles;
        double totalDistance;
        int totalPickups;
        int totalDeliveries;
        long pickupTardiness;
        long deliveryTardiness;

        // simulation
        long startTimeReal;
        long startTimeSim;
        long computationTime;
        long simulationTime;

        boolean simFinish;
        long scenarioEndTime;

        TheListener() {
            totalParcels = 0;
            acceptedParcels = 0;

            totalVehicles = 0;
            distanceMap = newLinkedHashMap();
            totalDistance = 0d;
            lastArrivalTimeAtDepot = newLinkedHashMap();

            totalPickups = 0;
            totalDeliveries = 0;
            pickupTardiness = 0;
            deliveryTardiness = 0;

            simFinish = false;
        }

        @Override
        public void handleEvent(Event e) {
            if (e.getEventType() == ClockEventType.STARTED) {
                startTimeReal = System.currentTimeMillis();
                startTimeSim = clock.getCurrentTime();
                computationTime = 0;

            } else if (e.getEventType() == ClockEventType.STOPPED) {
                computationTime = System.currentTimeMillis() - startTimeReal;
                simulationTime = clock.getCurrentTime() - startTimeSim;
            } else if (e.getEventType() == RoadEventType.MOVE) {
                verify(e instanceof MoveEvent);
                final MoveEvent me = (MoveEvent) e;
                increment((MovingRoadUser) me.roadUser, me.pathProgress.distance().getValue());
                totalDistance += me.pathProgress.distance().getValue();
                // if we are closer than 10 cm to the depot, we say we are 'at'
                // the depot
                if (Point.distance(me.roadModel.getPosition(me.roadUser),
                        ((Vehicle) me.roadUser).getStartPosition()) < MOVE_THRESHOLD) {
                    // only override time if the vehicle did actually move
                    if (me.pathProgress.distance().getValue() > MOVE_THRESHOLD) {
                        lastArrivalTimeAtDepot.put((MovingRoadUser) me.roadUser,
                                clock.getCurrentTime());
                        if (totalVehicles == lastArrivalTimeAtDepot.size()) {
                            eventDispatcher.dispatchEvent(new Event(
                                    StatisticsEventType.ALL_VEHICLES_AT_DEPOT, this));
                        }
                    }
                } else {
                    lastArrivalTimeAtDepot.remove(me.roadUser);
                }

            } else if (e.getEventType() == PDPModelEventType.START_PICKUP) {
                verify(e instanceof PDPModelEvent);
                final PDPModelEvent pme = (PDPModelEvent) e;
                final Parcel p = pme.parcel;
                final Vehicle v = pme.vehicle;
                assert p != null;
                assert v != null;

                final long latestBeginTime = p.getPickupTimeWindow().end()
                        - p.getPickupDuration();
                if (pme.time > latestBeginTime) {
                    final long tardiness = pme.time - latestBeginTime;
                    pickupTardiness += tardiness;
                    eventDispatcher.dispatchEvent(new StatisticsEvent(
                            StatisticsEventType.PICKUP_TARDINESS, this, p, v, tardiness,
                            pme.time));
                }
            } else if (e.getEventType() == PDPModelEventType.END_PICKUP) {
                totalPickups++;
            } else if (e.getEventType() == PDPModelEventType.START_DELIVERY) {
                final PDPModelEvent pme = (PDPModelEvent) e;

                final Parcel p = pme.parcel;
                final Vehicle v = pme.vehicle;
                assert p != null;
                assert v != null;

                final long latestBeginTime = p.getDeliveryTimeWindow().end()
                        - p.getDeliveryDuration();
                if (pme.time > latestBeginTime) {
                    final long tardiness = pme.time - latestBeginTime;
                    deliveryTardiness += tardiness;
                    eventDispatcher.dispatchEvent(new StatisticsEvent(
                            StatisticsEventType.DELIVERY_TARDINESS, this, p, v, tardiness,
                            pme.time));
                }
            } else if (e.getEventType() == PDPModelEventType.END_DELIVERY) {
                totalDeliveries++;
            } else if (e.getEventType() == SCENARIO_EVENT) {
                final ScenarioEvent se = (ScenarioEvent) e;
                if (se.getTimedEvent() instanceof AddParcelEvent) {
                    totalParcels++;
                } else if (se.getTimedEvent() instanceof AddVehicleEvent) {
                    totalVehicles++;
                } else if (se.getTimedEvent() instanceof TimeOutEvent) {
                    simFinish = true;
                    scenarioEndTime = se.getTimedEvent().getTime();
                }
            } else if (e.getEventType() == NEW_PARCEL) {
                // pdp model event
                acceptedParcels++;
            } else if (e.getEventType() == NEW_VEHICLE) {
                verify(e instanceof PDPModelEvent);
                final PDPModelEvent ev = (PDPModelEvent) e;
                lastArrivalTimeAtDepot.put(ev.vehicle, clock.getCurrentTime());
            } else {
                // currently not handling fall throughs
            }

        }

        void increment(MovingRoadUser mru, double num) {
            if (!distanceMap.containsKey(mru)) {
                distanceMap.put(mru, num);
            } else {
                distanceMap.put(mru, distanceMap.get(mru) + num);
            }
        }
    }
}
