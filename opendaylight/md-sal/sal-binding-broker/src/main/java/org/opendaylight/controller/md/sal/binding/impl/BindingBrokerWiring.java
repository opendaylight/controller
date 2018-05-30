/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

/**
 * Provides the implementations of the APIs.
 *
 * <p>Intended to be usable in a standalone environment (non-OSGi/Karaf). Also
 * internally used by the Blueprint XML to expose the same as OSGi services.
 * This class does not require (depend on) the Guice dependency injection
 * framework, but can we used with it.
 *
 * @author Michael Vorburger.ch
 */
public class BindingBrokerWiring implements AutoCloseable {

    private final BindingToNormalizedNodeCodec bindingToNormalizedNodeCodec;
    private final ListenerRegistration<SchemaContextListener> mappingCodecListenerReg;

    public BindingBrokerWiring(ClassLoadingStrategy classLoadingStrategy, DOMSchemaService schemaService) {
        // Runtime binding/normalized mapping service
        bindingToNormalizedNodeCodec = BindingToNormalizedNodeCodecFactory.newInstance(classLoadingStrategy);
        // Register the BindingToNormalizedNodeCodec with the SchemaService as a SchemaContextListener
        mappingCodecListenerReg = BindingToNormalizedNodeCodecFactory
                                    .registerInstance(bindingToNormalizedNodeCodec, schemaService);
    }

    @Override
    public void close() throws Exception {
        mappingCodecListenerReg.close();
    }

    public BindingToNormalizedNodeCodec getBindingToNormalizedNodeCodec() {
        return bindingToNormalizedNodeCodec;
    }


}
