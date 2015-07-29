/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.rest.services.impl;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckForNull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.opendaylight.controller.rest.common.NormalizedNodeContext;
import org.opendaylight.controller.rest.connector.RestBrokerFacade;
import org.opendaylight.controller.rest.connector.RestSchemaController;
import org.opendaylight.controller.rest.services.RestconfServiceBase;
import org.opendaylight.controller.rest.services.RestconfServiceData;
import org.opendaylight.controller.rest.services.RestconfServiceOperations;
import org.opendaylight.controller.rest.services.RestconfStatisticsServiceWrapper;

import com.google.common.base.Preconditions;

/**
 * RestconfService wrapper class calculate and provided Request/Response statistics
 */
public class RestconfStatisticsServiceWrapperImpl implements RestconfStatisticsServiceWrapper {

    AtomicLong operationalGet = new AtomicLong();
    AtomicLong configGet = new AtomicLong();
    AtomicLong rpc = new AtomicLong();
    AtomicLong configPost = new AtomicLong();
    AtomicLong configPut = new AtomicLong();
    AtomicLong configDelete = new AtomicLong();
    AtomicLong successGetConfig = new AtomicLong();
    AtomicLong successGetOperational = new AtomicLong();
    AtomicLong successPost = new AtomicLong();
    AtomicLong successPut = new AtomicLong();
    AtomicLong successDelete = new AtomicLong();
    AtomicLong failureGetConfig = new AtomicLong();
    AtomicLong failureGetOperational = new AtomicLong();
    AtomicLong failurePost = new AtomicLong();
    AtomicLong failurePut = new AtomicLong();
    AtomicLong failureDelete = new AtomicLong();

    final RestconfServiceBase delegateRestServBase;
    final RestconfServiceData delegateRestServData;
    final RestconfServiceOperations delegateRestServOper;

    /**
     * StatisticsService constructor create all Restconf service instances.
     *
     * @param dataBroker
     * @param schemaCx
     */
    public RestconfStatisticsServiceWrapperImpl(@CheckForNull final RestBrokerFacade dataBroker,
            @CheckForNull final RestSchemaController schemaCx) {
        Preconditions.checkArgument(dataBroker != null);
        Preconditions.checkArgument(schemaCx != null);
        this.delegateRestServBase = new RestconfServiceBaseImpl(dataBroker, schemaCx);
        this.delegateRestServData = new RestconfServiceDataImpl(dataBroker, schemaCx);
        this.delegateRestServOper = new RestconfServiceOperationsImpl(dataBroker, schemaCx);
    }

    @Override
    public Object getRoot() {
        return delegateRestServBase.getRoot();
    }

    @Override
    public NormalizedNodeContext getModules(final UriInfo uriInfo) {
        return delegateRestServBase.getModules(uriInfo);
    }

    @Override
    public NormalizedNodeContext getModules(final String identifier, final UriInfo uriInfo) {
        return delegateRestServBase.getModules(identifier, uriInfo);
    }

    @Override
    public NormalizedNodeContext getModule(final String identifier, final UriInfo uriInfo) {
        return delegateRestServBase.getModule(identifier, uriInfo);
    }

    @Override
    public NormalizedNodeContext getOperations(final UriInfo uriInfo) {
        return delegateRestServOper.getOperations(uriInfo);
    }

    @Override
    public NormalizedNodeContext getOperations(final String identifier, final UriInfo uriInfo) {
        return delegateRestServOper.getOperations(identifier, uriInfo);
    }

    @Override
    public NormalizedNodeContext invokeRpc(final String identifier, final NormalizedNodeContext payload,
            final UriInfo uriInfo) {
        rpc.incrementAndGet();
        return delegateRestServOper.invokeRpc(identifier, payload, uriInfo);
    }

    @Override
    public NormalizedNodeContext invokeRpc(final String identifier, final String noPayload, final UriInfo uriInfo) {
        rpc.incrementAndGet();
        return delegateRestServOper.invokeRpc(identifier, noPayload, uriInfo);
    }

    @Override
    public NormalizedNodeContext readConfigurationData(final String identifier, final UriInfo uriInfo) {
        configGet.incrementAndGet();
        NormalizedNodeContext normalizedNodeContext = null;
        try {
            normalizedNodeContext = delegateRestServData.readConfigurationData(identifier, uriInfo);
            if (normalizedNodeContext.getData() != null) {
                successGetConfig.incrementAndGet();
            } else {
                failureGetConfig.incrementAndGet();
            }
        } catch (final Exception e) {
            failureGetConfig.incrementAndGet();
            throw e;
        }
        return normalizedNodeContext;
    }

    @Override
    public NormalizedNodeContext readOperationalData(final String identifier, final UriInfo uriInfo) {
        operationalGet.incrementAndGet();
        NormalizedNodeContext normalizedNodeContext = null;
        try {
            normalizedNodeContext = delegateRestServData.readOperationalData(identifier, uriInfo);
            if (normalizedNodeContext.getData() != null) {
                successGetOperational.incrementAndGet();
            } else {
                failureGetOperational.incrementAndGet();
            }
        } catch (final Exception e) {
            failureGetOperational.incrementAndGet();
            throw e;
        }
        return normalizedNodeContext;
    }

    @Override
    public Response updateConfigurationData(final String identifier, final NormalizedNodeContext payload) {
        configPut.incrementAndGet();
        Response response = null;
        try {
            response = delegateRestServData.updateConfigurationData(identifier, payload);
            if (response.getStatus() == Status.OK.getStatusCode()) {
                successPut.incrementAndGet();
            } else {
                failurePut.incrementAndGet();
            }
        } catch (final Exception e) {
            failurePut.incrementAndGet();
            throw e;
        }
        return response;
    }

    @Override
    public Response patchConfigurationData(final String identifier, final NormalizedNodeContext payload) {
        // FIXME : patch means update so add code to increase AtomicUpdateIndex
        return delegateRestServData.patchConfigurationData(identifier, payload);
    }

    @Override
    public Response createConfigurationData(final String identifier, final NormalizedNodeContext payload,
            final UriInfo uriInfo) {
        configPost.incrementAndGet();
        Response response = null;
        try {
            response = delegateRestServData.createConfigurationData(identifier, payload, uriInfo);
            if (response.getStatus() == Status.OK.getStatusCode()) {
                successPost.incrementAndGet();
            } else {
                failurePost.incrementAndGet();
            }
        } catch (final Exception e) {
            failurePost.incrementAndGet();
            throw e;
        }
        return response;
    }

    @Override
    public Response createConfigurationData(final NormalizedNodeContext payload, final UriInfo uriInfo) {
        configPost.incrementAndGet();
        Response response = null;
        try {
            response = delegateRestServData.createConfigurationData(payload, uriInfo);
            if (response.getStatus() == Status.OK.getStatusCode()) {
                successPost.incrementAndGet();
            } else {
                failurePost.incrementAndGet();
            }
        } catch (final Exception e) {
            failurePost.incrementAndGet();
            throw e;
        }
        return response;
    }

    @Override
    public Response deleteConfigurationData(final String identifier) {
        configDelete.incrementAndGet();
        Response response = null;
        try {
            response = delegateRestServData.deleteConfigurationData(identifier);
            if (response.getStatus() == Status.OK.getStatusCode()) {
                successDelete.incrementAndGet();
            } else {
                failureDelete.incrementAndGet();
            }
        } catch (final Exception e) {
            failureDelete.incrementAndGet();
            throw e;
        }
        return response;
    }

    @Override
    public Response subscribeToStream(final String identifier, final UriInfo uriInfo) {
        return delegateRestServBase.subscribeToStream(identifier, uriInfo);
    }

    @Override
    public NormalizedNodeContext getAvailableStreams(final UriInfo uriInfo) {
        return delegateRestServBase.getAvailableStreams(uriInfo);
    }

    @Override
    public BigInteger getConfigDelete() {
        return BigInteger.valueOf(configDelete.get());
    }

    @Override
    public BigInteger getConfigGet() {
        return BigInteger.valueOf(configGet.get());
    }

    @Override
    public BigInteger getConfigPost() {
        return BigInteger.valueOf(configPost.get());
    }

    @Override
    public BigInteger getConfigPut() {
        return BigInteger.valueOf(configPut.get());
    }

    @Override
    public BigInteger getOperationalGet() {
        return BigInteger.valueOf(operationalGet.get());
    }

    @Override
    public BigInteger getRpc() {
        return BigInteger.valueOf(rpc.get());
    }

    @Override
    public BigInteger getSuccessGetConfig() {
        return BigInteger.valueOf(successGetConfig.get());
    }

    @Override
    public BigInteger getSuccessGetOperational() {
        return BigInteger.valueOf(successGetOperational.get());
    }

    @Override
    public BigInteger getSuccessPost() {
        return BigInteger.valueOf(successPost.get());
    }

    @Override
    public BigInteger getSuccessPut() {
        return BigInteger.valueOf(successPut.get());
    }

    @Override
    public BigInteger getSuccessDelete() {
        return BigInteger.valueOf(successDelete.get());
    }

    @Override
    public BigInteger getFailureGetConfig() {
        return BigInteger.valueOf(failureGetConfig.get());
    }

    @Override
    public BigInteger getFailureGetOperational() {
        return BigInteger.valueOf(failureGetOperational.get());
    }

    @Override
    public BigInteger getFailurePost() {
        return BigInteger.valueOf(failurePost.get());
    }

    @Override
    public BigInteger getFailurePut() {
        return BigInteger.valueOf(failurePut.get());
    }

    @Override
    public BigInteger getFailureDelete() {
        return BigInteger.valueOf(failureDelete.get());
    }
}