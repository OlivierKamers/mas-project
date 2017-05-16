package core.messages;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import core.Customer;

/**
 * Pickup request by Customer.
 */
public class ContractRequest implements MessageContents {
    private Customer customer;

    public ContractRequest(Customer customer) {
        this.customer = customer;
    }

    public Customer getCustomer() {
        return customer;
    }
}
