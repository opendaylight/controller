/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.maven.plugin.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.VmIdentifier;

public class VMProcessMonitor extends ProcessMonitor {

    @Override
    public List<JavaProcess> getProcesses() {
        Set<Integer> activeVmPids = null;
        List<JavaProcess> result = new ArrayList<JavaProcess>();
        MonitoredHost monitoredHost = null;
        MonitoredVm mvm = null;
        try {
            monitoredHost = MonitoredHost.getMonitoredHost(
                    new HostIdentifier((String) null));
            activeVmPids = monitoredHost.activeVms();
        } catch (Exception e) {
            throw new IllegalStateException("Error accessing VM", e);
        }
        for (Integer vmPid : activeVmPids) {
            try {
                mvm = monitoredHost.getMonitoredVm(
                        new VmIdentifier(vmPid.toString()));
                JavaProcess proc = new JavaProcess(vmPid,
                        MonitoredVmUtil.mainClass(mvm, true));
                proc.setSystemProperties(MonitoredVmUtil.jvmArgs(mvm));
                proc.setSystemProperties(MonitoredVmUtil.jvmFlags(mvm));
                result.add(proc);
            } catch(Exception e2) {
                log("Error connecting to pid: " + vmPid + " reason:"
                    + e2.getMessage());
                e2.printStackTrace();
            } finally {
                if (mvm != null) {
                    mvm.detach();
                }
            }
        }
        return result;
    }


}
