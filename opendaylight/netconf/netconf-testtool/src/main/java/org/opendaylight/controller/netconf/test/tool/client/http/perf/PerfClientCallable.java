/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool.client.http.perf;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import org.opendaylight.controller.netconf.test.tool.client.http.perf.RestPerfClient.DestToPayload;
import org.opendaylight.controller.netconf.test.tool.client.stress.ExecutionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerfClientCallable implements Callable<Void>{

    private static final Logger LOG = LoggerFactory.getLogger(PerfClientCallable.class);

    private final Parameters params;
    private final ArrayList<Request> payloads;
    private final AsyncHttpClient asyncHttpClient;
    private ExecutionStrategy executionStrategy;

    public PerfClientCallable(Parameters params, ArrayList<DestToPayload> payloads) {
        this.params = params;
        this.asyncHttpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder()
                .setConnectTimeout(Integer.MAX_VALUE)
                .setRequestTimeout(Integer.MAX_VALUE)
                .setAllowPoolingConnections(true)
                .build());
        this.payloads = new ArrayList<>();
        for (DestToPayload payload : payloads) {
            this.payloads.add(asyncHttpClient.preparePut(payload.getDestination())
                    .addHeader("content-type", "application/json")
                    .addHeader("Accept", "application/xml")
                    .setBody(payload.getPayload())
                    .setRequestTimeout(Integer.MAX_VALUE)
                    .build());
        }
        executionStrategy = getExecutionStrategy();
    }

    private ExecutionStrategy getExecutionStrategy() {
        return params.async
                ? new AsyncExecutionStrategy(params, asyncHttpClient, payloads)
                : new SyncExecutionStrategy(params, asyncHttpClient, payloads);
    }

    @Override
    public Void call() throws Exception{

        executionStrategy.invoke();
        asyncHttpClient.closeAsynchronously();
        return null;
    }
}
