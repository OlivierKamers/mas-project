package core.statistics;

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

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.ui.renderers.PanelRenderer;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;


/**
 * A UI panel that gives a live view of the current statistics of a simulation.
 *
 * @author Rinde van Lon
 */
public final class StatsPanel extends Model.AbstractModel<Void> implements PanelRenderer, TickListener {

    private static final int PREFERRED_SIZE = 300;
    private final StatsTracker statsTracker;
    Optional<Table> statsTable;

    /**
     * Create a new instance using the specified {@link StatsTracker} which
     * supplies the statistics.
     *
     * @param stats The tracker to use.
     */
    StatsPanel(StatsTracker stats) {
        statsTracker = stats;
        statsTable = Optional.absent();
    }

    public static Builder builder() {
        return new AutoValue_StatsPanel_Builder(new StatsTracker());
    }

    @Override
    public void initializePanel(Composite parent) {
        final FillLayout layout = new FillLayout();
        layout.marginWidth = 2;
        layout.marginHeight = 2;
        layout.type = SWT.VERTICAL;
        parent.setLayout(layout);

        final Table table = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        statsTable = Optional.of(table);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        final String[] statsTitles = new String[]{"Variable", "Value"};
        for (String statsTitle : statsTitles) {
            final TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(statsTitle);
        }
        final StatisticsDTO stats = statsTracker.getStatistics();
        final Field[] fields = stats.getClass().getFields();
        for (final Field f : fields) {
            final TableItem ti = new TableItem(table, 0);
            ti.setText(0, f.getName());
            try {
                ti.setText(1, f.get(stats).toString());
            } catch (final IllegalArgumentException | IllegalAccessException e) {
                ti.setText(1, e.getMessage());
            }
        }
        for (int i = 0; i < statsTitles.length; i++) {
            table.getColumn(i).pack();
        }
    }

    @Override
    public int preferredSize() {
        return PREFERRED_SIZE;
    }

    @Override
    public int getPreferredPosition() {
        return SWT.LEFT;
    }

    @Override
    public String getName() {
        return "Statistics";
    }

    @Override
    public void tick(TimeLapse timeLapse) {
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }

    @Override
    public void render() {
        final StatisticsDTO stats = statsTracker.getStatistics();

        final Field[] fields = stats.getClass().getFields();
        if (statsTable.get().isDisposed()
                || statsTable.get().getDisplay().isDisposed()) {
            return;
        }
        statsTable.get().getDisplay().syncExec(() -> {
            if (statsTable.get().isDisposed()) {
                return;
            }
            for (int i = 0; i < fields.length; i++) {
                try {
                    statsTable.get().getItem(i).setText(1, fields[i].get(stats).toString());
                } catch (final ConcurrentModificationException | IllegalArgumentException | IllegalAccessException e) {
                    statsTable.get().getItem(i).setText(1, e.getMessage());
                }
            }
        });

    }

    @Override
    public boolean register(Void element) {
        return false;
    }

    @Override
    public boolean unregister(Void element) {
        return false;
    }

    @AutoValue
    public abstract static class Builder extends ModelBuilder.AbstractModelBuilder<StatsPanel, Void> {

        Builder() {
            setDependencies(
                    Clock.class,
                    RoadModel.class,
                    PDPModel.class);
        }

        abstract StatsTracker st();

        @Override
        public StatsPanel build(@NotNull DependencyProvider dependencyProvider) {
            return new StatsPanel(StatsTracker.builder().build(dependencyProvider));
        }

    }
}
