package org.opendaylight.controller.netconf.cli;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.netconf.cli.io.ConsoleIOImpl;

public class ConsoleIOTestImpl extends ConsoleIOImpl {

    Map<String, String> inputValues = new HashMap<>();
    String lastMessage;
    private List<ValueForMessage> valuesForMessages;

    public ConsoleIOTestImpl(Map<String, String> inputValues, List<ValueForMessage> valuesForMessages)
            throws IOException {
        super();
        this.inputValues = inputValues;
        this.valuesForMessages = valuesForMessages;
    }

    StringBuilder output = new StringBuilder();

    @Override
    public String read() throws IOException {
        String prompt = buildPrompt();
        output.append(prompt);
        System.out.print(prompt);

        String lastElement = contexts.peek().getPrompt();
        String value;
        if (lastElement != null) {
            value = inputValues.get(lastElement);
        } else {
            value = getValueForLastMessage();
        }

        value = value != null ? value : "****NO VALUE****";

        output.append(value + "\n");
        System.out.print(value + "\n");
        return value;
    }

    private String getValueForLastMessage() {
        for (ValueForMessage valueForMessage : valuesForMessages) {
            if (containsLastMessageKeyWords(valueForMessage.getKeyWords())) {
                return valueForMessage.getValue();
            }
        }
        return null;
    }

    private boolean containsLastMessageKeyWords(List<String> keyWords) {
        for (String keyWord : keyWords) {
            if (!lastMessage.contains(keyWord)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void write(CharSequence data) throws IOException {
        output.append(data);
        lastMessage = (String) data;
        System.out.print(data);
    }

    @Override
    public void writeLn(CharSequence data) throws IOException {
        write(data);
        output.append("\n");
        System.out.print("\n");
    }

    public String getConsoleOutput() {
        return output.toString();
    }
}
