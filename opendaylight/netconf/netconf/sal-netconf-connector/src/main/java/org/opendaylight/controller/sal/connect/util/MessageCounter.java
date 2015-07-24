package org.opendaylight.controller.sal.connect.util;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class MessageCounter {
    final AtomicInteger messageId = new AtomicInteger(0);

    private static final String messageIdBlueprint = "%s-%s";

    public String getNewMessageId(final String prefix) {
        Preconditions.checkArgument(Strings.isNullOrEmpty(prefix) == false, "Null or empty prefix");
        return String.format(messageIdBlueprint, prefix, getNewMessageId());
    }

    public String getNewMessageId() {
        return Integer.toString(messageId.getAndIncrement());
    }
}
