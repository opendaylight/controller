/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.netconf.mdsal.notification;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.netconf.notifications.NetconfNotificationCollector;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.Netconf;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.Streams;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.Stream;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.StreamKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NotificationToMdsalWriter implements AutoCloseable, NetconfNotificationCollector.NetconfNotificationStreamListener, BindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationToMdsalWriter.class);

    private final NetconfNotificationCollector netconfNotificationCollector;
    private DataBroker dataBroker;

    public NotificationToMdsalWriter(NetconfNotificationCollector netconfNotificationCollector) {
        this.netconfNotificationCollector = netconfNotificationCollector;
    }

    @Override
    public void close() {
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Netconf.class));
        final CheckedFuture<Void, TransactionCommitFailedException> submit = tx.submit();

        Futures.addCallback(submit, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                LOG.debug("Streams cleared successfully");
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.warn("Unable to clear streams", throwable);
            }
        });
    }

    @Override
    public void onSessionInitiated(BindingAwareBroker.ProviderContext session) {
        dataBroker = session.getSALService(DataBroker.class);
        netconfNotificationCollector.registerStreamListener(this);
    }

    @Override
    public void onStreamRegistered(Stream stream) {
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();

        final InstanceIdentifier streamIdentifier = InstanceIdentifier.create(Netconf.class).child(Streams.class).builder().child(Stream.class, stream.getKey()).build();
        tx.merge(LogicalDatastoreType.OPERATIONAL, streamIdentifier, stream, true);

        try {
            tx.submit().checkedGet();
            LOG.debug("Stream %s registered successfully.", stream.getName());
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Unable to register stream.", e);
        }
    }

    @Override
    public void onStreamUnregistered(StreamNameType stream) {
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();

        final StreamKey streamKey = new StreamKey(stream);
        final InstanceIdentifier streamIdentifier = InstanceIdentifier.create(Netconf.class).child(Streams.class).builder().child(Stream.class, streamKey).build();

        tx.delete(LogicalDatastoreType.OPERATIONAL, streamIdentifier);

        try {
            tx.submit().checkedGet();
            LOG.debug("Stream %s unregistered successfully.", stream);
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Unable to unregister stream", e);
        }
    }
}
