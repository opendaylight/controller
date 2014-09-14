/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager
 *
 * StatListeningCommiter
 * Definition Interface for {@link DataChangeListener} implementer class rule.
 * Interface represent a contract between Config/DataStore changes and
 * Operational/DataStore commits. All Operational/DataStore commit have
 * to by represent as RPC Device response Notification processing. So
 * Operational/DS could contains only real mirror of OF Device
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 27, 2014
 */
public interface StatListeningCommiter<T extends DataObject, N extends NotificationListener> extends DataChangeListener, StatNotifyCommiter<N> {

    /**
     * Method represents needed processing for new Listening {@link DataObject} in Config/DS
     * Primary -> ask Device for element's statistic (they should contain actual property
     * for whole element)
     *
     * @param InstanceIdentifier keyIdent
     * @param data
     * @param InstanceIdentifier<Node> nodeIdent
     */
    void createStat(InstanceIdentifier<T> keyIdent, T data, InstanceIdentifier<Node> nodeIdent);

    /**
     * Method represents needed processing for removed Listening {@link DataObject} in Config/DS
     * Primary -> remove path from Operational/DS identified by input {@link InstanceIdentifier}
     *
     * @param InstanceIdentifier keyIdent
     */
    void removeStat(InstanceIdentifier<T> keyIdent);
}

