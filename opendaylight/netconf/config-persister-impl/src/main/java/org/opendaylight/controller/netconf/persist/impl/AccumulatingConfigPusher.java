package org.opendaylight.controller.netconf.persist.impl;

import java.util.List;

import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccumulatingConfigPusher implements org.opendaylight.controller.config.persist.api.AccumulatingConfigPusher {

    private static final Logger logger = LoggerFactory.getLogger(AccumulatingConfigPusher.class);
    private ConfigPusher configPusher;
    private List<ConfigSnapshotHolder> configs;

    public void setConfigPusher(ConfigPusher configPusher)  {
        this.configPusher = configPusher;
        // Push accumulated configs
        try {
            configPusher.pushConfigs(this.configs);
        } catch (NetconfDocumentedException e) {
            logger.error("Error pushing configs {}",configs);
        }
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
