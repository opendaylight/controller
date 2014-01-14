/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker;


import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.core.api.Provider;
import org.osgi.framework.BundleContext;

public class SalRemoteServiceBroker implements Broker,AutoCloseable {

    @Override
    public void close() throws Exception {

    }

    @Override
    public ConsumerSession registerConsumer(Consumer cons, BundleContext context) {
        return null;
    }

    @Override
    public ProviderSession registerProvider(Provider prov, BundleContext context) {
        return null;
    }
}
