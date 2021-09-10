/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.DeactivateEosDatacenterInputBuilder;

@Service
@Command(scope = "cluster-admin", name = "deactivate-eos-datacenter",
        description = "Run a deactivate-eos-datacenter test")
public class DeactivateEosDatacenterCommand implements Action {

    @Reference
    private ClusterAdminService clusterAdminService;

    @Override
    public Object execute() throws Exception {
        return clusterAdminService
                .deactivateEosDatacenter(new DeactivateEosDatacenterInputBuilder().build())
                .get()
                .getResult();
    }
}
