/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.DataStoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.GetShardRoleInputBuilder;

@Service
@Command(scope = "cluster-admin", name = "get-shard-role", description = "Run a get-shard-role test")
public class GetShardRoleCommand implements Action {

    @Reference
    private ClusterAdminService clusterAdminService;

    @Argument(index = 0, name = "shard-name", required = true)
    String shardName;

    @Argument(index = 1, name = "data-store-type", required = true)
    DataStoreType dataStoreType;

    @Override
    public Object execute() throws Exception {
        return clusterAdminService
                .getShardRole(new GetShardRoleInputBuilder()
                    .setShardName(shardName)
                    .setDataStoreType(dataStoreType)
                    .build())
                .get()
                .getResult();
    }
}
