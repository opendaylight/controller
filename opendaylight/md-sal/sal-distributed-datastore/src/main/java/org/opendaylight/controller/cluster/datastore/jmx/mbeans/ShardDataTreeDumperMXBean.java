/*
 * Copyright (c) 2019 Ericsson Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.jmx.mbeans;

/**
 * MXBean interface for dumping shard data tree content to a file.
 *
 * @author etanvvi
 */
public interface ShardDataTreeDumperMXBean {

    void getShardDataTreeDump(String filename);
}
