/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.netconf.mdsal.monitoring;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MonitoringToMdsalWriter implements AutoCloseable, NetconfMonitoringService.MonitoringListener, BindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MonitoringToMdsalWriter.class);

    private final NetconfMonitoringService serverMonitoringDependency;
    private DataBroker dataBroker;

    public MonitoringToMdsalWriter(final NetconfMonitoringService serverMonitoringDependency) {
        this.serverMonitoringDependency = serverMonitoringDependency;
    }

    @Override
    public void close() {
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetconfState.class));
        final CheckedFuture<Void, TransactionCommitFailedException> submit = tx.submit();

        Futures.addCallback(submit, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void aVoid) {
                LOG.debug("Netconf state cleared successfully");
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Unable to clear netconf state", throwable);
            }
        });
    }

    @Override
    public void onStateChanged(final NetconfState state) {
        // FIXME first attempt (right after we register to binding broker) always fails
        Preconditions.checkState(dataBroker != null);
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(NetconfState.class), state);
        final CheckedFuture<Void, TransactionCommitFailedException> submit = tx.submit();

        Futures.addCallback(submit, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void aVoid) {
                LOG.debug("Netconf state updated successfully");
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Unable to update netconf state", throwable);
            }
        });
    }

    @Override
    public void onSessionInitiated(final BindingAwareBroker.ProviderContext providerContext) {
        dataBroker = providerContext.getSALService(DataBroker.class);
        serverMonitoringDependency.registerListener(this);
    }
}
