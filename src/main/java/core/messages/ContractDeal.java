package core.messages;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import core.Customer;

/**
 * Represents a deal for a contract.
 */
public class ContractDeal implements MessageContents {
    private Customer customer;

    public ContractDeal(Customer customer) {
        this.customer = customer;
    }

    public Customer getCustomer() {
        return customer;
    }
}
