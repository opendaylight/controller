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

@Command(scope = "mdsal", name = "shard", description = "MDSAL Shard Information Command")
public class ShardInfo extends AbstractAction {

    private static final Logger LOG = LoggerFactory.getLogger(ShardInfo.class);

    @Option(name = "-n",
            aliases = { "--Name" },
            description = "Show the information of specific shard module",
            required = false,
            multiValued = false)
    private String shardModuleName = "";

    @Option(name = "-m",
            aliases = { "--Metrics" },
            description = "Show metrics information of the shard manager",
            required = false,
            multiValued = false)
    private boolean shardMetrics = false;

    @Override
    protected Object doExecute() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            if (!shardModuleName.isEmpty()) {
                ObjectName shardTopology = new ObjectName("org.opendaylight.controller:type=DistributedOperationalDatastore,Category=Shards,name=" + shardModuleName);
                Utils.printBeanTable(server, shardTopology, shardModuleName);
                return "";
            }
            if (shardMetrics) {
                ObjectName operationalShardMetrics = new ObjectName("org.opendaylight.controller.actor.metric:name=/user/shardmanager-operational.msg-rate");
                ObjectName configShardMetrics = new ObjectName("org.opendaylight.controller.actor.metric:name=/user/shardmanager-config.msg-rate");
                Utils.printBeanTable(server, configShardMetrics, "Config Shard Manager Message Rate Metrics");
                Utils.printBeanTable(server, operationalShardMetrics, "Operational Shard Manager Message Rate Metrics");
                return "";
            }
            ObjectName shardManagerOperational = new ObjectName("org.opendaylight.controller:type=DistributedOperationalDatastore,Category=ShardManager,name=shard-manager-operational");
            ObjectName shardManagerConfig = new ObjectName("org.opendaylight.controller:type=DistributedConfigDatastore,Category=ShardManager,name=shard-manager-config");
            Utils.printBeanTable(server, shardManagerConfig, "Shard manager of Config DataStore");
            Utils.printBeanTable(server, shardManagerOperational, "Shard manager of Operational DataStore");
            return "";
        } catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException | IntrospectionException e) {
            LOG.error("Mdsal Shard Info command ", e);
            return e.getMessage();
        }
    }
}
