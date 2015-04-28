/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.raft.election.ElectionStrategy;

public class FixedLeaderElectionStrategy implements ElectionStrategy {

    Map<String, Boolean> allowedLeaders = new HashMap<>();

    /**
     *
     * @param fixedLeaders
     *          inventory-config=member-1
     *          inventory-operational=member-2
     */
    FixedLeaderElectionStrategy(Dictionary<String, Object> fixedLeaders){

        Enumeration<String> keys = fixedLeaders.keys();

        while(keys.hasMoreElements()){
            String key = keys.nextElement();
            allowedLeaders.put(fixedLeaders.get(key).toString() + "-shard-" + key, true);
        }
    }

    @Override
    public boolean isAllowedToBecomeLeader(String id) {
        if(allowedLeaders.get(id) != null){
            return true;
        } else if(id.contains("member-1")){
            return true;
        }

        return false;
    }
}
