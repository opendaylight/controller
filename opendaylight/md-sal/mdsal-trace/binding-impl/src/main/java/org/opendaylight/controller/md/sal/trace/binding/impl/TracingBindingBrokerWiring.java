/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.spi.AdapterFactory;
import org.opendaylight.controller.md.sal.trace.api.TracingDOMDataBroker;

/**
 * Programmatic "wiring" for dependency injection.
 *
 * <p>See org.opendaylight.controller.md.sal.binding.impl.BindingBrokerWiring.
 *
 * @author Michael Vorburger.ch
 */
public class TracingBindingBrokerWiring {

    private final DataBroker dataBroker;
    private final DataBroker pingPongDataBroker;

    public TracingBindingBrokerWiring(TracingDOMDataBroker tracingDOMDataBroker,
            TracingDOMDataBroker tracingPingPongDOMDataBroker, AdapterFactory adapterFactory) {

        dataBroker = adapterFactory.createDataBroker(tracingDOMDataBroker);
        pingPongDataBroker = adapterFactory.createDataBroker(tracingPingPongDOMDataBroker);
    }

    public DataBroker getTracingDataBroker() {
        return dataBroker;
    }

    public DataBroker getTracingPingPongDataBroker() {
        return pingPongDataBroker;
    }

}
