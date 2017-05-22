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
import org.apache.commons.cli.*;
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
    public static final double MAX_SPEED = 15;
    public static final int TAXI_CAPACITY = 5;
    private static final int NUM_TAXIS = 10000;
    private static final int SPEED_UP = 5;
    private static final double DEFAULT_SAMPLE = 0.05;

    private static Random r = new Random();

    private MasProject() {
    }

    /**
     * Starts the {@link MasProject}.
     *
     * @param args The first option may optionally indicate the end time of the simulation.
     */
    public static void main(@Nullable String[] args) {
        Options options = new Options();

        options.addOption(new Option("g", "gui", false, "Run with GUI"));
        options.addOption(new Option("f", "field", false, "Enable field"));
        options.addOption(Option.builder("s").longOpt("sample").desc("Data sampling factor").hasArg().type(Number.class).build());

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
            boolean showGUI = cmd.hasOption("gui");
            boolean useField = cmd.hasOption("field");
            double sample = cmd.hasOption("sample") ? (double) cmd.getParsedOptionValue("sample") : DEFAULT_SAMPLE;

            run(showGUI, useField, sample);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("MAS-project", options);

            System.exit(1);
        }
    }

    /**
     * Starts the project.
     */
    public static void run(boolean showGUI, boolean useField, double sample) {
        DiscreteField discreteField = null;
        if (useField) {
            FieldGenerator fieldGenerator = new FieldGenerator();
            discreteField = fieldGenerator.load();
        }

        Simulator.Builder simulatorBuilder = Simulator.builder()
                .addModel(RoadModelBuilders.plane()
                        .withMinPoint(Helper.convertToPointInBoundaries(Helper.ROADMODEL_MIN_POINT))
                        .withMaxPoint(Helper.convertToPointInBoundaries(Helper.ROADMODEL_MAX_POINT))
                        .withMaxSpeed(MAX_SPEED)
                )
                .addModel(CommModel.builder())
                .addModel(DefaultPDPModel.builder())
                .addModel(StatsTracker.builder())
                .setTimeUnit(SI.MILLI(SI.SECOND))
                .setTickLength(1000L);

        if (showGUI) simulatorBuilder.addModel(createGui(discreteField));

        final Simulator simulator = simulatorBuilder.build();

        final RandomGenerator rng = simulator.getRandomGenerator();

        final RoadModel roadModel = simulator.getModelProvider().getModel(RoadModel.class);

        // Register random Taxis
        for (int i = 0; i < NUM_TAXIS * sample; i++) {
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
                        if (chance <= sample) {
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
    }

    static View.Builder createGui(DiscreteField df) {
        View.Builder builder = View.builder();

        if (df != null) {
            builder = builder.with(DiscreteFieldRenderer.builder()
                    .withField(df)
            );
        }

        builder = builder
                .withSpeedUp(SPEED_UP)
                .with(PlaneRoadModelRenderer.builder())
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
        return builder;
    }
}
