/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.impl;

import java.io.IOException;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;

import com.google.common.base.Optional;

public class DecisionReader {

    private static final String YES = "Y";
    private static final String NO = "N";
    public static final Completer YES_NO_COMPLETER = new StringsCompleter(YES, NO);

    public boolean read(final ConsoleIO console, final String questionMessageBlueprint, final Object ... questionMessageArgs) throws IOException {
        final ConsoleContext ctx = getContext();
        console.enterContext(ctx);
        try {
            console.formatLn(questionMessageBlueprint, questionMessageArgs);
            final String rawConsoleValue = console.read();
            return YES.equals(rawConsoleValue.toUpperCase());
        } finally {
            console.leaveContext();
        }
    }

    private static ConsoleContext getContext() {
        return new ConsoleContext() {

            @Override
            public Optional<String> getPrompt() {
                return Optional.absent();
            }

            @Override
            public Completer getCompleter() {
                return YES_NO_COMPLETER;
            }
        };
    }

}
