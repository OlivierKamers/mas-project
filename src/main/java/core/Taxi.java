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

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import core.messages.ContractAccept;
import core.messages.ContractBid;
import core.messages.ContractDeal;
import core.messages.ContractRequest;

/**
 * Implementation of a very simple taxi agent. It moves to the closest customer,
 * picks it up, then delivers it, repeat.
 *
 * @author Rinde van Lon
 */
public class Taxi extends Vehicle implements CommUser {
    private static final double SPEED = 1000d;
    private static final double MAX_RANGE = Double.MAX_VALUE;
    private Optional<Parcel> curr;
    private Optional<CommDevice> commDevice;
    private TaxiState state = TaxiState.IDLE;

    Taxi(Point startPosition, int capacity) {
        super(VehicleDTO.builder()
                .capacity(capacity)
                .startPosition(startPosition)
                .speed(SPEED)
                .build());
        curr = Optional.absent();
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

        // Idle core.Taxi (no objective for now, later no passengers)
        if (!curr.isPresent()) {
            handleContractNet();
//            curr = Optional.fromNullable(RoadModels.findClosestObject(
//                    rm.getPosition(this), rm, Parcel.class));
        }

        // core.Taxi with a goal
        if (curr.isPresent()) {
            final boolean inCargo = pm.containerContains(this, curr.get());
            // sanity check: if it is not in our cargo AND it is also not on the
            // RoadModel, we cannot go to curr anymore.
            if (!inCargo && !rm.containsObject(curr.get())) {
                System.out.println("Current objective does not exist anymore!");
                curr = Optional.absent();
            } else if (inCargo) {
                // if it is in cargo, go to its destination
                rm.moveTo(this, curr.get().getDeliveryLocation(), time);
                if (rm.getPosition(this).equals(curr.get().getDeliveryLocation())) {
                    // deliver when we arrive
                    pm.deliver(this, curr.get(), time);
                }
            } else {
                // it is still available, go there as fast as possible
                rm.moveTo(this, curr.get(), time);
                if (rm.equalPosition(this, curr.get())) {
                    // pickup customer
                    pm.pickup(this, curr.get(), time);
                }
            }
        }
    }

    private void handleContractNet() {
        ImmutableList<Message> messages = commDevice.get().getUnreadMessages();
        // TODO: refactor naar case over state ipv loop over messages

        if (state == TaxiState.IDLE) {
            handleIdle(messages);
        }

        for (Message msg : messages) {
            MessageContents contents = msg.getContents();
            if (contents instanceof ContractRequest) {
                handleRequest((ContractRequest) contents);
            } else if (contents instanceof ContractDeal) {
                handleDeal((ContractDeal) contents);
            }
        }
    }

    private void handleIdle(ImmutableList<Message> messages) {
        messages.stream().filter()
        Customer customer = request.getCustomer();
        ContractBid bid = new ContractBid(this, getBid(customer));
        commDevice.get().send(bid, customer);
    }

    private void handleRequest(ContractRequest request) {
        Customer customer = request.getCustomer();
        ContractBid bid = new ContractBid(this, getBid(customer));
        commDevice.get().send(bid, customer);
    }

    private double getBid(Customer customer) {
        return 0;
    }

    private void handleDeal(ContractDeal deal) {
        Customer customer = deal.getCustomer();
        ContractAccept accept = new ContractAccept(this);
        commDevice.get().send(accept, customer);
        curr = Optional.of(customer);
        state = TaxiState.ACCEPTED;
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

    enum TaxiState {
        IDLE, ACCEPTED, HAS_CUSTOMER
    }
}