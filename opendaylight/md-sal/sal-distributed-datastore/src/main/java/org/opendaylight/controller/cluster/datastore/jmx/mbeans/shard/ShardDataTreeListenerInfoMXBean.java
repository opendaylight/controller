/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import java.util.List;
import org.opendaylight.controller.cluster.mgmt.api.DataTreeListenerInfo;

/**
 * MXBean interface for reporting shard data tree change listener information.
 *
 * @author Thomas Pantelis
 */
public interface ShardDataTreeListenerInfoMXBean {
    List<DataTreeListenerInfo> getDataTreeChangeListenerInfo();
}
