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

@Command(scope = "mdsal", name = "shardInfo", description = "MDSAL Shard Information Command")
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

    @Option(name = "-config",
            aliases = { "--Configuration" },
            description = "Show the configuration data store",
            required = false,
            multiValued = false)
    private boolean configurationList = false;

    @Option(name = "-operational",
            aliases = { "--Metrics" },
            description = "Show the operational data store",
            required = false,
            multiValued = false)
    private boolean operationalList = false;

    @Override
    protected Object doExecute() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName shardManagerOperational = new ObjectName("org.opendaylight.controller:type=DistributedOperationalDatastore,Category=ShardManager,name=shard-manager-operational");
            ObjectName shardManagerConfig = new ObjectName("org.opendaylight.controller:type=DistributedConfigDatastore,Category=ShardManager,name=shard-manager-config");
            ObjectName operationalShardMetrics = new ObjectName("org.opendaylight.controller.actor.metric:name=/user/shardmanager-operational.msg-rate");
            ObjectName configShardMetrics = new ObjectName("org.opendaylight.controller.actor.metric:name=/user/shardmanager-config.msg-rate");
            if (!shardModuleName.isEmpty()) {
                ObjectName shardTopology = new ObjectName("org.opendaylight.controller:type=DistributedOperationalDatastore,Category=Shards,name=" + shardModuleName);
                Utils.printBeanTable(server, shardTopology, shardModuleName);
                return "";
            }
            if (shardMetrics && configurationList) {
                Utils.printBeanTable(server, configShardMetrics, "Config Shard Manager Metrics");
                return "";
            }
            if (shardMetrics && operationalList) {
                Utils.printBeanTable(server, operationalShardMetrics, "Operational Shard Manager Metrics");
                return "";
            }
            if (shardMetrics) {
                Utils.printBeanTable(server, configShardMetrics, "Config Shard Manager Metrics");
                Utils.printBeanTable(server, operationalShardMetrics, "Operational Shard Manager Metrics");
                return "";
            }
            if (configurationList) {
                Utils.printBeanTable(server, shardManagerConfig, "Config DataStore Shard manager");
                return "";
            }
            if (operationalList) {
                Utils.printBeanTable(server, shardManagerOperational, "Operational DataStore Shard manager");
                return "";
            }
            Utils.printBeanTable(server, shardManagerConfig, "Config DataStore Shard manager");
            Utils.printBeanTable(server, shardManagerOperational, "Operational DataStore Shard manager");
            return "";
        } catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException | IntrospectionException e) {
            LOG.error("Mdsal Shard Info command", e);
            return "";
        }
    }
}
