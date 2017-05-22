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
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import core.statistics.StatsPanel;
import core.statistics.StatsTracker;
import org.apache.commons.math3.random.RandomGenerator;

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
    public static final double MAX_SPEED = 50;
    private static final int NUM_TAXIS = 200;
    private static final int TAXI_CAPACITY = 6;
    private static final int SPEED_UP = 5;
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
//        final long endTime = args != null && args.length >= 1 ? Long
//                .parseLong(args[0]) : Long.MAX_VALUE;

        run();
    }

    /**
     * Starts the project.
     *
     * @return The simulator instance.
     */
    public static Simulator run() {

        FieldGenerator fieldGenerator = new FieldGenerator();
        DiscreteField discreteField = fieldGenerator.load();

        final View.Builder view = createGui(discreteField);

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

        // Register random Taxis
        for (int i = 0; i < NUM_TAXIS; i++) {
            simulator.register(new Taxi(i, roadModel.getRandomPosition(rng), TAXI_CAPACITY, discreteField));
        }

        MySQLDataLoader dataLoader = new MySQLDataLoader();

        simulator.addTickListener(new TickListener() {
            @Override
            public void tick(TimeLapse time) {
//                System.out.println(Helper.START_TIME.plusNanos(time.getEndTime() * 1000000));

                if (Helper.START_TIME.plusNanos(time.getEndTime() * 1000000).isAfter(Helper.STOP_TIME)) {
                    if (roadModel.getObjectsOfType(Customer.class).isEmpty() &&
                            roadModel.getObjectsOfType(Taxi.class).stream().allMatch(t -> t.getState() == Taxi.TaxiState.IDLE)) {
                        simulator.stop();
                    }
                } else {
                    List<HistoricalData> data = dataLoader.read(
                            Helper.START_TIME.plusNanos(time.getStartTime() * 1000000),
                            Helper.START_TIME.plusNanos(time.getEndTime() * 1000000)
                    );

                    for (HistoricalData h : data) {
                        float chance = r.nextFloat();
                        if (chance <= CUSTOMER_SAMPLE) {
                            simulator.register(new Customer(h, time));
                        }
                    }
                }
            }

            @Override
            public void afterTick(TimeLapse timeLapse) {
            }
        });

        simulator.start();

        // simulation is done, lets print the statistics!
        System.out.println(simulator.getModelProvider().getModel(StatsTracker.class).getStatistics());

        return simulator;
    }

    static View.Builder createGui(DiscreteField df) {
        return View.builder()
                .withSpeedUp(SPEED_UP)
                .with(PlaneRoadModelRenderer.builder())
                .with(DiscreteFieldRenderer.builder()
                        .withField(df)
                )
                .with(StatsPanel.builder())
//                .with(CommRenderer.builder().withReliabilityColors())
                .with(RoadUserRenderer.builder()
                                .withImageAssociation(
                                        Taxi.class, "/graphics/flat/taxi-32.png")
                                .withImageAssociation(
                                        Customer.class, "/graphics/flat/hailing-cab-32.png")
//                        .withToStringLabel()
                )
                .with(TaxiRenderer.builder(TaxiRenderer.Language.ENGLISH))
                .withTitleAppendix("MAS Project 2017 - Evert Etienne & Olivier Kamers")
                .withFullScreen()
                .withAutoPlay()
                .withAutoClose();
    }
}
