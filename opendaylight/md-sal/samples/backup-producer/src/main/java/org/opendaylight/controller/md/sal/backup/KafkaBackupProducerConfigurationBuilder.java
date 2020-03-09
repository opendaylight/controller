/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.backup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaBackupProducerConfigurationBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaBackupProducerConfigurationBuilder.class);

    private static final String DEFAULT_TOPIC = "DefaultTopic";
    private static final String DEFAULT_KEY = "DefaultKey";
    private static final int DEFAULT_PARTITION = 0;
    private static final int DEFAULT_PROPERTIES_RETRIES = 0;
    private static final int DEFAULT_PROPERTIES_LINGER_MS = 1;
    private static final int DEFAULT_PROPERTIES_MAX_BLOCK_MS = 1000;

    private String kafkaServerHost;
    private int kafkaServerPort;

    private String messageTopic;
    private int messagePartition;
    private String messageKey;

    private Properties kafkaProducerProperties = new Properties();

    public static KafkaBackupProducerConfigurationBuilder defaultBuilder(final String kafkaServerHost,
            final int kafkaServerPort) {
        return new KafkaBackupProducerConfigurationBuilder()
                .setKafkaServerHost(kafkaServerHost)
                .setKafkaServerPort(kafkaServerPort)
                .setMessageTopic(DEFAULT_TOPIC)
                .setMessagePartition(DEFAULT_PARTITION)
                .setMessageKey(DEFAULT_KEY)
                .setKafkaProducerProperties(defaultKafkaProducerProperties());
    }

    public static KafkaBackupProducerConfiguration fromJson(final InputStream jsonConfigInput) throws IOException {
        KafkaBackupProducerConfigurationBuilder builder = new KafkaBackupProducerConfigurationBuilder();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode configNode = mapper.readTree(jsonConfigInput);
        jsonConfigInput.close();

        builder.setKafkaServerHost(configNode.at("/kafka-connection/kafka-host").asText())
                .setKafkaServerPort(configNode.at("/kafka-connection/kafka-port").asInt())

                .setMessageTopic(configNode.at("/kafka-messaging/topic").asText(DEFAULT_TOPIC))
                .setMessageKey(configNode.at("/kafka-messaging/key").asText(DEFAULT_KEY))
                .setMessagePartition(configNode.at("/kafka-messaging/partition").asInt(DEFAULT_PARTITION));
        if (!configNode.at("/kafka-producer-properties").isMissingNode()) {
            builder.setKafkaProducerProperties(parseProperties(configNode.at("/kafka-producer-properties")));
        } else {
            builder.setKafkaProducerProperties(defaultKafkaProducerProperties());
        }
        return builder.build();
    }

    private static Properties parseProperties(JsonNode configProperties) {
        Properties properties = new Properties();
        configProperties.fields().forEachRemaining(f -> properties.put(f.getKey(), f.getValue().asText()));
        return properties;
    }

    private static Properties defaultKafkaProducerProperties() {
        Properties properties = new Properties();
        properties.put("retries", DEFAULT_PROPERTIES_RETRIES);
        properties.put("linger.ms", DEFAULT_PROPERTIES_LINGER_MS);
        properties.put("max.block.ms", DEFAULT_PROPERTIES_MAX_BLOCK_MS);
        return properties;
    }

    public KafkaBackupProducerConfiguration build() {
        kafkaProducerProperties.putIfAbsent("bootstrap.servers", kafkaServerHost + ":" + kafkaServerPort);
        kafkaProducerProperties.putIfAbsent("key.serializer",
                "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducerProperties.putIfAbsent("value.serializer",
                "org.apache.kafka.common.serialization.ByteArraySerializer");
        return new KafkaBackupProducerConfiguration(kafkaServerHost, kafkaServerPort, messageTopic, messagePartition,
                messageKey, kafkaProducerProperties);
    }

    public KafkaBackupProducerConfigurationBuilder setKafkaServerHost(String kafkaServerHost) {
        this.kafkaServerHost = kafkaServerHost;
        return this;
    }

    public KafkaBackupProducerConfigurationBuilder setKafkaServerPort(int kafkaServerPort) {
        this.kafkaServerPort = kafkaServerPort;
        return this;
    }

    public KafkaBackupProducerConfigurationBuilder setMessageTopic(String messageTopic) {
        this.messageTopic = messageTopic;
        return this;
    }

    public KafkaBackupProducerConfigurationBuilder setMessagePartition(int messagePartition) {
        this.messagePartition = messagePartition;
        return this;
    }

    public KafkaBackupProducerConfigurationBuilder setMessageKey(String messageKey) {
        this.messageKey = messageKey;
        return this;
    }

    public KafkaBackupProducerConfigurationBuilder setKafkaProducerProperties(Properties kafkaProducerProperties) {
        this.kafkaProducerProperties = kafkaProducerProperties;
        return this;
    }
}
