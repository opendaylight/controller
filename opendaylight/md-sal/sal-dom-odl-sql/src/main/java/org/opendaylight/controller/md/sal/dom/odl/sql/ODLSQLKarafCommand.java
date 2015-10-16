/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.odl.sql;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.odl.sql.rev151016.OdlSqlService;

@Command(scope = "odl", name = "sql", description = "ODL SQL Karaf Command")
public class ODLSQLKarafCommand extends OsgiCommandSupport {

    @Option(name = "-o", aliases = { "--option" }, description = "An option to the command", required = false, multiValued = false)
    private String option;

    @Argument(name = "argument", description = "Argument to the command", required = false, multiValued = false)
    private String argument;

    protected Object doExecute() throws Exception {
        if(argument==null){
            System.out.println("Nothing to do..., please specify a command.");
            return null;
        }
        OdlSqlService service = this.getService(OdlSqlService.class);
        ((ODLSQLServiceImpl)service).getAdapter().processCommand(new StringBuffer(argument),System.out);
        return null;
    }
}
