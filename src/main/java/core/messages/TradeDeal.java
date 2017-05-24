package core.messages;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import core.Customer;
import core.Taxi;

/**
 * Reply to a {@link TradeRequest} to agree to a trade.
 */
public class TradeDeal implements MessageContents {
    private double profit;
    private Taxi taxi;
    private Customer customer;

    public TradeDeal(double profit, Taxi taxi, Customer customer) {
        this.profit = profit;
        this.taxi = taxi;
        this.customer = customer;
    }

    public Customer getCustomer() {

        return customer;
    }

    public Taxi getTaxi() {
        return taxi;
    }

    public double getProfit() {
        return profit;
    }

    @Override
    public String toString() {
        return "TradeDeal{" + getProfit() + "\t" + getTaxi() + "\t" + getCustomer().toString() + "}";
    }
}
