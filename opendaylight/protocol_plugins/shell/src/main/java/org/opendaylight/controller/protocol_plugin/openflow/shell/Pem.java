package org.opendaylight.controller.protocol_plugin.openflow.shell;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.protocol_plugin.openflow.ITopologyServiceShimShell;

@Command(scope = "shimshell", name = "pem", description="pem")
public class Pem extends OsgiCommandSupport{

    private ITopologyServiceShimShell shimShell;

    @Argument(index = 0, name = "arg0", description = "BWfactor command", required = false, multiValued = false)
    String arg0 = null;

	@Override
	protected Object doExecute() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    public void setShimShell(ITopologyServiceShimShell shimShell) {
        this.shimShell = shimShell;
    }
}
