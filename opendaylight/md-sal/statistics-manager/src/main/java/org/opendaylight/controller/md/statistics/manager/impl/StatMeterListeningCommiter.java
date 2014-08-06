/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 29, 2014
 */
public class StatMeterListeningCommiter extends StatAbstractListeningCommiter<Meter>{

    public StatMeterListeningCommiter(final StatisticsManager manager, final DataBroker db) {
        super(manager, db, Meter.class);
    }

    @Override
    protected InstanceIdentifier<Meter> getWildCardedRegistrationPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class)
                .augmentation(FlowCapableNode.class).child(Meter.class);
    }

    @Override
    public void createStat(final InstanceIdentifier<Meter> keyIdent, final Meter data) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeStat(final InstanceIdentifier<Meter> keyIdent) {
        final WriteTransaction trans = manager.getWriteTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, keyIdent);
        trans.submit();
    }
}

