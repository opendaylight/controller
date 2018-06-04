/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.dom.impl;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.trace.api.TracingDOMDataBroker;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsaltrace.rev160908.Config;

/**
 * Programmatic "wiring" for dependency injection.
 *
 * <p>
 * See org.opendaylight.controller.md.sal.binding.impl.BindingBrokerWiring.
 *
 * @author Michael Vorburger.ch
 */
public class TracingBrokerWiring {

    private final TracingDOMDataBroker tracingDOMDataBroker;

    public TracingBrokerWiring(DOMDataBroker realDefaultDOMBroker, Config tracingConfig,
            BindingNormalizedNodeSerializer codec) {
        this.tracingDOMDataBroker = new TracingBroker(realDefaultDOMBroker, tracingConfig, codec);
    }

    public TracingDOMDataBroker getTracingDOMDataBroker() {
        return tracingDOMDataBroker;
    }

    // TODO pingpong, after c/72656 (CONTROLLER-1834) is sorted...

}
