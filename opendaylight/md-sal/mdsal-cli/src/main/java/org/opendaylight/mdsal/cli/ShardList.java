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
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "mdsal", name = "shardList", description = "Available Shard List Command")
public class ShardList extends AbstractAction {

    private static final Logger LOG = LoggerFactory.getLogger(ShardList.class);

    @Option(name = "-config",
            aliases = { "--Configuration" },
            description = "Show the configuration data store shard list",
            required = false,
            multiValued = false)
    private boolean configurationList = false;

    @Option(name = "-operational",
            aliases = { "--Metrics" },
            description = "Show the operational data store shard list",
            required = false,
            multiValued = false)
    private boolean operationalList = false;

    @Override
    protected Object doExecute() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName shardManagerOperational = new ObjectName("org.opendaylight.controller:type=DistributedOperationalDatastore,Category=ShardManager,name=shard-manager-operational");
            ObjectName shardManagerConfig = new ObjectName("org.opendaylight.controller:type=DistributedConfigDatastore,Category=ShardManager,name=shard-manager-config");
            if (configurationList) {
                Utils.printBeanAttribute(server, shardManagerConfig, "Configuration DataStore Shard", "LocalShards", "shard-");
                return "";
            }
            if (operationalList) {
                Utils.printBeanAttribute(server, shardManagerOperational, "Operational DataStore Shard", "LocalShards", "shard-");
                return "";
            }
            Utils.printBeanAttribute(server, shardManagerConfig, "Configuration DataStore Shard", "LocalShards", "shard-");
            Utils.printBeanAttribute(server, shardManagerOperational, "Operational DataStore Shard", "LocalShards", "shard-");
            return "";
        } catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException | IntrospectionException e) {
            LOG.error("Mdsal Shard List command", e);
            return "";
        }
    }
}
