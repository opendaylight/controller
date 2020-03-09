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

public class KafkaBackupConsumerConfigurationBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaBackupConsumerConfigurationBuilder.class);

    private static final String DEFAULT_TOPIC = "DefaultTopic";
    private static final String DEFAULT_GROUP_ID = "DefaultGroup";
    private static final String DEFAULT_ENABLE_AUTO_COMMIT = "true";
    private static final int DEFAULT_AUTO_COMMIT_INTERVAL_MS = 1000;

    private String kafkaServerHost;
    private int kafkaServerPort;

    private String messageTopic;

    private Properties kafkaConsumerProperties = new Properties();

    public static KafkaBackupConsumerConfigurationBuilder defaultBuilder(final String kafkaServerHost,
            final int kafkaServerPort) {
        return new KafkaBackupConsumerConfigurationBuilder()
                .setKafkaServerHost(kafkaServerHost)
                .setKafkaServerPort(kafkaServerPort)
                .setMessageTopic(DEFAULT_TOPIC)
                .setKafkaConsumerProperties(defaultKafkaConsumerProperties());
    }

    public static KafkaBackupConsumerConfiguration fromJson(final InputStream jsonConfigInput) throws IOException {
        KafkaBackupConsumerConfigurationBuilder builder = new KafkaBackupConsumerConfigurationBuilder();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode configNode = mapper.readTree(jsonConfigInput);
        jsonConfigInput.close();

        builder.setKafkaServerHost(configNode.at("/kafka-connection/kafka-host").asText())
                .setKafkaServerPort(configNode.at("/kafka-connection/kafka-port").asInt())

                .setMessageTopic(configNode.at("/kafka-messaging/topic").asText(DEFAULT_TOPIC));
        if (!configNode.at("/kafka-consumer-properties").isMissingNode()) {
            builder.setKafkaConsumerProperties(parseProperties(configNode.at("/kafka-consumer-properties")));
        } else {
            builder.setKafkaConsumerProperties(defaultKafkaConsumerProperties());
        }
        return builder.build();
    }

    private static Properties parseProperties(JsonNode configProperties) {
        Properties properties = new Properties();
        configProperties.fields().forEachRemaining(f -> properties.put(f.getKey(), f.getValue().asText()));
        return properties;
    }

    private static Properties defaultKafkaConsumerProperties() {
        Properties properties = new Properties();
        properties.put("group.id", DEFAULT_GROUP_ID);
        properties.put("enable.auto.commit", DEFAULT_ENABLE_AUTO_COMMIT);
        properties.put("auto.commit.interval.ms", DEFAULT_AUTO_COMMIT_INTERVAL_MS);
        return properties;
    }

    public KafkaBackupConsumerConfiguration build() {
        kafkaConsumerProperties.putIfAbsent("bootstrap.servers", kafkaServerHost + ":" + kafkaServerPort);
        kafkaConsumerProperties.put("key.deserializer",  "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaConsumerProperties.put("value.deserializer",
                "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        return new KafkaBackupConsumerConfiguration(kafkaServerHost, kafkaServerPort, messageTopic,
                kafkaConsumerProperties);
    }

    public KafkaBackupConsumerConfigurationBuilder setKafkaServerHost(String kafkaServerHost) {
        this.kafkaServerHost = kafkaServerHost;
        return this;
    }

    public KafkaBackupConsumerConfigurationBuilder setKafkaServerPort(int kafkaServerPort) {
        this.kafkaServerPort = kafkaServerPort;
        return this;
    }

    public KafkaBackupConsumerConfigurationBuilder setMessageTopic(String messageTopic) {
        this.messageTopic = messageTopic;
        return this;
    }

    public KafkaBackupConsumerConfigurationBuilder setKafkaConsumerProperties(Properties kafkaConsumerProperties) {
        this.kafkaConsumerProperties = kafkaConsumerProperties;
        return this;
    }
}
