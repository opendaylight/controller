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

public class StatisticsRestconfServiceWrapper implements RestconfService {

    AtomicLong operationalGet = new AtomicLong();
    AtomicLong configGet = new AtomicLong();
    AtomicLong rpc = new AtomicLong();
    AtomicLong configPost = new AtomicLong();
    AtomicLong configPut = new AtomicLong();
    AtomicLong configDelete = new AtomicLong();

    private static final StatisticsRestconfServiceWrapper INSTANCE = new StatisticsRestconfServiceWrapper(RestconfImpl.getInstance());

    final RestconfService delegate;

    private StatisticsRestconfServiceWrapper(final RestconfService delegate) {
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
    public NormalizedNodeContext getModules(final UriInfo uriInfo) {
        return delegate.getModules(uriInfo);
    }

    @Override
    public NormalizedNodeContext getModules(final String identifier, final UriInfo uriInfo) {
        return delegate.getModules(identifier, uriInfo);
    }

    @Override
    public NormalizedNodeContext getModule(final String identifier, final UriInfo uriInfo) {
        return delegate.getModule(identifier, uriInfo);
    }

    @Override
    public NormalizedNodeContext getOperations(final UriInfo uriInfo) {
        return delegate.getOperations(uriInfo);
    }

    @Override
    public NormalizedNodeContext getOperations(final String identifier, final UriInfo uriInfo) {
        return delegate.getOperations(identifier, uriInfo);
    }

    @Override
    public NormalizedNodeContext invokeRpc(final String identifier, final NormalizedNodeContext payload, final UriInfo uriInfo) {
        rpc.incrementAndGet();
        return delegate.invokeRpc(identifier, payload, uriInfo);
    }

    @Override
    public StructuredData invokeRpc(final String identifier, final String noPayload, final UriInfo uriInfo) {
        rpc.incrementAndGet();
        return delegate.invokeRpc(identifier, noPayload, uriInfo);
    }

    @Override
    public NormalizedNodeContext readConfigurationData(final String identifier, final UriInfo uriInfo) {
        configGet.incrementAndGet();
        return delegate.readConfigurationData(identifier, uriInfo);
    }

    @Override
    public NormalizedNodeContext readOperationalData(final String identifier, final UriInfo uriInfo) {
        operationalGet.incrementAndGet();
        return delegate.readOperationalData(identifier, uriInfo);
    }

    @Override
    public Response updateConfigurationData(final String identifier, final NormalizedNodeContext payload) {
        configPut.incrementAndGet();
        return delegate.updateConfigurationData(identifier, payload);
    }

    @Override
    public Response createConfigurationData(final String identifier, final NormalizedNodeContext payload, final UriInfo uriInfo) {
        configPost.incrementAndGet();
        return delegate.createConfigurationData(identifier, payload, uriInfo);
    }

    @Override
    public Response createConfigurationData(final NormalizedNodeContext payload, final UriInfo uriInfo) {
        configPost.incrementAndGet();
        return delegate.createConfigurationData(payload, uriInfo);
    }

    @Override
    public Response deleteConfigurationData(final String identifier) {
        return delegate.deleteConfigurationData(identifier);
    }

    @Override
    public Response subscribeToStream(final String identifier, final UriInfo uriInfo) {
        return delegate.subscribeToStream(identifier, uriInfo);
    }

    @Override
    public NormalizedNodeContext getAvailableStreams(final UriInfo uriInfo) {
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
