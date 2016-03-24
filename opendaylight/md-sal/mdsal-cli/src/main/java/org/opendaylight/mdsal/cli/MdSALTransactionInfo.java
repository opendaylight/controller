/*
 * Copyright (c) 2016 CableLabs and others.  All rights reserved.
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
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(scope = "mdsal", name = "Info", description = "MDSAL Transactions Information Command")
public class MdSALTransactionInfo extends AbstractAction {

    @Option(name = "-h", aliases = { "--help" }, description = "mdsal:TransInfo show a valuable info about MD-SAL Transactions", required = false, multiValued = false)
    private final String help = "";

    @Override
    protected Object doExecute() {
        try {
            ObjectName commitStats = new ObjectName("org.opendaylight.controller:type=DOMDataBroker,name=CommitStats");
            MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
            String avgCommitTime = (String) mbeanServer.getAttribute(commitStats, "AverageCommitTime");
            String longestCommitTime = (String) mbeanServer.getAttribute(commitStats, "LongestCommitTime");
            String shortestCommitTime = (String) mbeanServer.getAttribute(commitStats, "ShortestCommitTime");
            Long totalCommits = (Long) mbeanServer.getAttribute(commitStats, "TotalCommits");
            String info = " Shortest Commit Time is : " + shortestCommitTime + "\n Longest Commit Time is : " + longestCommitTime +
                    "\n Average Commit Time is : " + avgCommitTime + "\n Total Commits is : " + totalCommits;
            return info;
        } catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e) {
            return e.getMessage();
        }
    }
}
