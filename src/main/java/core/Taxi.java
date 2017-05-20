package core;/*
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

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import core.messages.*;

import java.util.Comparator;

/**
 * Implementation of a very simple taxi agent. It moves to the closest customer,
 * picks it up, then delivers it, repeat.
 *
 * @author Rinde van Lon
 */
public class Taxi extends Vehicle implements CommUser {
    private static final double SPEED = 1000d;
    private static final double MAX_RANGE = Double.MAX_VALUE;
    private static final int FIELD_RANGE = 5;


    private final int id;
    private Optional<Customer> currentCustomer;
    private Optional<CommDevice> commDevice;
    private TaxiState state;
    private DiscreteField df;

    Taxi(int id, Point startPosition, int capacity, DiscreteField df) {
        super(VehicleDTO.builder()
                .capacity(capacity)
                .startPosition(startPosition)
                .speed(SPEED)
                .build());
        this.currentCustomer = Optional.absent();
        this.id = id;
        this.df = df;
        setState(TaxiState.IDLE);
    }

    public int getId() {
        return id;
    }

    private TaxiState getState() {
        return state;
    }

    private void setState(TaxiState state) {
        this.state = state;
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
    }

    @Override
    protected void tickImpl(TimeLapse time) {
        final RoadModel rm = getRoadModel();
        final PDPModel pm = getPDPModel();

        if (!time.hasTimeLeft()) {
            return;
        }

        ImmutableList<Message> messages = commDevice.get().getUnreadMessages();

        // Idle Taxi (no objective for now, later no passengers)
        if (shouldHandleContractNet()) {
            handleContractNet(messages);
//            curr = Optional.fromNullable(RoadModels.findClosestObject(
//                    rm.getPosition(this), rm, Parcel.class));
        }

        // Taxi with a goal
        if (currentCustomer.isPresent()) {
            final boolean inCargo = pm.containerContains(this, currentCustomer.get());
            // sanity check: if it is not in our cargo AND it is also not on the
            // RoadModel, we cannot go to curr anymore.
            if (!inCargo && !rm.containsObject(currentCustomer.get())) {
                currentCustomer = Optional.absent();
                setState(TaxiState.IDLE);
            } else if (inCargo) {
                // if it is in cargo, go to its destination
                rm.moveTo(this, currentCustomer.get().getDeliveryLocation(), time);
                if (rm.getPosition(this).equals(currentCustomer.get().getDeliveryLocation())) {
                    // deliver when we arrive
                    pm.deliver(this, currentCustomer.get(), time);
                }
            } else {
                // it is still available, go there as fast as possible
                rm.moveTo(this, currentCustomer.get(), time);
                if (rm.equalPosition(this, currentCustomer.get())) {
                    // pickup customer
                    pm.pickup(this, currentCustomer.get(), time);
                    setState(TaxiState.HAS_CUSTOMER);
                }
            }
        } else if (getState() == TaxiState.IDLE) {
            // Idle
            Point fieldPoint = df.getNextPosition(this, time.getStartTime(), messages, FIELD_RANGE);
            rm.moveTo(this, fieldPoint, time);
        }
        sendPositionMessage();
    }

    private void sendPositionMessage() {
        commDevice.get().broadcast(new PositionBroadcast(getPosition().get(), getFreeCapacity()));
    }

    /**
     * Check whether this Taxi can pickup more customers.
     */
    private boolean shouldHandleContractNet() {
//        return getFreeCapacity() > 0;
        return !currentCustomer.isPresent();
    }

    private double getFreeCapacity() {
        return getCapacity() - getPDPModel().getContentsSize(this);
    }

    private void handleContractNet(ImmutableList<Message> messages) {
        if (getState() == TaxiState.IDLE) {
            handleIdle(messages);
        }
    }

    /**
     * This method will accept the best deal it received
     * or send a ContractBid to every customer that sent a ContractRequest if no deal was received.
     *
     * @param messages All unread messages for this taxi.
     */
    private void handleIdle(ImmutableList<Message> messages) {
        handleDeals(messages);
        handleBids(messages);
    }

    private void handleDeals(ImmutableList<Message> messages) {
        java.util.Optional<ContractDeal> deal = messages.stream()
                .filter(m -> m.getContents() instanceof ContractDeal)
                .map(msg -> (ContractDeal) msg.getContents())
                .filter(m -> getFreeCapacity() >= m.getCustomer().getNeededCapacity())
                .sorted(Comparator.comparingDouble(ContractDeal::getBid).reversed())
                .findFirst();
        deal.ifPresent(this::acceptDeal);
    }

    private void handleBids(ImmutableList<Message> messages) {
        messages.stream()
                .filter(m -> m.getContents() instanceof ContractRequest)
                .map(m -> (ContractRequest) m.getContents())
                .filter(m -> getFreeCapacity() >= m.getCustomer().getNeededCapacity())
                .forEach(this::sendBid);
    }

    /**
     * Handle a ContractRequest.
     * Calculates the bid and sends a ContractBid to the customer.
     *
     * @param request the request.
     */
    private void sendBid(ContractRequest request) {
        Customer customer = request.getCustomer();
        ContractBid bid = new ContractBid(this, getBid(customer));
        commDevice.get().send(bid, customer);
    }

    /**
     * Calculate a bid for the given customer.
     *
     * @param customer the customer to calculate the bid for.
     * @return the inverse of the distance between this taxi and the customer or 0 if either of the positions are absent.
     */
    private double getBid(Customer customer) {
        if (getPosition().isPresent() && customer.getPosition().isPresent())
            return 1.0 / Point.distance(getPosition().get(), customer.getPosition().get());
        return 0;
    }

    /**
     * Handles a ContractDeal: accepts it and sets the Customer as next target.
     *
     * @param deal the deal to accept.
     */
    private void acceptDeal(ContractDeal deal) {
        Customer customer = deal.getCustomer();
        ContractAccept accept = new ContractAccept(this);
        commDevice.get().send(accept, customer);
        currentCustomer = Optional.of(customer);
        setState(TaxiState.ACCEPTED);
    }

    @Override
    public Optional<Point> getPosition() {
        return Optional.of(getRoadModel().getPosition(this));
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {
        commDevice = Optional.of(builder
                .setReliability(1)
                .setMaxRange(MAX_RANGE)
                .build()
        );
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append("T")
                .append(getId())
                .append("{")
                .append(getState());
        if (currentCustomer.isPresent())
            sb.append(" ").append(currentCustomer.get().getId());
        sb.append("}");
        return sb.toString();
    }

    enum TaxiState {
        IDLE, ACCEPTED, HAS_CUSTOMER
    }
}
