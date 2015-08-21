/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool.client.stress;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AsyncExecutionStrategy implements ExecutionStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncExecutionStrategy.class);

    private final Parameters params;
    private final List<NetconfMessage> preparedMessages;
    private final NetconfDeviceCommunicator sessionListener;
    private final List<Integer> editBatches;
    private final int editAmount;

    public AsyncExecutionStrategy(final Parameters params, final List<NetconfMessage> editConfigMsgs, final NetconfDeviceCommunicator sessionListener) {
        this.params = params;
        this.preparedMessages = editConfigMsgs;
        this.sessionListener = sessionListener;
        this.editBatches = countEditBatchSizes(params, editConfigMsgs.size());
        editAmount = editConfigMsgs.size();
    }

    private static List<Integer> countEditBatchSizes(final Parameters params, final int amount) {
        final List<Integer> editBatches = Lists.newArrayList();
        if (params.editBatchSize != amount) {
            final int fullBatches = amount / params.editBatchSize;
            for (int i = 0; i < fullBatches; i++) {
                editBatches.add(params.editBatchSize);
            }

            if (amount % params.editBatchSize != 0) {
                editBatches.add(amount % params.editBatchSize);
            }
        } else {
            editBatches.add(params.editBatchSize);
        }
        return editBatches;
    }

    @Override
    public void invoke() {
        final AtomicInteger responseCounter = new AtomicInteger(0);
        final List<ListenableFuture<RpcResult<NetconfMessage>>> futures = Lists.newArrayList();

        int batchI = 0;
        for (final Integer editBatch : editBatches) {
            for (int i = 0; i < editBatch; i++) {
                final int msgId = i + (batchI * params.editBatchSize);
                final NetconfMessage msg = preparedMessages.get(msgId);
                LOG.debug("Sending message {}", msgId);
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Sending message {}", XmlUtil.toString(msg.getDocument()));
                }
                final ListenableFuture<RpcResult<NetconfMessage>> netconfMessageFuture =
                        sessionListener.sendRequest(msg, StressClient.EDIT_QNAME);
                futures.add(netconfMessageFuture);
            }
            batchI++;
            LOG.info("Batch {} with size {} sent. Committing", batchI, editBatch);
            if (params.candidateDatastore) {
                futures.add(sessionListener.sendRequest(StressClient.COMMIT_MSG, StressClient.COMMIT_QNAME));
            }
        }

        LOG.info("All batches sent. Waiting for responses");
        // Wait for every future
        for (final ListenableFuture<RpcResult<NetconfMessage>> future : futures) {
            try {
                final RpcResult<NetconfMessage> netconfMessageRpcResult = future.get(params.msgTimeout, TimeUnit.SECONDS);
                if(netconfMessageRpcResult.isSuccessful()) {
                    responseCounter.incrementAndGet();
                    LOG.debug("Received response {}", responseCounter.get());
                } else {
                    LOG.warn("Request failed {}", netconfMessageRpcResult);
                }
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            } catch (final ExecutionException | TimeoutException e) {
                throw new RuntimeException("Request not finished", e);
            }
        }

        Preconditions.checkState(responseCounter.get() == editAmount + (params.candidateDatastore ? editBatches.size() : 0),
                "Not all responses were received, only %s from %s", responseCounter.get(), params.editCount + editBatches.size());
    }
}
