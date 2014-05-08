package org.opendaylight.controller.hosttracker;

import java.util.List;

public interface IHosttrackerShell {

    public List<Object> getDumpPendingArpReqList();
    public List<Object> getDumpFailedArpReqList();

}