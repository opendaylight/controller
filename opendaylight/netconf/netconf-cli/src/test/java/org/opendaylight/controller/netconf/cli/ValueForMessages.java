package org.opendaylight.controller.netconf.cli;

import java.util.Arrays;
import java.util.List;

class ValueForMessage {
    List<String> messageKeyWords;
    String value;

    public ValueForMessage(String value, String... messageKeyWords) {
        this.messageKeyWords = Arrays.asList(messageKeyWords);
        this.value = value;
    }

    public List<String> getKeyWords() {
        return messageKeyWords;
    }

    public String getValue() {
        return value;
    }
}
