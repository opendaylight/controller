package org.opendaylight.controller.md.statistics.manager.impl;

public class StatisticsManagerConfig {
    private final int maxNodesForCollector;
    private final int minRequestNetMonitorInterval;

    private StatisticsManagerConfig(StatisticsManagerConfigBuilder builder) {
        this.maxNodesForCollector = builder.getMaxNodesForCollector();
        this.minRequestNetMonitorInterval = builder.getMinRequestNetMonitorInterval();
    }

    public int getMaxNodesForCollector() {
        return maxNodesForCollector;
    }

    public int getMinRequestNetMonitorInterval() {
        return minRequestNetMonitorInterval;
    }

    public static StatisticsManagerConfigBuilder builder() {
        return new StatisticsManagerConfigBuilder();
    }

    public static class StatisticsManagerConfigBuilder {
        private int maxNodesForCollector;
        private int minRequestNetMonitorInterval;

        public int getMaxNodesForCollector() {
            return maxNodesForCollector;
        }

        public void setMaxNodesForCollector(int maxNodesForCollector) {
            this.maxNodesForCollector = maxNodesForCollector;
        }

        public int getMinRequestNetMonitorInterval() {
            return minRequestNetMonitorInterval;
        }

        public void setMinRequestNetMonitorInterval(int minRequestNetMonitorInterval) {
            this.minRequestNetMonitorInterval = minRequestNetMonitorInterval;
        }

        public StatisticsManagerConfig build() {
            return new StatisticsManagerConfig(this);
        }
    }
}
