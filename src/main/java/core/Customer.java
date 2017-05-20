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
    private static final long SERVICE_DURATION = 10000;
    private static final double MAX_RANGE = Double.MAX_VALUE;
    private static final int MAX_TICKS_TO_WAIT_FOR_ACCEPT = 3;

    private long pickupTime;

    private long id;
    private Optional<CommDevice> commDevice;
    private CustomerState state = CustomerState.INIT;
    private int ticksSinceSentDeal;
    private int ticksSinceCreate;

    Customer(long id, ParcelDTO dto) {
        super(dto);
        this.id = id;
        this.ticksSinceSentDeal = 0;
        this.ticksSinceCreate = 0;
    }

    Customer(HistoricalData data, TimeLapse time) {
        this(data.getId(), Parcel.builder(
                data.getPickupPoint(),
                data.getDropoffPoint()
        )
                .orderAnnounceTime(time.getStartTime())
                // TODO: window bepalen
                .pickupTimeWindow(TimeWindow.create(time.getStartTime(), time.getEndTime() + 1000000))
                .neededCapacity(data.getPassengerCount())
                .serviceDuration(SERVICE_DURATION)
                .buildDTO());
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

    public long getPickupTime() {
        return pickupTime;
    }

    void setPickupTime(long pickupTime) {
        this.pickupTime = pickupTime;
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        ticksSinceCreate += 1;

        ImmutableList<Message> messages = commDevice.get().getUnreadMessages();

        if (getState() == CustomerState.SENT_REQUEST) {
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
                .sorted(Comparator.comparingDouble(ContractBid::getBid))
                .findFirst();

        if (!highestBid.isPresent())
            return;

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
            System.out.println(toString() + " accepted by " + accept.get().getTaxi().toString());
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
                .append("}").toString();
    }

    enum CustomerState {INIT, SENT_REQUEST, SENT_DEAL, TAKEN}
}