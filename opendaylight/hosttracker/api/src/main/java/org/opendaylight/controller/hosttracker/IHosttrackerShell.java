package org.opendaylight.controller.hosttracker;

import org.eclipse.osgi.framework.console.CommandInterpreter;

public interface IHosttrackerShell {

    public void dumpPendingARPReqList(CommandInterpreter ci);
    public void dumpFailedARPReqList(CommandInterpreter ci);

}