package core.messages;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import core.Taxi;

/**
 * Implements an Accept message.
 */
public class ContractAccept implements MessageContents {
    private Taxi taxi;

    public ContractAccept(Taxi taxi) {
        this.taxi = taxi;
    }

    public Taxi getTaxi() {
        return taxi;
    }
}
