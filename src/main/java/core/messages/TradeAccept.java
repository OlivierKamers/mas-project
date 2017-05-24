package core.messages;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import core.Customer;

/**
 * Reply to a {@link TradeDeal} to finalise the trade.
 */
public class TradeAccept implements MessageContents {
    private Customer customer;

    public TradeAccept(Customer customer) {
        this.customer = customer;
    }

    public Customer getCustomer() {
        return customer;
    }

    @Override
    public String toString() {
        return "TradeAccept{" + getCustomer().toString() + "}";
    }
}
