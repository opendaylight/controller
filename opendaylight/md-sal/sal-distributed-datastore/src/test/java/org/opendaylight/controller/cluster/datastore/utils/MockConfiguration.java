/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import org.opendaylight.controller.cluster.datastore.Configuration;

import java.util.ArrayList;
import java.util.List;

public class MockConfiguration implements Configuration{
    @Override public List<String> getMemberShardNames(String memberName) {
        List<String> shardNames = new ArrayList<>();
        shardNames.add("default");
        return shardNames;
    }
}
