/*
 * Copyright (c) 2016 Inocybe Technologies.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.mdsal.cli;

import java.lang.management.ManagementFactory;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "cluster", name = "info", description = "Cluster Information Command")
public class ClusterInfo extends AbstractAction {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterInfo.class);

    @Override
    protected Object doExecute() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName shardManagerOperational = new ObjectName("org.opendaylight.controller:type=DistributedOperationalDatastore,Category=ShardManager,name=shard-manager-operational");
            Utils.printClusterNodes(server, shardManagerOperational);
            return "";
        } catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException | IntrospectionException e) {
            LOG.error("Mdsal Shard Info command ", e);
            return "";
        }
    }
}
