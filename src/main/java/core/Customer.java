package core;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import core.messages.ContractAccept;
import core.messages.ContractBid;
import core.messages.ContractDeal;
import core.messages.ContractRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A customer with very permissive time windows.
 */
public class Customer extends Parcel implements CommUser, TickListener {
    // time in ms
    private static final long SERVICE_DURATION = 10000;
    private static final double MAX_RANGE = Double.MAX_VALUE;

    private Optional<CommDevice> commDevice;
    private CustomerState state = CustomerState.INIT;
    private List<ContractBid> bids;

    Customer(ParcelDTO dto) {
        super(dto);
        bids = new ArrayList<>();
    }

    Customer(HistoricalData data) {
        this(Parcel.builder(
                data.getPickupPoint(),
                data.getDropoffPoint()
        )
                .neededCapacity(data.getPassengerCount())
                .serviceDuration(SERVICE_DURATION)
                .buildDTO());
    }

    private CustomerState getState() {
        return state;
    }

    private void setState(CustomerState state) {
        this.state = state;
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        sendRequest();
    }

    private void sendRequest() {
        commDevice.get().broadcast(new ContractRequest(this));
        setState(CustomerState.SENT_REQUEST);

    }

    @Override
    public Optional<Point> getPosition() {
        if (getRoadModel().containsObject(this)) {
            return Optional.of(getRoadModel().getPosition(this));
        }
        return Optional.absent();
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {
        commDevice = Optional.of(builder
                .setReliability(1)
                .setMaxRange(MAX_RANGE)
                .build()
        );
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        ImmutableList<Message> messages = commDevice.get().getUnreadMessages();

        if (getState() == CustomerState.SENT_REQUEST) {
            handleSentRequest(messages);
        } else if (getState() == CustomerState.SENT_DEAL) {
            handleSentDeal(messages);
        }
//        for (Message msg : messages) {
//            MessageContents contents = msg.getContents();
//            if (contents instanceof ContractBid) {
//                handleBid((ContractBid) contents);
//            } else if (contents instanceof ContractAccept) {
//                handleSentDeal((ContractAccept) contents);
//            }
//        }
    }

    private void handleSentRequest(ImmutableList<Message> messages) {
        bids.addAll(messages.stream()
                .filter(msg -> msg.getContents() instanceof ContractBid)
                .map(msg -> (ContractBid) msg.getContents())
                .collect(Collectors.toList()));
        if (!bids.isEmpty()) {
            bids.sort(Comparator.comparingDouble(ContractBid::getBid));
            // Send a deal to the highest bidder
            ContractBid highestBid = bids.remove(0);
            ContractDeal deal = new ContractDeal(this, highestBid.getBid());
            commDevice.get().send(deal, highestBid.getTaxi());
            setState(CustomerState.SENT_DEAL);
        }
    }

    private void handleSentDeal(ImmutableList<Message> messages) {
        java.util.Optional<ContractAccept> accept = messages.stream()
                .filter(msg -> msg.getContents() instanceof ContractAccept)
                .map(msg -> (ContractAccept) msg.getContents())
                .findFirst();
        if (accept.isPresent()) {
            setState(CustomerState.TAKEN);
        } else {
            handleSentRequest(messages);
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
    }

    @Override
    public String toString() {
        return new StringBuilder().append("Customer{")
//                .append(this.getPickupLocation().toString())
                .append(getState())
                .append("}").toString();
    }

    enum CustomerState {INIT, SENT_REQUEST, SENT_DEAL, TAKEN}
}