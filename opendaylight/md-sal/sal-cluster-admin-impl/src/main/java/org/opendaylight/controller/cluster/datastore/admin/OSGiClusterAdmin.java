/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin;

import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.eos.akka.NativeEosService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
@Component(immediate = true)
public final class OSGiClusterAdmin {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiClusterAdmin.class);

    @Reference(target = "(type=distributed-config)")
    DistributedDataStoreInterface configDatastore = null;
    @Reference(target = "(type=distributed-operational)")
    DistributedDataStoreInterface operDatastore = null;
    @Reference
    BindingNormalizedNodeSerializer serializer = null;
    @Reference
    RpcProviderService rpcProviderService = null;
    @Reference
    NativeEosService nativeEosService = null;

    private ObjectRegistration<?> reg;

    @Activate
    void activate() {
        reg = rpcProviderService.registerRpcImplementation(ClusterAdminService.class,
            new ClusterAdminRpcService(configDatastore, operDatastore, serializer, nativeEosService));
        LOG.info("Cluster Admin services started");
    }

    @Deactivate
    void deactivate() {
        reg.close();
        LOG.info("Cluster Admin services stopped");
    }
}
