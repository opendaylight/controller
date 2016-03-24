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

@Command(scope = "mdsal", name = "status", description = "MDSAL Status Command")
public class StatusInfo extends AbstractAction {

    private static final Logger LOG = LoggerFactory.getLogger(StatusInfo.class);

    @Override
    protected Object doExecute() {
        try {
            ObjectName configRegistry =  new ObjectName("org.opendaylight.controller:type=ConfigRegistry");
            ObjectName commitExecutorStats = new ObjectName("org.opendaylight.controller:type=DOMDataBroker,name=CommitExecutorStats");
            ObjectName commitFutureExecutorStats = new ObjectName("org.opendaylight.controller:type=DOMDataBroker,name=CommitFutureExecutorStats");
            ObjectName commitStats = new ObjectName("org.opendaylight.controller:type=DOMDataBroker,name=CommitStats");
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            System.out.println("ConfigRegistry Version: " + server.getAttribute(configRegistry, "Version") + " Healthy: " + server.getAttribute(configRegistry, "Healthy"));
            System.out.println("------------------------------------");
            Utils.printBeanTable(server, commitExecutorStats, "DOM DataBroker Commit Executor Stats");
            Utils.printBeanTable(server, commitFutureExecutorStats, "DOM DataBroker Commit Future Executor Stats");
            Utils.printBeanTable(server, commitStats, "DOM DataBroker Commit Stats");
            return "";
        } catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException | IntrospectionException e) {
            LOG.error("Mdsal status command ", e);
            return e.getMessage();
        }
    }

}
