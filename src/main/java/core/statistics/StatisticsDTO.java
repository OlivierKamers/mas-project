package core.statistics;

import com.google.common.base.Objects;
import com.google.gson.Gson;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.Nullable;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

/**
 * This is an immutable value object containing statistics about a simulation
 * run.
 * <p>
 * Two statistics objects are equal when all fields, EXCEPT computation time,
 * are equal.
 *
 * @author Rinde van Lon
 */
public class StatisticsDTO implements Serializable {
    private static final long serialVersionUID = 1968951252238291733L;

    /**
     * The time unit that is used in the simulation that generated this statistics
     * object.
     */
    public final String timeUnit;

    /**
     * The distance unit that is used in the simulation that generated this
     * statistics object.
     */
    public final String distanceUnit;

    /**
     * The speed unit that is used in the simulation that generated this
     * statistics object.
     */
    public final String speedUnit;

    /**
     * The cumulative distance all vehicle have traveled.
     */
    public final double totalDistance;
    /**
     * The total number of parcels that are picked up.
     */
    public final int totalPickups;
    /**
     * The total number of parcels that are delivered.
     */
    public final int totalDeliveries;
    /**
     * The cumulative pickup waiting time of all parcels.
     * This is the time between the announcement of a parcel and the time it is picked up by an agent.
     */
    public final ArrayList<Long> pickupWaitingTimes;
    /**
     * The total number of contract requests sent by parcels that have been picked up.
     */
    public final ArrayList<Integer> numberOfRequests;
    /**
     * The cumulative travel time overhead of all parcels.
     * This is the actual time it took to go from its pickup location to its delivery location
     * divided by the time it would have taken if the taxi drove in a straight line at its max speed.
     */
    public final ArrayList<Float> travelTimeOverhead;
    /**
     * The time (ms) it took to compute the simulation.
     */
    public final long computationTime;
    /**
     * The time that has elapsed in the simulation (this is in the unit which is
     * used in the simulation).
     */
    public final long simulationTime;
    /**
     * Indicates whether the scenario has finished.
     */
    public final boolean simFinish;
    /**
     * Total number of vehicles available.
     */
    public final int totalVehicles;
    /**
     * Number of vehicles that have been used, 'used' means has moved.
     */
    public final int movedVehicles;
    public ArrayList<Integer> amountOfIdleTaxis;
    public ArrayList<Integer> amountOfWaitingCustomers;
    public List<Double> totalIdleMovement;
    public ArrayList<Double> tradeProfits;
    public String[] args;

    /**
     * Create a new statistics object.
     *
     * @param dist      {@link #totalDistance}.
     * @param pick      {@link #totalPickups}.
     * @param del       {@link #totalDeliveries}.
     * @param pickWT    {@link #pickupWaitingTimes}.
     * @param req       {@link #numberOfRequests}.
     * @param travelTOh {@link #travelTimeOverhead}.
     * @param compT     {@link #computationTime}.
     * @param simT      {@link #simulationTime}.
     * @param finish    {@link #simFinish}.
     * @param totalVeh  {@link #totalVehicles}.
     * @param moved     {@link #movedVehicles}.
     * @param time      {@link #timeUnit}.
     * @param distUnit  {@link #distanceUnit}.
     * @param speed     {@link #speedUnit}.
     */
    public StatisticsDTO(double dist, int pick, int del,
                         ArrayList<Long> pickWT, ArrayList<Integer> req, ArrayList<Float> travelTOh, long compT, long simT, boolean finish,
                         int totalVeh, int moved, Unit<Duration> time,
                         Unit<Length> distUnit, Unit<Velocity> speed) {
        totalDistance = dist;
        totalPickups = pick;
        totalDeliveries = del;
        pickupWaitingTimes = pickWT;
        numberOfRequests = req;
        travelTimeOverhead = travelTOh;
        computationTime = compT;
        simulationTime = simT;
        simFinish = finish;
        totalVehicles = totalVeh;
        movedVehicles = moved;
        amountOfIdleTaxis = new ArrayList<>();
        amountOfWaitingCustomers = new ArrayList<>();
        totalIdleMovement = new ArrayList<>();
        tradeProfits = new ArrayList<>();
        args = new String[]{};
        timeUnit = time.toString();
        distanceUnit = distUnit.toString();
        speedUnit = speed.toString();
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public void setTradeProfits(ArrayList<Double> tradeProfits) {
        this.tradeProfits = tradeProfits;
    }

    public void setAmountOfIdleTaxis(ArrayList<Integer> amountOfIdleTaxis) {
        this.amountOfIdleTaxis = amountOfIdleTaxis;
    }

    public void setAmountOfWaitingCustomers(ArrayList<Integer> amountOfWaitingCustomers) {
        this.amountOfWaitingCustomers = amountOfWaitingCustomers;
    }

    public void setTotalIdleMovement(List<Double> totalIdleMovement) {
        this.totalIdleMovement = totalIdleMovement;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final StatisticsDTO other = (StatisticsDTO) obj;
        return new EqualsBuilder().append(totalDistance, other.totalDistance)
                .append(totalPickups, other.totalPickups)
                .append(totalDeliveries, other.totalDeliveries)
                .append(pickupWaitingTimes, other.pickupWaitingTimes)
                .append(travelTimeOverhead, other.travelTimeOverhead)
                .append(simulationTime, other.simulationTime)
                .append(simFinish, other.simFinish)
                .append(totalVehicles, other.totalVehicles)
                .append(movedVehicles, other.movedVehicles).isEquals();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(totalDistance, totalPickups,
                pickupWaitingTimes, travelTimeOverhead, simulationTime,
                simFinish, totalVehicles, movedVehicles);
    }

    public void save() {
        try {
            String fileName = "stats/stats_" + new Date().getTime() + ".json";
            Gson gson = new Gson();
            String json = gson.toJson(this);
            FileWriter fw = new FileWriter(fileName);
            fw.write(json);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
