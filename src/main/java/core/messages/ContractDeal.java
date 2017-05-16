package core.messages;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import core.Customer;

/**
 * Represents a deal for a contract.
 */
public class ContractDeal implements MessageContents {
    private Customer customer;
    private double bid;

    public ContractDeal(Customer customer, double bid) {
        this.customer = customer;
        this.bid = bid;
    }

    public Customer getCustomer() {
        return customer;
    }

    public double getBid() {
        return bid;
    }
}
