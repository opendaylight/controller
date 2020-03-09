/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.backup;

import java.util.Properties;

public class KafkaBackupConsumerConfiguration {

    private final String kafkaServerHost;
    private final int kafkaServerPort;
    private final String messageTopic;
    private final Properties kafkaConsumerProperties;

    public KafkaBackupConsumerConfiguration(final String kafkaServerHost, final int kafkaServerPort,
            final String messageTopic, final Properties kafkaConsumerProperties) {
        this.kafkaServerHost = kafkaServerHost;
        this.kafkaServerPort = kafkaServerPort;
        this.messageTopic = messageTopic;
        this.kafkaConsumerProperties = kafkaConsumerProperties;
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

    public Properties getKafkaConsumerProperties() {
        return kafkaConsumerProperties;
    }
}
