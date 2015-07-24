/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.SKIP;

import com.google.common.base.Optional;
import java.io.IOException;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import jline.internal.Log;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.io.IOUtil;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;

public class DecisionReader {

    private static final String YES = "Y";
    private static final String NO = "N";
    public static final Completer YES_NO_COMPLETER = new StringsCompleter(YES, NO);

    public Optional<Boolean> read(final ConsoleIO console, final String questionMessageBlueprint,
            final Object... questionMessageArgs) throws IOException, ReadingException {
        final ConsoleContext ctx = getContext();
        console.enterContext(ctx);
        try {
            console.formatLn(questionMessageBlueprint, questionMessageArgs);
            final String rawValue = console.read();
            if (YES.equals(rawValue.toUpperCase())) {
                return Optional.of(Boolean.TRUE);
            } else if (NO.equals(rawValue.toUpperCase())) {
                return Optional.of(Boolean.FALSE);
            } else if (SKIP.equals(rawValue)) {
                return Optional.absent();
            } else {
                final String message = String.format("Incorrect possibility (%s) was selected", rawValue);
                Log.error(message);
                throw new ReadingException(message);
            }
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
                return new AggregateCompleter(YES_NO_COMPLETER, new StringsCompleter(IOUtil.SKIP));
            }

        };
    }

}
