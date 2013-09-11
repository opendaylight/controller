package org.opendaylight.controller.containermanager;

import java.io.Serializable;

import org.opendaylight.controller.sal.core.UpdateType;

public class ContainerChangeEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private ContainerConfig config;
    private UpdateType update;

    public ContainerChangeEvent(ContainerConfig config, UpdateType update) {
        this.config = config;
        this.update = update;
    }

    public UpdateType getUpdateType() {
        return update;
    }

    public ContainerConfig getConfig() {
        return config;
    }
}
