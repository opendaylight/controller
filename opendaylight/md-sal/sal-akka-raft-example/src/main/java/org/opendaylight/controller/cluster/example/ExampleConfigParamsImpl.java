/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.example;

import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;

/**
 * Implementation of ConfigParams for Example
 */
public class ExampleConfigParamsImpl extends DefaultConfigParamsImpl {
    @Override
    public long getSnapshotBatchCount() {
        return 25;
    }

    @Override
    public int getSnapshotChunkSize() {
        return 50;
    }
}
