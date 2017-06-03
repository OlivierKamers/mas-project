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
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import core.statistics.StatisticsDTO;
import core.statistics.StatsPanel;
import core.statistics.StatsTracker;
import org.apache.commons.cli.*;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.measure.unit.SI;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MAS Project 2017
 *
 * @author Evert Etienne & Olivier Kamers
 */
public final class MasProject {
    public static final double MAX_SPEED = 15;
    static final int TAXI_CAPACITY = 5;
    private static final int NUM_TAXIS = 10000;
    private static final int SPEED_UP = 5;
    private static final double DEFAULT_SAMPLE = 0.02;

    private static Random r = new Random();

    private MasProject() {
    }

    /**
     * Starts the {@link MasProject}.
     *
     * @param args The first option may optionally indicate the end time of the simulation.
     */
    public static void main(@Nullable String[] args) {
        System.out.println(Arrays.toString(args));
        Options options = new Options();

        options.addOption(new Option("g", "gui", false, "Run with GUI"));
        options.addOption(new Option("f", "field", false, "Enable field"));
        options.addOption(new Option("t", "trade", false, "Enable trading"));
        options.addOption(Option.builder("s").longOpt("sample").desc("Data sampling factor").hasArg().type(Number.class).build());
        options.addOption(Option.builder("m").longOpt("mtxstep").desc("Matrix Subdivision Step").hasArg().type(Number.class).build());
        options.addOption(Option.builder("r").longOpt("resolution").desc("Minutes per time frame").hasArg().type(Number.class).build());
        options.addOption(Option.builder("i").longOpt("influence").desc("Taxi repulsion influence range").hasArg().type(Number.class).build());
        options.addOption(Option.builder("F").longOpt("frange").desc("Range for field analysis").hasArg().type(Number.class).build());
        options.addOption(Option.builder("l").longOpt("idlelimit").desc("Distance limit for idle driving").hasArg().type(Number.class).build());

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
            boolean showGUI = cmd.hasOption("gui");
            boolean useField = cmd.hasOption("field");
            boolean useTrading = cmd.hasOption("trade");
            double sample = cmd.hasOption("sample") ? (double) cmd.getParsedOptionValue("sample") : DEFAULT_SAMPLE;
            int matrixStep = cmd.hasOption("mtxstep") ? ((Number) cmd.getParsedOptionValue("mtxstep")).intValue() : 0;
            int minPerFrame = cmd.hasOption("resolution") ? ((Number) cmd.getParsedOptionValue("resolution")).intValue() : 1;
            double taxiInfluenceRange = cmd.hasOption("influence") ? (double) cmd.getParsedOptionValue("influence") : DiscreteField.DEFAULT_TAXI_INFLUENCE_RANGE;
            int fieldRange = cmd.hasOption("frange") ? ((Number) cmd.getParsedOptionValue("frange")).intValue() : Taxi.DEFAULT_FIELD_RANGE;
            double idleTravelLimit = cmd.hasOption("idlelimit") ? (double) cmd.getParsedOptionValue("idlelimit") : Double.MAX_VALUE;

            run(args, showGUI, useField, useTrading, sample, matrixStep, minPerFrame, taxiInfluenceRange, fieldRange, idleTravelLimit);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("MAS-project", options);

            System.exit(1);
        }
    }

    /**
     * Starts the project.
     */
    public static void run(String[] args, boolean showGUI, boolean useField, boolean useTrading, double sample, int matrixStep, int minPerFrame, double taxiInfluenceRange, int fieldRange, double idleTravelLimit) {
        DiscreteField discreteField = null;
        if (useField) {
            FieldGenerator fieldGenerator = new FieldGenerator(matrixStep, minPerFrame);
            discreteField = fieldGenerator.load(taxiInfluenceRange);
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
        final DefaultPDPModel pdpModel = simulator.getModelProvider().getModel(DefaultPDPModel.class);
        final CommModel commModel = simulator.getModelProvider().getModel(CommModel.class);

        // Register random Taxis
        for (int i = 0; i < NUM_TAXIS * sample; i++) {
            simulator.register(new Taxi(i, roadModel.getRandomPosition(rng), TAXI_CAPACITY, discreteField, useTrading, fieldRange, idleTravelLimit));
        }

        MySQLDataLoader dataLoader = new MySQLDataLoader();


        ArrayList<Integer> amountOfIdleTaxis = new ArrayList<>();
        ArrayList<Integer> amountOfWaitingCustomers = new ArrayList<>();

        simulator.addTickListener(new TickListener() {
            @Override
            public void tick(@NotNull TimeLapse time) {
                Collection<Parcel> parcels = new ArrayList<>(pdpModel.getParcels(PDPModel.ParcelState.DELIVERED));
                parcels.forEach(c -> {
                    commModel.unregister((Customer) c);
                    pdpModel.unregister(c);
                    roadModel.unregister(c);
                });

                if (time.getStartTime() % (15 * 60 * 1000) == 0) {
                    // Print progress every 15 simulated minutes
                    System.out.println(LocalTime.now().toString() + " ==> " + Helper.START_TIME.plusNanos(time.getStartTime() * 1000000));
                }
                amountOfIdleTaxis.add((int) roadModel.getObjectsOfType(Taxi.class).stream().filter(t -> t.getState() == Taxi.TaxiState.IDLE).count());
                amountOfWaitingCustomers.add(roadModel.getObjectsOfType(Customer.class).size());

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
        StatisticsDTO stats = simulator.getModelProvider().getModel(StatsTracker.class).getStatistics();
        stats.setAmountOfIdleTaxis(amountOfIdleTaxis);
        stats.setAmountOfWaitingCustomers(amountOfWaitingCustomers);
        List<Double> totalIdleMovements = roadModel.getObjectsOfType(Taxi.class)
                .stream()
                .map(Taxi::getIdleMoveProgress)
                .map(l -> l
                        .stream()
                        .mapToDouble(mp -> mp.distance().getValue())
                        .sum())
                .collect(Collectors.toList());
        stats.setTotalIdleMovement(totalIdleMovements);
        ArrayList<Double> tradeProfits = roadModel.getObjectsOfType(Taxi.class)
                .stream()
                .map(Taxi::getTradeProfits)
                .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
        stats.setTradeProfits(tradeProfits);
        stats.setArgs(args);
        System.out.println(stats);
        stats.save();
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
