package core;
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

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import core.messages.*;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation of a Taxi agent.
 * It can pick up multiple customers and calculate the most efficient route to take.
 * When idle, the Taxi uses historical knowledge and messages from neighboring vehicles to move to a more optimal waiting location.
 *
 * @author Evert Etienne & Olivier Kamers
 */
public class Taxi extends Vehicle implements CommUser {
    public static final int DEFAULT_FIELD_RANGE = 5;
    private static final int TRADE_DEAL_WAIT_TICKS = 2;
    private static final double TRADE_RANGE_MIN = 2;
    private static final double TRADE_RANGE_MAX = 2.5;
    private static final double COMMUNICATION_RANGE = Helper.ROADMODEL_BOUNDARIES_SCALE;
    private static final double SPEED = 15;
    private static final double FIELD_VECTOR_FACTOR = 0.5;
    private static final int MAX_CONCURRENT_PICKUPS = 3;
    private final int id;
    private Vector2D fieldVector;
    private ArrayList<Customer> currentCustomers;
    private ArrayList<Customer> pickedUpCustomers;
    private ArrayList<Point> route;
    private double remainingRouteLength;
    private Optional<CommDevice> commDevice;
    private TaxiState state;
    private DiscreteField df;
    private ArrayList<MoveProgress> idleMoveProgress;
    private int ticksSinceTradeDeal;
    private double dealCapacity;
    private boolean useTrading;
    private int fieldRange;
    private double idleTravelDistance;
    private double idleTravelLimit;

    Taxi(int id, Point startPosition, int capacity, DiscreteField df, boolean useTrading, int fieldRange, double idleTravelLimit) {
        super(VehicleDTO.builder()
                .capacity(capacity)
                .startPosition(startPosition)
                .speed(SPEED)
                .build());
        this.currentCustomers = new ArrayList<>();
        this.pickedUpCustomers = new ArrayList<>();
        this.idleMoveProgress = new ArrayList<>();
        this.route = new ArrayList<>();
        this.remainingRouteLength = 0;
        this.id = id;
        this.df = df;
        this.fieldVector = new Vector2D(0, 0);
        setState(TaxiState.IDLE);
        this.ticksSinceTradeDeal = TRADE_DEAL_WAIT_TICKS;
        this.dealCapacity = 0;
        this.useTrading = useTrading;
        this.fieldRange = fieldRange;
        this.idleTravelDistance = 0;
        this.idleTravelLimit = idleTravelLimit;
    }

    public ArrayList<MoveProgress> getIdleMoveProgress() {
        return idleMoveProgress;
    }

    public int getId() {
        return id;
    }

    public TaxiState getState() {
        return state;
    }

    private void setState(TaxiState state) {
        this.state = state;
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
    }

    @Override
    protected void tickImpl(@NotNull TimeLapse time) {
        final RoadModel rm = getRoadModel();
        final PDPModel pm = getPDPModel();

        if (!time.hasTimeLeft()) {
            return;
        }

        ticksSinceTradeDeal++;

        if (ticksSinceTradeDeal > TRADE_DEAL_WAIT_TICKS) {
            dealCapacity = 0;
        }

        ImmutableList<Message> messages = commDevice.get().getUnreadMessages();

        // Handle the contract net (deals, pickup, delivery) if needed
        if (shouldHandleContractNet()) {
            handleContractNet(messages);
        }

        // Taxi has not finished its route yet
        if (!route.isEmpty()) {
            Point target = route.get(0);
            rm.moveTo(this, target, time);
            if (rm.getPosition(this).equals(target)) {
                // We have reached our next target.
                // If there are customers to be dropped off here, drop them
                java.util.Optional<Customer> customerToDeliver = currentCustomers.stream()
                        .filter(c -> c.getDeliveryLocation().equals(rm.getPosition(this)))
                        .filter(c -> pm.containerContains(this, c))
                        .findFirst();
                if (customerToDeliver.isPresent()) {
                    pm.deliver(this, customerToDeliver.get(), time);
                    currentCustomers.remove(customerToDeliver.get());
                    pickedUpCustomers.remove(customerToDeliver.get());
                }
                // If there are customers to be picked up here, pick them up
                java.util.Optional<Customer> customerToPickup = currentCustomers.stream()
                        .filter(c -> c.getPickupLocation().equals(rm.getPosition(this)))
                        .filter(c -> !pm.containerContains(this, c) && rm.containsObject(c))
                        .findFirst();
                if (customerToPickup.isPresent()) {
                    pm.pickup(this, customerToPickup.get(), time);
                    customerToPickup.get().setPickupTime(time.getTime());
                    pickedUpCustomers.add(customerToPickup.get());
                }
                route.remove(0);
                if (route.isEmpty()) {
                    setState(TaxiState.IDLE);
                    idleTravelDistance = 0;
                }
            }
        }
        if (getState() == TaxiState.IDLE && df != null && idleTravelDistance < idleTravelLimit) {
            // Idle state: move according to the discrete field
            fieldVector = df.getNextPosition(this, time.getStartTime(), messages, fieldRange).add(FIELD_VECTOR_FACTOR, fieldVector);
            Point targetPoint = new Point(
                    Math.max(0, Math.min(rm.getBounds().get(1).x, getPosition().get().x + fieldVector.getX())),
                    Math.max(0, Math.min(rm.getBounds().get(1).y, getPosition().get().y + fieldVector.getY()))
            );
            MoveProgress moveProgress = rm.moveTo(this, targetPoint, time);
            idleMoveProgress.add(moveProgress);
            idleTravelDistance += moveProgress.distance().getValue();
        }
        // Broadcast position message
        sendPositionMessage();
        // Do trading if needed
        if (useTrading)
            trade(messages);
    }

    private void sendPositionMessage() {
        commDevice.get().broadcast(new PositionBroadcast(getPosition().get(), getFreeCapacity()));
    }

    /**
     * Check whether this Taxi can pickup more customers.
     */
    private boolean shouldHandleContractNet() {
        return getFreeCapacity() > 0;
    }

    private double getFreeCapacity() {
        if (currentCustomers.size() >= MAX_CONCURRENT_PICKUPS) {
            return 0;
        }

        return getCapacity() - currentCustomers.stream().mapToDouble(Parcel::getNeededCapacity).sum() - dealCapacity;
    }

    private void handleContractNet(ImmutableList<Message> messages) {
        handleDeals(messages);
        handleBids(messages);
    }

    private void handleDeals(ImmutableList<Message> messages) {
        double freeCapacity = getFreeCapacity();
        java.util.Optional<ContractDeal> deal = messages.stream()
                .filter(m -> m.getContents() instanceof ContractDeal)
                .map(msg -> (ContractDeal) msg.getContents())
                .filter(m -> m.getCustomer().getNeededCapacity() <= freeCapacity)
                .sorted(Comparator.comparingDouble(ContractDeal::getBid).reversed())
                .findFirst();
        deal.ifPresent(this::acceptDeal);
    }

    private void handleBids(ImmutableList<Message> messages) {
        double freeCapacity = getFreeCapacity();
        messages.stream()
                .filter(m -> m.getContents() instanceof ContractRequest)
                .map(m -> (ContractRequest) m.getContents())
                .filter(m -> m.getCustomer().getNeededCapacity() <= freeCapacity)
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
     * The calculation is done based on the route this Taxi would take to pick up the customer.
     */
    private double getBid(Customer customer) {
        if (getPosition().isPresent() && customer.getPosition().isPresent()) {
            ArrayList<Customer> customersWithThisCustomer = new ArrayList<>(currentCustomers);
            customersWithThisCustomer.add(customer);
            return routeLength(getShortestRoute(customersWithThisCustomer)) / getSpeed();
        }
        return Double.MAX_VALUE;
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
        currentCustomers.add(customer);
        sortRoute();
        setState(TaxiState.BUSY);
    }

    /**
     * Generate the shortest route for this taxi taking into account its current customers.
     */
    private void sortRoute() {
        route = getShortestRoute(currentCustomers);
    }

    /**
     * Find the shortest route for the given list of customers.
     */
    private ArrayList<Point> getShortestRoute(List<Customer> customers) {
        if (customers.isEmpty()) {
            return new ArrayList<>();
        }
        ArrayList<ArrayList<Point>> allPermutations = generatePermutations(new ArrayList<>(customers));
        return allPermutations.stream().sorted(Comparator.comparingDouble(this::routeLength)).findFirst().get();
    }

    /**
     * Generate all valid routes for the given list of customers.
     * It creates all permutations taking into account that pickup has to occur before delivery
     * and taking into account which customers have already been picked up.
     */
    private ArrayList<ArrayList<Point>> generatePermutations(ArrayList<Customer> customers) {
        if (customers.isEmpty()) {
            // Base case for recursion: empty route
            ArrayList<ArrayList<Point>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }

        // Remove first customer in the list
        Customer firstCustomer = customers.remove(0);

        // Recursive case with the remaining customers
        ArrayList<ArrayList<Point>> permutations = generatePermutations(customers);

        // Create new permutations from the recursive case and the current customer
        ArrayList<ArrayList<Point>> returnValue = new ArrayList<>();
        Point firstPickup = firstCustomer.getPickupLocation();
        Point firstDelivery = firstCustomer.getDeliveryLocation();
        if (!pickedUpCustomers.contains(firstCustomer)) {
            // Insert pickup and delivery for this customer in all possible positions (pickup before delivery) for all other permutations
            for (ArrayList<Point> smallerPermuted : permutations) {
                for (int pickupIndex = 0; pickupIndex <= smallerPermuted.size(); pickupIndex++) {
                    for (int deliveryIndex = pickupIndex + 1; deliveryIndex <= smallerPermuted.size() + 1; deliveryIndex++) {
                        ArrayList<Point> temp = new ArrayList<>(smallerPermuted);
                        temp.add(pickupIndex, firstPickup);
                        temp.add(deliveryIndex, firstDelivery);
                        returnValue.add(temp);
                    }
                }
            }
        } else {
            // The current customer has already been picked up so we don't add his pickupLocation to the route
            // Insert delivery for this customer in all possible positions for all other permutations
            for (ArrayList<Point> smallerPermuted : permutations) {
                for (int deliveryIndex = 0; deliveryIndex <= smallerPermuted.size(); deliveryIndex++) {
                    ArrayList<Point> temp = new ArrayList<>(smallerPermuted);
                    temp.add(deliveryIndex, firstDelivery);
                    returnValue.add(temp);
                }
            }
        }
        return returnValue;
    }

    /**
     * Calculate the length of the given route.
     */
    private double routeLength(ArrayList<Point> route) {
        if (route.isEmpty())
            return 0;
        double distance = Point.distance(getPosition().get(), route.get(0));
        if (route.size() == 1)
            return distance;
        distance += IntStream
                .range(0, route.size() - 1)
                .mapToDouble(i -> Point.distance(route.get(i), route.get(i + 1)))
                .sum();
        return distance;
    }

    /**
     * Do trading with nearby taxis
     */
    private void trade(ImmutableList<Message> messages) {
        handleTradeAccept(messages);
        handleTradeDeals(messages);

        remainingRouteLength = routeLength(route);

        if (ticksSinceTradeDeal > TRADE_DEAL_WAIT_TICKS)
            handleTradeRequests(messages);
        sendTradeRequest(messages);
    }

    /**
     * Handle an accepted trade: add the customer to the current customers and update route.
     */
    private void handleTradeAccept(ImmutableList<Message> messages) {
        messages.stream()
                .filter(m -> m.getContents() instanceof TradeAccept)
                .map(m -> (TradeAccept) m.getContents())
                .findFirst()
                .ifPresent(ta -> {
                    currentCustomers.add(ta.getCustomer());
                    dealCapacity = 0;
//                    System.out.println(toString() + " handled accept for customer " + ta.getCustomer());
                    sortRoute();
                    setState(TaxiState.BUSY);
                });
    }

    /**
     * Handle received trade deals.
     * Find the deal with the highest profit, remove the traded customer and send an accept.
     */
    private void handleTradeDeals(ImmutableList<Message> messages) {
        messages.stream()
                .filter(m -> m.getContents() instanceof TradeDeal)
                .map(m -> (TradeDeal) m.getContents())
                .sorted(Comparator.comparingDouble(TradeDeal::getProfit).reversed())
                .findFirst()
                .ifPresent(tradeDeal -> {
                    if (currentCustomers.contains(tradeDeal.getCustomer()) && !pickedUpCustomers.contains(tradeDeal.getCustomer())) {
                        TradeAccept tradeAccept = new TradeAccept(tradeDeal.getCustomer());
//                        System.out.println("Sending trade accept " + tradeAccept.toString() + " for deal " + tradeDeal.toString());
                        commDevice.get().send(tradeAccept, tradeDeal.getTaxi());
                        currentCustomers.remove(tradeDeal.getCustomer());
                        sortRoute();
                    }
                });
    }

    /**
     * Handle incoming trade requests.
     * The taxi can send only 1 trade deal every tick.
     */
    private void handleTradeRequests(ImmutableList<Message> messages) {
        double freeCapacity = getFreeCapacity();
        List<TradeRequest> tradeRequests = messages.stream()
                .filter(m -> m.getContents() instanceof TradeRequest)
                .map(m -> (TradeRequest) m.getContents())
                .filter(tr -> tr.getCustomer().getNeededCapacity() <= freeCapacity)
                .collect(Collectors.toList());

        TradeRequest bestRequest = null;
        double bestProfit = 0;
        for (TradeRequest tradeRequest : tradeRequests) {
            double profit = calculateProfit(tradeRequest);
            if (profit > bestProfit) {
                bestRequest = tradeRequest;
                bestProfit = profit;
            }
        }

        if (bestRequest != null) {
            TradeDeal tradeDeal = new TradeDeal(bestProfit, this, bestRequest.getCustomer());
//            System.out.println(toString() + " Sending trade deal " + tradeDeal + " to " + bestRequest.getTaxi());
            commDevice.get().send(tradeDeal, bestRequest.getTaxi());
            ticksSinceTradeDeal = 0;
            dealCapacity = bestRequest.getCustomer().getNeededCapacity();
        }
    }

    private double calculateProfit(TradeRequest tradeRequest) {
        ArrayList<Customer> customersWithTradedCustomer = new ArrayList<>(currentCustomers);
        customersWithTradedCustomer.add(tradeRequest.getCustomer());
        double extraCost = routeLength(getShortestRoute(customersWithTradedCustomer)) - remainingRouteLength;
        return tradeRequest.getRouteReduction() - extraCost;
    }

    /**
     * Send trade requests for a pending customer.
     */
    private void sendTradeRequest(ImmutableList<Message> messages) {
        List<Customer> pendingCustomers = currentCustomers.stream().filter(c -> !pickedUpCustomers.contains(c)).collect(Collectors.toList());
        if (pendingCustomers.isEmpty()) return;

        List<Taxi> possibleTaxis = messages.stream()
                .filter(m -> m.getSender() instanceof Taxi && m.getContents() instanceof PositionBroadcast)
                .filter(m -> {
                    double dist = Point.distance(getRoadModel().getPosition((Taxi) m.getSender()), getPosition().get());
                    return dist < TRADE_RANGE_MAX && dist > TRADE_RANGE_MIN;
                })
                .map(m -> (Taxi) m.getSender())
                .collect(Collectors.toList());
        if (possibleTaxis.isEmpty()) return;

        double bestReduction = 0;
        Customer bestCustomer = null;
        for (Customer customer : pendingCustomers) {
            List<Customer> newCustomers = new ArrayList<>(pendingCustomers);
            newCustomers.remove(customer);
            double routeReduction = remainingRouteLength - routeLength(getShortestRoute(newCustomers));

            if (routeReduction > bestReduction) {
                bestReduction = routeReduction;
                bestCustomer = customer;
            }
        }

        if (bestCustomer != null) {
            Customer finalBestCustomer = bestCustomer;
            double finalBestReduction = bestReduction;
            possibleTaxis.forEach(taxi -> {
                TradeRequest tradeRequest = new TradeRequest(this, finalBestCustomer, finalBestReduction);
//                System.out.println(toString() + " Sending trade request " + tradeRequest);
                commDevice.get().send(tradeRequest, taxi);
            });
        }
    }

    @Override
    public Optional<Point> getPosition() {
        return Optional.of(getRoadModel().getPosition(this));
    }

    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        commDevice = Optional.of(builder
                .setReliability(1)
                .setMaxRange(COMMUNICATION_RANGE)
                .build()
        );
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append("T")
                .append(getId())
                .append("{")
                .append(getState())
                .append(" ")
                .append((int) getFreeCapacity())
                .append("/")
                .append((int) getCapacity());
        currentCustomers.stream().map(Customer::getId).forEach(id -> sb.append(" ").append(id));
        sb.append("}");
        return sb.toString();
    }

    enum TaxiState {
        IDLE, BUSY
    }
}
