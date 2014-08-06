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
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class StatGroupListeningCommiter extends StatAbstractListeningCommiter<Group>{

    private static final Logger LOG = LoggerFactory.getLogger(StatGroupListeningCommiter.class);

    public StatGroupListeningCommiter(final StatisticsManager manager, final DataBroker db) {
        super(manager, db, Group.class);
    }

    @Override
    protected InstanceIdentifier<Group> getWildCardedRegistrationPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class)
                .augmentation(FlowCapableNode.class).child(Group.class);
    }

    @Override
    public void createStat(final InstanceIdentifier<Group> keyIdent, final Group data) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeStat(final InstanceIdentifier<Group> keyIdent) {
        final WriteTransaction trans = manager.getWriteTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, keyIdent);
        trans.submit();
    }
}

