
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.topologymanager;

import org.opendaylight.controller.sal.topology.IListenTopoUpdates;

/**
 * Interface for all the listener of topology updates created by
 * Topology Manager, effectively speaking the updates are an extension
 * of the IListenTopoUpdates coming from SAL.
 */
public interface ITopologyManagerAware extends IListenTopoUpdates {
}
