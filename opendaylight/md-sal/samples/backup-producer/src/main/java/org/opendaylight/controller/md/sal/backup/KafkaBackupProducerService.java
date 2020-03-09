/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.backup;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.datastore.persisted.DataTreeCandidateInputOutput;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ClusteredDOMDataTreeChangeListener which listens on the root of CONFIG dataTree. This means that any modification
 * to the dataTree is caught and sent to the Kafka stream. There should be a BackupConsumer running on the backup
 * site which is connected to the same Kafka stream. The consumer will then apply the received modifications to the
 * backup site.
 * */
public class KafkaBackupProducerService implements ClusterSingletonService, ClusteredDOMDataTreeChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaBackupProducerService.class);

    private static final long PRODUCER_SHUTDOWN_TIMEOUT = 5L;
    private static final String SINGLETON_IDENTIFIER = "KafkaBackupProducerService";

    private final DOMDataBroker domDataBroker;

    private final KafkaProducer<String, byte[]> messageProducer;
    private long testMsgCount = 0;

    public KafkaBackupProducerService(final DOMDataBroker domDataBroker) {
        this.domDataBroker = domDataBroker;
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "tibor-VirtualBox:9092");
        properties.put("retries", 0);
        properties.put("linger.ms", 1);
        properties.put("max.block.ms", 1000);
        properties.put("key.serializer",  "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        messageProducer = new KafkaProducer<>(properties);
    }

    @Override
    public void instantiateServiceInstance() {

        LOG.info("Init KafkaBackupProducerService");
        domDataBroker.getExtensions().getInstance(DOMDataTreeChangeService.class).registerDataTreeChangeListener(
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty()), this);
        //Todo: remove this test backup invocation
        /*for (int i = 0; i < 2; i++) {
            backup(null);
        }*/
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeCandidate> changes) {
        for (DataTreeCandidate candidate : changes) {
            LOG.info("Received DataTreeCandidate change, sending to backup");
            backup(candidate);
        }
    }

    private void backup(final DataTreeCandidate candidate) {
        //TODO: send actual candidate instead of dummy data
        LOG.info("Producer - sending message - {}", candidate.getRootNode().toString());

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream =new DataOutputStream(byteArrayOutputStream);
            DataTreeCandidateInputOutput.writeDataTreeCandidate(dataOutputStream, candidate);
            messageProducer.send(new ProducerRecord<>("demo-topic1", "testKey",
                    byteArrayOutputStream.toByteArray())).get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Producer - failed to send message: {}", testMsgCount);
        } catch (IOException e) {
            LOG.error("Failed to write DataTreeCandidate to byteOutputStreem: {}", testMsgCount);
        }

        testMsgCount++;
    }

    @Override
    public ListenableFuture<? extends Object> closeServiceInstance() {
        messageProducer.close(Duration.ofSeconds(PRODUCER_SHUTDOWN_TIMEOUT));
        //TODO: refactor this return value to better represent the shutdown status
        return Futures.immediateFuture(Boolean.TRUE);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return ServiceGroupIdentifier.create(SINGLETON_IDENTIFIER);
    }
}
