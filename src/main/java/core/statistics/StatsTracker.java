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
package core.statistics;

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
import com.github.rinde.rinsim.pdptw.common.StatisticsProvider;
import com.google.auto.value.AutoValue;
import core.Customer;
import core.MasProject;

import javax.measure.unit.Unit;
import java.util.ArrayList;
import java.util.Map;

import static com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType.*;
import static com.github.rinde.rinsim.core.model.road.GenericRoadModel.RoadEventType.MOVE;
import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STARTED;
import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STOPPED;
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.Maps.newLinkedHashMap;


public final class StatsTracker extends AbstractModelVoid {
    private final EventDispatcher eventDispatcher;
    private final TheListener theListener;
    private final Clock clock;
    private final RoadModel roadModel;

    StatsTracker() {
        clock = null;
        roadModel = null;

        eventDispatcher = new EventDispatcher(StatisticsEventType.values());
        theListener = new TheListener();
    }

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
    public StatisticsDTO getStatistics() {
        long compTime = theListener.computationTime;
        if (compTime == 0) {
            compTime = System.currentTimeMillis() - theListener.startTimeReal;
        }

        return new StatisticsDTO(
                theListener.totalDistance,
                theListener.totalPickups, theListener.totalDeliveries,
                theListener.totalParcels, theListener.acceptedParcels,
                theListener.pickupWaitingTimes, theListener.totalRequestsBeforePickup, theListener.travelOverhead, compTime,
                clock.getCurrentTime(), theListener.simFinish,
                theListener.totalVehicles, theListener.distanceMap.size(),
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
            final Clock clock = dependencyProvider.get(Clock.class);
            final RoadModel rm = dependencyProvider.get(RoadModel.class);
            final PDPModel pm = dependencyProvider.get(PDPModel.class);
            return new StatsTracker(clock, rm, pm);
        }
    }

    class TheListener implements Listener {

        final Map<MovingRoadUser, Double> distanceMap;
        // parcels
        int totalParcels;
        int acceptedParcels;
        // vehicles
        int totalVehicles;
        double totalDistance;
        int totalPickups;
        int totalRequestsBeforePickup;
        int totalDeliveries;
        ArrayList<Long> pickupWaitingTimes;
        float travelOverhead;

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

            totalPickups = 0;
            totalDeliveries = 0;
            pickupWaitingTimes = new ArrayList<>();
            travelOverhead = 0;

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
            } else if (e.getEventType() == PDPModelEventType.START_PICKUP) {
                verify(e instanceof PDPModelEvent);
                final PDPModelEvent pme = (PDPModelEvent) e;
                final Parcel parcel = pme.parcel;
                final Vehicle vehicle = pme.vehicle;
                assert parcel != null;
                assert vehicle != null;

                final long waitingTime = pme.time - parcel.getOrderAnnounceTime();
                pickupWaitingTimes.add(waitingTime);
            } else if (e.getEventType() == PDPModelEventType.END_PICKUP) {
                verify(e instanceof PDPModelEvent);
                final PDPModelEvent pme = (PDPModelEvent) e;
                assert pme.parcel instanceof Customer;
                final Customer customer = (Customer) pme.parcel;
                assert customer != null;

                totalRequestsBeforePickup += customer.getNumberOfSentRequests();
                totalPickups++;
            } else if (e.getEventType() == PDPModelEventType.START_DELIVERY) {
                // do nothing, delivery has only started
            } else if (e.getEventType() == PDPModelEventType.END_DELIVERY) {
                final PDPModelEvent pme = (PDPModelEvent) e;
                assert pme.parcel instanceof Customer;
                final Customer customer = (Customer) pme.parcel;
                final Vehicle vehicle = pme.vehicle;
                assert customer != null;
                assert vehicle != null;

                final double travelTime = clock.getTimeUnit().getConverterTo(Unit.valueOf("h")).convert(pme.time - customer.getPickupTime());
                final double minimumTime = Point.distance(customer.getPickupLocation(), customer.getDeliveryLocation()) / MasProject.MAX_SPEED;
                if (travelTime > 0 && minimumTime > 0) {
                    travelOverhead += travelTime / minimumTime;
                } else {
                    // To avoid infinity, just say that overhead is 0 (fraction is 1)
                    travelOverhead += 1;
                }
                totalDeliveries++;
            } else if (e.getEventType() == NEW_PARCEL) {
                // pdp model event
                acceptedParcels++;
            } else if (e.getEventType() == NEW_VEHICLE) {
                totalVehicles++;
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
