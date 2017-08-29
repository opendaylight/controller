/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.cli;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.controller.md.sal.trace.api.TracingDOMDataBroker;

/**
 * Karaf CLI command to dump all open transactions.
 *
 * @author Michael Vorburger.ch
 */
@Service
@Command(scope = "trace", name = "transactions",
    description = "Show all (still) open transactions; including stack trace of creator, "
    + "if transaction-debug-context-enabled is true in mdsaltrace_config.xml")
public class PrintOpenTransactionsCommand implements Action {

    @Reference
    private TracingDOMDataBroker tracingDOMDataBroker;

    // NB: Do NOT have a non-default constructor for injection of @Reference
    // Karaf needs a default constructor to create the command - and it works as is.

    @Override
    @SuppressWarnings("checkstyle:RegexpSingleLineJava")
    public Object execute() throws Exception {
        if (!tracingDOMDataBroker.printOpenTransactions(System.out)) {
            System.out.println("No open transactions, great!");
        }
        return null;
    }

}
