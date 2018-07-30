/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.cli;

import java.util.List;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
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

    @Argument(index = 0, name = "minOpenTransactions", required = false, multiValued = false,
            description = "Minimum open number of transactions (leaks with fewer are suppressed)")
    Integer minOpenTransactions = 1;

    @Reference
    private List<TracingDOMDataBroker> tracingDOMDataBrokers;

    // NB: Do NOT have a non-default constructor for injection of @Reference
    // Karaf needs a default constructor to create the command - and it works as is.

    @Override
    @SuppressWarnings("checkstyle:RegexpSingleLineJava")
    public Object execute() {
        boolean hasFound = false;
        for (TracingDOMDataBroker tracingDOMDataBroker : tracingDOMDataBrokers) {
            hasFound |= tracingDOMDataBroker.printOpenTransactions(System.out, minOpenTransactions);
        }
        if (hasFound) {
            System.out.println(
                    "Actually did find real leaks with more than " + minOpenTransactions + " open transactions");
        } else {
            System.out.println(
                    "Did not find any real leaks with more than " + minOpenTransactions + " open transactions");
        }
        return hasFound;
    }

}
