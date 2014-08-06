/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager
 *
 * StatListeningCommiter
 * Definition Interface for DataChangeListener implementer class rule
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 27, 2014
 */
public interface StatListeningCommiter<T extends DataObject> extends AutoCloseable, DataChangeListener {

    void createStat(InstanceIdentifier<T> keyIdent, T data, InstanceIdentifier<FlowCapableNode> nodeIdent);

    void removeStat(InstanceIdentifier<T> keyIdent);
}

