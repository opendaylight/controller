/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool.client.http.perf;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.netconf.test.tool.client.stress.ExecutionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncExecutionStrategy implements ExecutionStrategy{

    private static final Logger LOG = LoggerFactory.getLogger(SyncExecutionStrategy.class);

    private final Parameters params;
    private final ArrayList<Request> payloads;
    private final AsyncHttpClient asyncHttpClient;

    SyncExecutionStrategy(final Parameters params, final AsyncHttpClient asyncHttpClient, final ArrayList<Request> payloads) {
        this.params = params;
        this.asyncHttpClient = asyncHttpClient;
        this.payloads = payloads;
    }

    @Override
    public void invoke() {

        LOG.info("Begin sending sync requests");
        for (Request request : payloads) {
            try {
                asyncHttpClient.executeRequest(request).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn(e.toString());
            }
        }
        LOG.info("End sending sync requests");

    }
}
