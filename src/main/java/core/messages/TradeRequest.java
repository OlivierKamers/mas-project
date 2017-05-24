package core.messages;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import core.Customer;
import core.Taxi;

/**
 * Represents a request of a Taxi to trade 1 specific customer.
 */
public class TradeRequest implements MessageContents {
    private Taxi taxi;
    private Customer customer;
    private double routeReduction;

    public TradeRequest(Taxi taxi, Customer customer, double routeReduction) {
        this.taxi = taxi;
        this.customer = customer;
        this.routeReduction = routeReduction;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Taxi getTaxi() {
        return taxi;
    }

    public double getRouteReduction() {
        return routeReduction;
    }

    @Override
    public String toString() {
        return "TradeRequest{" + getRouteReduction() + "\t" + getTaxi() + "\t" + getCustomer().toString() + "}";
    }
}
