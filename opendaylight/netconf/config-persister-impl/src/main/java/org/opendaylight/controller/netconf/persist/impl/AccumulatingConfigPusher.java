package org.opendaylight.controller.netconf.persist.impl;

import java.util.List;

import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;

public class AccumulatingConfigPusher {

    private ConfigPusher configPusher;
    private List<ConfigSnapshotHolder> configs;

    public void setConfigPusher(ConfigPusher configPusher)  {
        this.configPusher = configPusher;
        // Push accumulated configs
        configPusher.pushConfigs(this.configs);
    }

    public void pushConfigs(List<ConfigSnapshotHolder> configs) throws NetconfDocumentedException {
        // Note, configPusher will be set at some point after startup,
        // So if its not set, accumulate configs until it is
        if(configPusher != null) {
            // This also needs error handling, see ConfigPersisterActivator run() at line 158
            configPusher.pushConfigs(configs);
        } else {
            this.configs.addAll(configs);
        }
    }

}
