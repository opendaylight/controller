/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.backup;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.opendaylight.controller.cluster.datastore.persisted.DataTreeCandidateInputOutput;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ReusableImmutableNormalizedNodeStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaBackupConsumerService extends AbstractBackupConsumerService{

    private static final Logger LOG = LoggerFactory.getLogger(KafkaBackupConsumerService.class);

    private static final long CONSUMER_SHUTDOWN_TIMEOUT = 5L;
    private static final String SINGLETON_IDENTIFIER = "KafkaBackupConsumerService";

    private final KafkaConsumer<String, byte[]> messageConsumer;
    private ScheduledFuture<?> scheduledFuture;

    public KafkaBackupConsumerService(final DOMDataBroker domDataBroker) {
        super(domDataBroker);
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("KafkaConsumer.config"));
        } catch (Exception e) {
            LOG.error("Couldn't load config");
        }
        properties.put("key.deserializer",  "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        this.messageConsumer = new KafkaConsumer<>(properties);
        messageConsumer.subscribe(Arrays.asList("demo-topic1"));
    }

    @Override
    protected void startConsumption() {
        LOG.info("Init KafkaBackupConsumerService");
        final ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduledFuture = scheduler.scheduleAtFixedRate(()-> {
                ConsumerRecords<String, byte[]> records = messageConsumer.poll(1000);
                for (ConsumerRecord<String, byte[]> record : records) {
                    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(record.value()));
                    try {
                        DataTreeCandidateInputOutput.DataTreeCandidateWithVersion dataTreeCandidateWithVersion =
                                DataTreeCandidateInputOutput.readDataTreeCandidate(dataInputStream,
                                ReusableImmutableNormalizedNodeStreamWriter.create());
                        LOG.info("Received record - DataTreeCandidate: {}", dataTreeCandidateWithVersion.getCandidate().getRootNode()
                                .toString());
                        applyBackup(dataTreeCandidateWithVersion.getCandidate());
                    } catch (IOException e) {
                        LOG.error("Couldn't deserialize DataTreeCandidate from received message.");
                    }
                }
            }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    protected Boolean closeBackupConsumer() {
        scheduledFuture.cancel(true);
        messageConsumer.close(Duration.ofSeconds(CONSUMER_SHUTDOWN_TIMEOUT));
        //TODO: refactor this return value to better represent the shutdown status
        return true;
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return ServiceGroupIdentifier.create(SINGLETON_IDENTIFIER);
    }
}
