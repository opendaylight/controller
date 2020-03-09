/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.backup;

import java.util.Properties;

public class KafkaBackupProducerConfiguration {

    private final String kafkaServerHost;
    private final int kafkaServerPort;
    private final String messageTopic;
    private final int messagePartition;
    private final String messageKey;
    private final Properties kafkaProducerProperties;

    public KafkaBackupProducerConfiguration(String kafkaServerHost, int kafkaServerPort, String messageTopic,
            int messagePartition, String messageKey, Properties kafkaProducerProperties) {
        this.kafkaServerHost = kafkaServerHost;
        this.kafkaServerPort = kafkaServerPort;
        this.messageTopic = messageTopic;
        this.messagePartition = messagePartition;
        this.messageKey = messageKey;
        this.kafkaProducerProperties = kafkaProducerProperties;
    }

    public String getKafkaServerHost() {
        return kafkaServerHost;
    }

    public int getKafkaServerPort() {
        return kafkaServerPort;
    }

    public String getMessageTopic() {
        return messageTopic;
    }

    public int getMessagePartition() {
        return messagePartition;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Properties getKafkaProducerProperties() {
        return kafkaProducerProperties;
    }
}
