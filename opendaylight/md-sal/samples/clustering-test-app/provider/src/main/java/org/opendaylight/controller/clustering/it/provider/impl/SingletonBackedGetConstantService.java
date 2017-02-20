/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingletonBackedGetConstantService implements ClusterSingletonService {

    private static final Logger LOG = LoggerFactory.getLogger(SingletonBackedGetConstantService.class);

    private static final QName GET_SINGLETON_CONSTANT =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target",
                    "2017-02-15","get-singleton-constant");
    private static final ServiceGroupIdentifier SERVICE_GROUP_IDENTIFIER =
            ServiceGroupIdentifier.create("singleton-backed-get-constant");



    private final ClusterSingletonServiceProvider singletonService;
    private final DOMRpcProviderService domRpcProviderService;
    private final String constant;
    private DOMRpcImplementationRegistration<GetConstantService> registration;


    public SingletonBackedGetConstantService(final ClusterSingletonServiceProvider singletonService,
                                             final DOMRpcProviderService domRpcProviderService,
                                             final String constant) {
        this.singletonService = singletonService;
        this.domRpcProviderService = domRpcProviderService;
        this.constant = constant;

        singletonService.registerClusterSingletonService(this);
    }

    @Override
    public void instantiateServiceInstance() {

        LOG.debug("Instantiating singleton backed get-constant, value: {}", constant);

        final DOMRpcIdentifier rpcId =
                DOMRpcIdentifier.create(SchemaPath.create(Collections.singletonList(GET_SINGLETON_CONSTANT), true));
//        registration = domRpcProviderService.registerRpcImplementation(new GetConstantService(constant), rpcId);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {

        LOG.debug("Closing singleton backed get-constant.");

        registration.close();
        return Futures.immediateFuture(null);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return SERVICE_GROUP_IDENTIFIER;
    }
}
