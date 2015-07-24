/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import java.io.IOException;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.netconf.cli.io.ConsoleIOImpl;

public class ConsoleIOTestImpl extends ConsoleIOImpl {

    Map<String, Deque<String>> inputValues = new HashMap<>();
    String lastMessage;
    private final List<ValueForMessage> valuesForMessages;

    public ConsoleIOTestImpl(final Map<String, Deque<String>> inputValues, final List<ValueForMessage> valuesForMessages)
            throws IOException {
        super();
        this.inputValues = inputValues;
        this.valuesForMessages = valuesForMessages;
    }

    StringBuilder output = new StringBuilder();

    @Override
    public String read() throws IOException {
        final String prompt = buildPrompt();
        output.append(prompt);
        System.console().writer().print(prompt);

        String value = inputValues.get(prompt).pollFirst();
        if (value == null) {
            value = getValueForLastMessage();
        }

        value = value != null ? value : "****NO VALUE****";

        output.append(value + "\n");
        System.console().writer().println(value + "\n");
        return value;
    }

    private String getValueForLastMessage() {
        for (final ValueForMessage valueForMessage : valuesForMessages) {
            if (containsLastMessageKeyWords(valueForMessage.getKeyWords())) {
                return valueForMessage.getValue();
            }
        }
        return null;
    }

    private boolean containsLastMessageKeyWords(final List<String> keyWords) {
        for (final String keyWord : keyWords) {
            if (!lastMessage.contains(keyWord)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void write(final CharSequence data) throws IOException {
        output.append(data);
        lastMessage = (String) data;
        System.console().writer().print(data);
    }

    @Override
    public void writeLn(final CharSequence data) throws IOException {
        write(data);
        output.append("\n");
        System.console().writer().print("\n");
    }

    public String getConsoleOutput() {
        return output.toString();
    }
}
