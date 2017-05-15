/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * MAS Project 2017
 *
 * @author Evert Etienne & Olivier Kamers
 */
public final class MasProject {
    private static final double MAX_SPEED = 50;
    private static final int NUM_TAXIS = 20;
    private static final int NUM_CUSTOMERS = 200;
    // time in ms
    private static final long SERVICE_DURATION = 60000;
    private static final int TAXI_CAPACITY = 10;
    private static final int SPEED_UP = 1;
    private static final int MAX_CAPACITY = 3;
    private static final double NEW_CUSTOMER_PROB = .007;
    private static final Map<String, Graph<MultiAttributeData>> GRAPH_CACHE = newHashMap();
    private static final long TEST_STOP_TIME = 20 * 60 * 1000;
    private static final int TEST_SPEED_UP = 1;

    private MasProject() {
    }

    /**
     * Starts the {@link MasProject}.
     *
     * @param args The first option may optionally indicate the end time of the simulation.
     */
    public static void main(@Nullable String[] args) {
        final long endTime = args != null && args.length >= 1 ? Long
                .parseLong(args[0]) : Long.MAX_VALUE;

        run(false, endTime, null /* new Display() */, null, null);
    }

    /**
     * Run the example.
     *
     * @param testing If <code>true</code> enables the test mode.
     */
    public static void run(boolean testing) {
        run(testing, Long.MAX_VALUE, null, null, null);
    }

    /**
     * Starts the example.
     *
     * @param testing Indicates whether the method should run in testing mode.
     * @param endTime The time at which simulation should stop.
     * @param display The display that should be used to show the ui on.
     * @param m       The monitor that should be used to show the ui on.
     * @param list    A listener that will receive callbacks from the ui.
     * @return The simulator instance.
     */
    public static Simulator run(boolean testing, final long endTime, @Nullable Display display, @Nullable Monitor m, @Nullable Listener list) {

        final View.Builder view = createGui(testing, display, m, list);

        final Simulator simulator = Simulator.builder()
                .addModel(RoadModelBuilders.plane()
                        .withMinPoint(Helper.convertToPointInBoundaries(Helper.ROADMODEL_MIN_POINT))
                        .withMaxPoint(Helper.convertToPointInBoundaries(Helper.ROADMODEL_MAX_POINT))
                        .withMaxSpeed(MAX_SPEED)
                )
                .addModel(DefaultPDPModel.builder())
                .addModel(view)
                .build();

        final RandomGenerator rng = simulator.getRandomGenerator();

        final RoadModel roadModel = simulator.getModelProvider().getModel(RoadModel.class);

        for (int i = 0; i < NUM_TAXIS; i++) {
            simulator.register(new Taxi(roadModel.getRandomPosition(rng), TAXI_CAPACITY));
        }
        MySQLDataLoader dataLoader = new MySQLDataLoader();

        simulator.addTickListener(new TickListener() {
            @Override
            public void tick(TimeLapse time) {
//                System.out.println("Current DateTime: " + Helper.START_TIME.plusNanos(time.getStartTime()* 1000000).toString());

                if (time.getStartTime() > endTime) {
                    simulator.stop();
                }

                for (Customer c : roadModel.getObjectsOfType(Customer.class)) {
                    simulator.unregister(c);
                }

                List<HistoricalData> data = dataLoader.read(
                        Helper.START_TIME.plusNanos(time.getStartTime() * 1000000),
                        Helper.START_TIME.plusNanos(time.getEndTime() * 1000000)
                );

                for (HistoricalData h : data) {
                    simulator.register(new Customer(h));
                }
            }

            @Override
            public void afterTick(TimeLapse timeLapse) {
            }
        });
        simulator.start();

        return simulator;
    }

    static View.Builder createGui(
            boolean testing,
            @Nullable Display display,
            @Nullable Monitor m,
            @Nullable Listener list) {

        FieldGenerator f = new FieldGenerator();
        DiscreteField df = f.load();

        View.Builder view = View.builder()
                .with(PlaneRoadModelRenderer.builder())
                .with(DiscreteFieldRenderer.builder().withField(df))
                .with(RoadUserRenderer.builder()
                        .withImageAssociation(
                                Taxi.class, "/graphics/flat/taxi-32.png")
                        .withImageAssociation(
                                Customer.class, "/graphics/flat/person-red-32.png")
                        .withToStringLabel())
                .with(TaxiRenderer.builder(TaxiRenderer.Language.ENGLISH))
                .withTitleAppendix("MAS Project 2017 - Evert Etienne & Olivier Kamers");

        if (testing) {
            view = view.withAutoClose()
                    .withAutoPlay()
                    .withSimulatorEndTime(TEST_STOP_TIME)
                    .withSpeedUp(TEST_SPEED_UP);
        } else if (m != null && list != null && display != null) {
            view = view.withMonitor(m)
                    .withSpeedUp(SPEED_UP)
                    .withResolution(m.getClientArea().width, m.getClientArea().height)
                    .withDisplay(display)
                    .withCallback(list)
                    .withAsync()
                    .withAutoPlay()
                    .withAutoClose();
        }
        return view;
    }

    /**
     * A customer with very permissive time windows.
     */
    static class Customer extends Parcel {
        Customer(ParcelDTO dto) {
            super(dto);
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

        @Override
        public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        }

        @Override
        public String toString() {
            return "Customer{" +
                    this.getPickupLocation().toString() +
                    "}";
        }
    }
}
