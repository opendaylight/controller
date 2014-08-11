package org.opendaylight.controller.config.persist.api;

import java.util.List;

public interface ConfigPusher {
    public void pushConfigs(List<? extends ConfigSnapshotHolder> configs) throws InterruptedException;
}
