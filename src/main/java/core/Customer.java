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
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import core.messages.ContractAccept;
import core.messages.ContractBid;
import core.messages.ContractDeal;
import core.messages.ContractRequest;

import java.util.Comparator;

/**
 * A customer with very permissive time windows.
 */
public class Customer extends Parcel implements CommUser, TickListener {
    // time in ms
    public static final long SERVICE_DURATION = 0;
    private static final int MAX_TICKS_TO_WAIT_FOR_ACCEPT = 3;
    private static final int MAX_TICKS_TO_WAIT_FOR_BID = 5;

    private long pickupTime;

    private long id;
    private Optional<CommDevice> commDevice;
    private CustomerState state = CustomerState.INIT;
    private int ticksSinceSentRequest;
    private int ticksSinceSentDeal;
    private int ticksSinceCreate;
    private int numberOfSentRequests;

    Customer(long id, ParcelDTO dto) {
        super(dto);
        this.id = id;
        this.ticksSinceSentRequest = 0;
        this.ticksSinceSentDeal = 0;
        this.ticksSinceCreate = 0;
        this.numberOfSentRequests = 0;
        this.pickupTime = 0;
    }

    Customer(HistoricalData data, TimeLapse time) {
        this(data.getId(), Parcel.builder(
                data.getPickupPoint(),
                data.getDropoffPoint()
        )
                .orderAnnounceTime(time.getStartTime())
                // TODO: window bepalen
                .pickupTimeWindow(TimeWindow.create(time.getStartTime(), time.getEndTime() + 1000000))
                .neededCapacity(data.getPassengerCount() > MasProject.TAXI_CAPACITY ? MasProject.TAXI_CAPACITY : data.getPassengerCount())
                .serviceDuration(SERVICE_DURATION)
                .buildDTO());
    }

    Customer(HistoricalData data) {
        this(data.getId(), Parcel.builder(
                data.getPickupPoint(),
                data.getDropoffPoint()
        )
                .orderAnnounceTime(0)
                // TODO: window bepalen
                .pickupTimeWindow(TimeWindow.create(0, 1000000))
                .neededCapacity(data.getPassengerCount())
                .serviceDuration(SERVICE_DURATION)
                .buildDTO());
    }

    public int getNumberOfSentRequests() {
        return numberOfSentRequests;
    }

    public long getId() {
        return id;
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
        commDevice.get().broadcast(new ContractRequest(this, ticksSinceCreate));
        numberOfSentRequests++;
        ticksSinceSentRequest = 0;
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
        commDevice = Optional.of(builder.build());
    }

    public long getPickupTime() {
        return pickupTime;
    }

    void setPickupTime(long pickupTime) {
        this.pickupTime = pickupTime;
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        ticksSinceCreate++;

        ImmutableList<Message> messages = commDevice.get().getUnreadMessages();

        if (getState() == CustomerState.SENT_REQUEST) {
            ticksSinceSentRequest++;
            handleSentRequest(messages);
        } else if (getState() == CustomerState.SENT_DEAL) {
            ticksSinceSentDeal += 1;
            handleSentDeal(messages);
        }
    }

    private void handleSentRequest(ImmutableList<Message> messages) {
        java.util.Optional<ContractBid> highestBid = messages.stream()
                .filter(msg -> msg.getContents() instanceof ContractBid)
                .map(msg -> (ContractBid) msg.getContents())
                .max(Comparator.comparingDouble(ContractBid::getBid));

        if (!highestBid.isPresent()) {
            if (ticksSinceSentRequest >= MAX_TICKS_TO_WAIT_FOR_BID) {
                // No bids arrived before the deadline so the customer sends a new request
                sendRequest();
            }
            return;
        }

        ContractDeal deal = new ContractDeal(this, highestBid.get().getBid());
        commDevice.get().send(deal, highestBid.get().getTaxi());
        ticksSinceSentDeal = 0;
        setState(CustomerState.SENT_DEAL);
    }

    private void handleSentDeal(ImmutableList<Message> messages) {
        java.util.Optional<ContractAccept> accept = messages.stream()
                .filter(msg -> msg.getContents() instanceof ContractAccept)
                .map(msg -> (ContractAccept) msg.getContents())
                .findFirst();
        if (accept.isPresent()) {
//            System.out.println(toString() + " accepted by " + accept.get().getTaxi().toString());
            setState(CustomerState.TAKEN);
        } else if (ticksSinceSentDeal >= MAX_TICKS_TO_WAIT_FOR_ACCEPT) {
            sendRequest();
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("C")
                .append(getId())
                .append("{")
                .append(getState())
                .append(" #")
                .append(numberOfSentRequests)
                .append("}").toString();
    }

    enum CustomerState {INIT, SENT_REQUEST, SENT_DEAL, TAKEN}
}