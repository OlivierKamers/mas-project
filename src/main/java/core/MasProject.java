package core;/*
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
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

import javax.annotation.Nullable;
import javax.measure.unit.SI;
import java.util.List;
import java.util.Random;

/**
 * MAS Project 2017
 *
 * @author Evert Etienne & Olivier Kamers
 */
public final class MasProject {
    private static final double MAX_SPEED = 50;
    private static final int NUM_TAXIS = 50;
    private static final int TAXI_CAPACITY = 10;
    private static final int SPEED_UP = 3;
    private static final long TEST_STOP_TIME = 20 * 60 * 1000;
    private static final int TEST_SPEED_UP = 1;
    private static final double CUSTOMER_SAMPLE = 0.2;

    private static Random r = new Random();

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

        FieldGenerator fieldGenerator = new FieldGenerator();
        DiscreteField discreteField = fieldGenerator.load();

        final View.Builder view = createGui(testing, display, m, list, discreteField);

        final Simulator simulator = Simulator.builder()
                .addModel(RoadModelBuilders.plane()
                        .withMinPoint(Helper.convertToPointInBoundaries(Helper.ROADMODEL_MIN_POINT))
                        .withMaxPoint(Helper.convertToPointInBoundaries(Helper.ROADMODEL_MAX_POINT))
                        .withMaxSpeed(MAX_SPEED)
                )
                .addModel(CommModel.builder())
                .addModel(DefaultPDPModel.builder())
                .addModel(view)
                .addModel(StatsTracker.builder())
                .setTimeUnit(SI.MILLI(SI.SECOND))
                .setTickLength(1000L)
                .build();

        final RandomGenerator rng = simulator.getRandomGenerator();

        final RoadModel roadModel = simulator.getModelProvider().getModel(RoadModel.class);

        for (int i = 0; i < NUM_TAXIS; i++) {
            simulator.register(new Taxi(i, roadModel.getRandomPosition(rng), TAXI_CAPACITY, discreteField));
        }
        MySQLDataLoader dataLoader = new MySQLDataLoader();

        simulator.addTickListener(new TickListener() {
            @Override
            public void tick(TimeLapse time) {
//                System.out.println("Customers " + roadModel.getObjectsOfType(core.Customer.class).size());
//                System.out.println("Current DateTime: " + core.Helper.START_TIME.plusNanos(time.getStartTime() * 1000000).toString());
//                System.out.println(time);

                if (time.getStartTime() > endTime) {
                    simulator.stop();
                }

                // For debugging: clear the previous customers when loading new ones
//                for (core.Customer c : roadModel.getObjectsOfType(core.Customer.class)) {
//                    simulator.unregister(c);
//                }

                if (Helper.START_TIME.plusNanos(time.getEndTime() * 1000000).isBefore(Helper.STOP_TIME)) {
                    List<HistoricalData> data = dataLoader.read(
                            Helper.START_TIME.plusNanos(time.getStartTime() * 1000000),
                            Helper.START_TIME.plusNanos(time.getEndTime() * 1000000)
                    );

                    for (HistoricalData h : data) {
                        float chance = r.nextFloat();
                        if (chance <= CUSTOMER_SAMPLE) {
                            simulator.register(new Customer(h));

                            // For debugging: print the data
//                            System.out.println(h.toString());
                        }
                    }
                }

                // For debugging: only use with small field matrix
//                discreteField.printField(discreteField.getFrameIndexForTime(time.getTime()));

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
            @Nullable Listener list,
            DiscreteField df) {

        View.Builder view = View.builder()
                .withSpeedUp(SPEED_UP)
                .with(PlaneRoadModelRenderer.builder())
                .with(DiscreteFieldRenderer.builder()
                        .withField(df)
                )
//                .with(CommRenderer.builder().withReliabilityColors())
                .with(RoadUserRenderer.builder()
                        .withImageAssociation(
                                Taxi.class, "/graphics/flat/taxi-32.png")
                        .withImageAssociation(
                                Customer.class, "/graphics/flat/person-red-32.png")
                        .withToStringLabel()
                )
                .with(TaxiRenderer.builder(TaxiRenderer.Language.ENGLISH))
                .withTitleAppendix("MAS Project 2017 - Evert Etienne & Olivier Kamers")
                .withFullScreen();

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


}
