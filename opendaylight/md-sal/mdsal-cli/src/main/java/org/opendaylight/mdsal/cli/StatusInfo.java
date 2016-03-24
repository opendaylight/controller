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
            ObjectName inMemoryConfigDatastore = new ObjectName("org.opendaylight.controller:type=InMemoryConfigDataStore,name=notification-executor");
            ObjectName inMemoryOperationalDatastore = new ObjectName("org.opendaylight.controller:type=InMemoryOperationalDataStore,name=notification-executor");
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            Long version = (Long) server.getAttribute(configRegistry, "Version");
            boolean health = (Boolean) server.getAttribute(configRegistry, "Healthy");
            String info = "        ::MDSAL Status::"; 
            info += "\n ====================================";
            info += "\n ConfigRegistry Version: " + version + " Healthy: " + health;
            info += "\n ------------------------------------";
            info += printObjectData(server, commitExecutorStats, " Commit Executor Stats");
            info += printObjectData(server, commitFutureExecutorStats, " Commit Future Executor Stats");
            info += printObjectData(server, inMemoryConfigDatastore, " InMemory Config DataStore Stats");
            info += printObjectData(server, inMemoryOperationalDatastore, " InMemory Operational DataStore Stats");
            return info;
        } catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e) {
            LOG.error("Mdsal status command ", e);
            return e.getMessage();
        }
    }

    private String printObjectData(MBeanServer server, ObjectName obj, String header) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        Long activeThreadCount     = (Long) server.getAttribute(obj, "ActiveThreadCount");
        Long completedTaskCount    = (Long) server.getAttribute(obj, "CompletedTaskCount");
        Long currentQueueSize      = (Long) server.getAttribute(obj, "CurrentQueueSize");
        Long currentThreadPoolSize = (Long) server.getAttribute(obj, "CurrentThreadPoolSize");
        Long largestQueueSize      = (Long) server.getAttribute(obj, "LargestQueueSize");
        Long largestThreadPoolSize = (Long) server.getAttribute(obj, "LargestThreadPoolSize");
        Long maxQueueSize          = (Long) server.getAttribute(obj, "MaxQueueSize");
        Long maxThreadPoolSize     = (Long) server.getAttribute(obj, "MaxThreadPoolSize");
        Long rejectedTaskCount     = (Long) server.getAttribute(obj, "RejectedTaskCount");
        Long totalTaskCount        = (Long) server.getAttribute(obj, "TotalTaskCount");
        String data = "\n" + header + "\n ====================================";
        data +=" \n Active Thread Count is : " + activeThreadCount;
        data += "\n Completed Task Count is : " + completedTaskCount;
        data += "\n Current Queue Size is : " + currentQueueSize;
        data += "\n Current ThreadPool Size is : " + currentThreadPoolSize;
        data += "\n Largest Queue Size is : " + largestQueueSize;
        data += "\n Largest ThreadPool Size is : " + largestThreadPoolSize;
        data += "\n Max Queue Size is : " + maxQueueSize;
        data += "\n Max ThreadPool Size is : " + maxThreadPoolSize;
        data += "\n Rejected Task Count is : " + rejectedTaskCount;
        data += "\n Total Task Count is : " + totalTaskCount;
        data += "\n ------------------------------------";
        return data;
    }
}
