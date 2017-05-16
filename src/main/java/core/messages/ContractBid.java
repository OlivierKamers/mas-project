package core.messages;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import core.Customer;
import core.Taxi;

/**
 * Contract bid.
 */
public class ContractBid implements MessageContents {
    private Taxi taxi;
    private double bid;

    public ContractBid(Taxi taxi, double bid) {
        this.taxi = taxi;
        this.bid = bid;
    }

    public Taxi getTaxi() {
        return taxi;
    }

    public double getBid() {
        return bid;
    }
}
