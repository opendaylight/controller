package org.opendaylight.controller.xsql;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;

@Command(scope = "odl", name = "xsql", description = "XSQL Karaf Command")
public class xsql extends OsgiCommandSupport {

    @Option(name = "-o", aliases = { "--option" }, description = "An option to the command", required = false, multiValued = false)
    private String option;

    @Argument(name = "argument", description = "Argument to the command", required = false, multiValued = false)
    private String argument;

    protected Object doExecute() throws Exception {
        XSQLAdapter.getInstance().processCommand(new StringBuffer(argument),
                System.out);
        return null;
    }
}
