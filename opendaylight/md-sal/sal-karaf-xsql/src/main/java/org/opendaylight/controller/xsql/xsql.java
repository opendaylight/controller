package org.opendaylight.controller.xsql;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.XSQLModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "odl", name = "xsql", description = "XSQL Karaf Command")
public class xsql extends OsgiCommandSupport {

    @Option(name = "-o", aliases = { "--option" }, description = "An option to the command", required = false, multiValued = false)
    private String option;

    @Argument(name = "argument", description = "Argument to the command", required = false, multiValued = false)
    private String argument;

    protected Object doExecute() throws Exception {
        if(argument==null){
            System.out.println("Nothing to do..., please specify a command.");
            return null;
        }
        if (XSQLModule.getXSQLAdapter() != null) {
            XSQLModule.getXSQLAdapter().processCommand(new StringBuffer(argument),
                    System.out);
        } else {
            System.out.println("XSQLAdapter instance not initialized.");
        }
        return null;
    }
}
