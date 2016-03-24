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

@Command(scope = "mdsal", name = "store", description = "MDSAL Store Information Command")
public class StoresInfo extends AbstractAction {

    private static final Logger LOG = LoggerFactory.getLogger(StoresInfo.class);

    @Override
    protected Object doExecute() {
        try {
            ObjectName DistributedOperDatastore = new ObjectName("org.opendaylight.controller:type=DistributedOperationalDatastore,Category=Configuration,name=Datastore");
            ObjectName DistributedConfigDatastore = new ObjectName("org.opendaylight.controller:type=DistributedConfigDatastore,Category=Configuration,name=Datastore");
            ObjectName DistributedConfigGeneralRuntimeInfo = new ObjectName("org.opendaylight.controller:type=DistributedConfigDatastore,name=GeneralRuntimeInfo");
            ObjectName DistributedOperGeneralRuntimeInfo = new ObjectName("org.opendaylight.controller:type=DistributedOperationalDatastore,name=GeneralRuntimeInfo");
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            Utils.printBeanTable(server, DistributedConfigDatastore, "Distributed Config Datastore");
            System.out.println("Config DataStore Transaction Creation Rate Limit " + server.getAttribute(DistributedConfigGeneralRuntimeInfo, "TransactionCreationRateLimit"));
            System.out.println("");
            Utils.printBeanTable(server, DistributedOperDatastore, "Distributed Operational Datastore");
            System.out.println("Operational DataStore Transaction Creation Rate Limit " + server.getAttribute(DistributedOperGeneralRuntimeInfo, "TransactionCreationRateLimit"));
            return "";
        } catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException | IntrospectionException e) {
            LOG.error("Mdsal Store Info command ", e);
            return e.getMessage();
        }
    }
}
