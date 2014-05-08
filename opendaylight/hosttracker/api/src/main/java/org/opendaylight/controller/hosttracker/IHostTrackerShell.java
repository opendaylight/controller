package org.opendaylight.controller.hosttracker;

import java.util.List;

public interface IHostTrackerShell{

    public List<String> dumpPendingArpReqList();
    public List<String> dumpFailedArpReqList();
}