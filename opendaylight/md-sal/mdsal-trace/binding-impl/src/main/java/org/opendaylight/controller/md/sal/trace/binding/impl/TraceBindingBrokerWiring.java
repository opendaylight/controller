/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.trace.api.TracingDOMDataBroker;
import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;

/**
 * Programmatic "wiring" for dependency injection.
 *
 * <p>
 * See org.opendaylight.controller.md.sal.binding.impl.BindingBrokerWiring.
 *
 * @author Michael Vorburger.ch
 */
public class TraceBindingBrokerWiring {

    private final DataBroker dataBroker;
    private final DataBroker pingPongDataBroker;

    public TraceBindingBrokerWiring(ClassLoadingStrategy classLoadingStrategy,
            BindingToNormalizedNodeCodec mappingCodec, TracingDOMDataBroker tracingDOMDataBroker,
            TracingDOMDataBroker tracingPingPongDOMDataBroker) {

        dataBroker = new BindingDOMDataBrokerAdapter(tracingDOMDataBroker, mappingCodec);
        pingPongDataBroker = new BindingDOMDataBrokerAdapter(tracingPingPongDOMDataBroker, mappingCodec);
    }

    public DataBroker getTracingDataBroker() {
        return dataBroker;
    }

    public DataBroker getTracingPingPongDataBroker() {
        return pingPongDataBroker;
    }

}
