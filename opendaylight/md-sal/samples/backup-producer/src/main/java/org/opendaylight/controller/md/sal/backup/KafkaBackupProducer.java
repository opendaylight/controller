/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.backup;

import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;

/**
 * Bean producing the {@link KafkaBackupProducerService} and registering it as a ClusterSingleton
 */
public class KafkaBackupProducer {

    private DOMDataBroker domDataBroker;
    private ClusterSingletonServiceProvider clusterSingletonServiceProvider;

    public void setDomDataBroker(DOMDataBroker domDataBroker) {
        this.domDataBroker = domDataBroker;
    }

    public void setClusterSingletonServiceProvider(ClusterSingletonServiceProvider clusterSingletonServiceProvider) {
        this.clusterSingletonServiceProvider = clusterSingletonServiceProvider;
    }

    public void init() {
        clusterSingletonServiceProvider.registerClusterSingletonService(new KafkaBackupProducerService(domDataBroker));
    }
}
