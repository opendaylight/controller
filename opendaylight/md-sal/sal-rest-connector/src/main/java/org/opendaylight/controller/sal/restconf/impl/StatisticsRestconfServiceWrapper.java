/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;

public class StatisticsRestconfServiceWrapper implements RestconfService {

    AtomicLong operationalGet = new AtomicLong();
    AtomicLong configGet = new AtomicLong();
    AtomicLong rpc = new AtomicLong();
    AtomicLong configPost = new AtomicLong();
    AtomicLong configPut = new AtomicLong();
    AtomicLong configDelete = new AtomicLong();

    private static final StatisticsRestconfServiceWrapper INSTANCE = new StatisticsRestconfServiceWrapper(RestconfImpl.getInstance());

    final RestconfService delegate;

    private StatisticsRestconfServiceWrapper(RestconfService delegate) {
        this.delegate = delegate;
    }

    public static StatisticsRestconfServiceWrapper getInstance() {
        return INSTANCE;
    }

    @Override
    public Object getRoot() {
        return delegate.getRoot();
    }

    @Override
    public StructuredData getModules(UriInfo uriInfo) {
        return delegate.getModules(uriInfo);
    }

    @Override
    public StructuredData getModules(String identifier, UriInfo uriInfo) {
        return delegate.getModules(identifier, uriInfo);
    }

    @Override
    public StructuredData getModule(String identifier, UriInfo uriInfo) {
        return delegate.getModule(identifier, uriInfo);
    }

    @Override
    public StructuredData getOperations(UriInfo uriInfo) {
        return delegate.getOperations(uriInfo);
    }

    @Override
    public StructuredData getOperations(String identifier, UriInfo uriInfo) {
        return delegate.getOperations(identifier, uriInfo);
    }

    @Override
    public StructuredData invokeRpc(String identifier, CompositeNode payload, UriInfo uriInfo) {
        rpc.incrementAndGet();
        return delegate.invokeRpc(identifier, payload, uriInfo);
    }

    @Override
    public StructuredData invokeRpc(String identifier, String noPayload, UriInfo uriInfo) {
        rpc.incrementAndGet();
        return delegate.invokeRpc(identifier, noPayload, uriInfo);
    }

    @Override
    public NormalizedNodeContext readConfigurationData(String identifier, UriInfo uriInfo) {
        configGet.incrementAndGet();
        return delegate.readConfigurationData(identifier, uriInfo);
    }

    @Override
    public NormalizedNodeContext readOperationalData(String identifier, UriInfo uriInfo) {
        operationalGet.incrementAndGet();
        return delegate.readOperationalData(identifier, uriInfo);
    }

    @Override
    public Response updateConfigurationData(String identifier, Node<?> payload) {
        configPut.incrementAndGet();
        return delegate.updateConfigurationData(identifier, payload);
    }

    @Override
    public Response createConfigurationData(String identifier, Node<?> payload, UriInfo uriInfo) {
        configPost.incrementAndGet();
        return delegate.createConfigurationData(identifier, payload, uriInfo);
    }

    @Override
    public Response createConfigurationData(Node<?> payload, UriInfo uriInfo) {
        configPost.incrementAndGet();
        return delegate.createConfigurationData(payload, uriInfo);
    }

    @Override
    public Response deleteConfigurationData(String identifier) {
        return delegate.deleteConfigurationData(identifier);
    }

    @Override
    public Response subscribeToStream(String identifier, UriInfo uriInfo) {
        return delegate.subscribeToStream(identifier, uriInfo);
    }

    @Override
    public StructuredData getAvailableStreams(UriInfo uriInfo) {
        return delegate.getAvailableStreams(uriInfo);
    }

    public BigInteger getConfigDelete() {
        return BigInteger.valueOf(configDelete.get());
    }

    public BigInteger getConfigGet() {
        return BigInteger.valueOf(configGet.get());
    }

    public BigInteger getConfigPost() {
        return BigInteger.valueOf(configPost.get());
    }

    public BigInteger getConfigPut() {
        return BigInteger.valueOf(configPut.get());
    }

    public BigInteger getOperationalGet() {
        return BigInteger.valueOf(operationalGet.get());
    }

    public BigInteger getRpc() {
        return BigInteger.valueOf(rpc.get());
    }

}
