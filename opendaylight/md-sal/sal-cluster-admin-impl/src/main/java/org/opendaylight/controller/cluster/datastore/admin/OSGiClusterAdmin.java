/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin;

import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.eos.akka.DataCenterControl;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { })
public final class OSGiClusterAdmin {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiClusterAdmin.class);

    private final Registration reg;

    @Activate
    public OSGiClusterAdmin(
            @Reference(target = "(type=distributed-config)") final DistributedDataStoreInterface configDatastore,
            @Reference(target = "(type=distributed-operational)") final DistributedDataStoreInterface operDatastore,
            @Reference final BindingNormalizedNodeSerializer serializer,
            @Reference final RpcProviderService rpcProviderService,
            @Reference final DataCenterControl dataCenterControls,
            @Reference final DataCenterControl dataCenterControl) {
        reg = rpcProviderService.registerRpcImplementation(ClusterAdminService.class,
            new ClusterAdminRpcService(configDatastore, operDatastore, serializer, dataCenterControl));
        LOG.info("Cluster Admin services started");
    }

    @Deactivate
    void deactivate() {
        reg.close();
        LOG.info("Cluster Admin services stopped");
    }
}
