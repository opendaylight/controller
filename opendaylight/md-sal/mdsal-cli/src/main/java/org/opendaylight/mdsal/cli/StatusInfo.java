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

@Command(scope = "mdsal", name = "brokerState", description = "MDSAL brokerState Command")
public class StatusInfo extends AbstractAction {

    private static final Logger LOG = LoggerFactory.getLogger(StatusInfo.class);

    @Option(name = "-all",
            aliases = { "--all" },
            description = "Show all the databroker commit state",
            required = false,
            multiValued = false)
    private boolean all = false;

    @Override
    protected Object doExecute() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            final ObjectName commitStats = new ObjectName("org.opendaylight.controller:type=DOMDataBroker,name=CommitStats");
            if (all) {
                final ObjectName configRegistry =  new ObjectName("org.opendaylight.controller:type=ConfigRegistry");
                final ObjectName commitExecutorStats = new ObjectName("org.opendaylight.controller:type=DOMDataBroker,name=CommitExecutorStats");
                final ObjectName commitFutureExecutorStats = new ObjectName("org.opendaylight.controller:type=DOMDataBroker,name=CommitFutureExecutorStats");
                System.out.println("ConfigRegistry Version: " + server.getAttribute(configRegistry, "Version") + " Healthy: " + server.getAttribute(configRegistry, "Healthy"));
                System.out.println("------------------------------------");
                Utils.printBeanTable(server, commitExecutorStats, "DOM DataBroker Commit Executor Stats");
                Utils.printBeanTable(server, commitFutureExecutorStats, "DOM DataBroker Future Commit Executor Stats");
            }
            Utils.printBeanTable(server, commitStats, "DOM DataBroker Commit Stats");
            return "";
        } catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException | IntrospectionException e) {
            LOG.error("Mdsal brokerState command ", e);
            return "";
        }
    }

}
