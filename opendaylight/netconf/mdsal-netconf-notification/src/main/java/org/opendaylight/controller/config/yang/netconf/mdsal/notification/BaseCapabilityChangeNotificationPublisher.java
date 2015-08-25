/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.netconf.mdsal.notification;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.netconf.notifications.BaseNotificationPublisherRegistration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Capabilities;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfCapabilityChange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfCapabilityChangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.changed.by.parms.ChangedByBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.changed.by.parms.changed.by.server.or.user.ServerBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class BaseCapabilityChangeNotificationPublisher implements DataChangeListener, AutoCloseable {

    private final BaseNotificationPublisherRegistration baseNotificationPublisherRegistration;

    public BaseCapabilityChangeNotificationPublisher(BaseNotificationPublisherRegistration baseNotificationPublisherRegistration) {
        this.baseNotificationPublisherRegistration = baseNotificationPublisherRegistration;
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        baseNotificationPublisherRegistration.onCapabilityChanged(computeCapabilitychange(change));
    }

    private NetconfCapabilityChange computeCapabilitychange(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        final NetconfCapabilityChangeBuilder netconfCapabilityChangeBuilder = new NetconfCapabilityChangeBuilder();
        netconfCapabilityChangeBuilder.setChangedBy(new ChangedByBuilder().setServerOrUser(new ServerBuilder().setServer(true).build()).build());

        if (!change.getCreatedData().isEmpty()) {
            final InstanceIdentifier capabilitiesIdentifier = InstanceIdentifier.create(NetconfState.class).child(Capabilities.class).builder().build();
            Preconditions.checkArgument(change.getCreatedData().get(capabilitiesIdentifier) instanceof Capabilities);
            netconfCapabilityChangeBuilder.setAddedCapability(((Capabilities) change.getCreatedData().get(capabilitiesIdentifier)).getCapability());
            netconfCapabilityChangeBuilder.setDeletedCapability(Collections.<Uri>emptyList());
        } else {
            Preconditions.checkArgument(change.getUpdatedSubtree() instanceof Capabilities);
            final Set<Uri> currentState = Sets.newHashSet(((Capabilities) change.getUpdatedSubtree()).getCapability());
            final Set<Uri> previousState = Sets.newHashSet(((Capabilities) change.getOriginalSubtree()).getCapability());

            netconfCapabilityChangeBuilder.setAddedCapability(Lists.newArrayList(Sets.difference(currentState, previousState)));
            netconfCapabilityChangeBuilder.setDeletedCapability(Lists.newArrayList(Sets.difference(previousState, currentState)));
        }

        // TODO modified should be computed ... but why ?
        netconfCapabilityChangeBuilder.setModifiedCapability(Collections.<Uri>emptyList());
        return netconfCapabilityChangeBuilder.build();
    }

    @Override
    public void close() throws Exception {

    }
}
