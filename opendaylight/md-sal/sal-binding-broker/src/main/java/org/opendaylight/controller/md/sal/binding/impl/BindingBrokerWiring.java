/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import javassist.ClassPool;
import org.opendaylight.controller.md.sal.binding.compat.HeliumRpcProviderRegistry;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
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
 * @author Michael Vorburger.ch, partially based on refactored code originally by Thomas Pantelis
 */
public class BindingBrokerWiring implements AutoCloseable {

    private static final JavassistUtils JAVASSIST = JavassistUtils.forClassPool(ClassPool.getDefault());

    private final BindingToNormalizedNodeCodec bindingToNormalizedNodeCodec;
    private final ListenerRegistration<SchemaContextListener> mappingCodecListenerReg;
    private final RpcProviderRegistry rpcProviderRegistry;

    public BindingBrokerWiring(ClassLoadingStrategy classLoadingStrategy, DOMSchemaService schemaService,
            DOMRpcService domRpcService, DOMRpcProviderService domRpcProviderService) {
        // Runtime binding/normalized mapping service
        BindingNormalizedNodeCodecRegistry codecRegistry
            = new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(JAVASSIST));
        bindingToNormalizedNodeCodec = new BindingToNormalizedNodeCodec(classLoadingStrategy, codecRegistry, true);

        // Register the BindingToNormalizedNodeCodec with the SchemaService as a SchemaContextListener
        mappingCodecListenerReg = schemaService.registerSchemaContextListener(bindingToNormalizedNodeCodec);

        // Binding RPC Registry Service
        BindingDOMRpcServiceAdapter bindingDOMRpcServiceAdapter
            = new BindingDOMRpcServiceAdapter(domRpcService, bindingToNormalizedNodeCodec);
        BindingDOMRpcProviderServiceAdapter bindingDOMRpcProviderServiceAdapter
            = new BindingDOMRpcProviderServiceAdapter(domRpcProviderService, bindingToNormalizedNodeCodec);
        rpcProviderRegistry
            = new HeliumRpcProviderRegistry(bindingDOMRpcServiceAdapter, bindingDOMRpcProviderServiceAdapter);
    }

    @Override
    public void close() throws Exception {
        mappingCodecListenerReg.close();
    }

    public BindingToNormalizedNodeCodec getBindingToNormalizedNodeCodec() {
        return bindingToNormalizedNodeCodec;
    }

    public RpcProviderRegistry getRpcProviderRegistry() {
        return rpcProviderRegistry;
    }
}
