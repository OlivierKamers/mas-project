package core.messages;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import core.Customer;

/**
 * Pickup request by Customer.
 * TODO: take into account waitingTicks to give higher priority to long waiting customers
 */
public class ContractRequest implements MessageContents {
    private Customer customer;
    private int waitingTicks;

    public ContractRequest(Customer customer, int waitingTicks) {
        this.customer = customer;
        this.waitingTicks = waitingTicks;
    }

    public Customer getCustomer() {
        return customer;
    }

    public int getWaitingTicks() {
        return waitingTicks;
    }
}
