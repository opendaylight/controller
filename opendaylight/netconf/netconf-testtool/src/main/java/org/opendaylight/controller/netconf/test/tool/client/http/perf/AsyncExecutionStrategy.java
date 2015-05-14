/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool.client.http.perf;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.controller.netconf.test.tool.client.stress.ExecutionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncExecutionStrategy implements ExecutionStrategy{

    private static final Logger LOG = LoggerFactory.getLogger(AsyncExecutionStrategy.class);

    private final Parameters params;
    private final ArrayList<Request> payloads;
    private final AsyncHttpClient asyncHttpClient;
    private final AtomicInteger requestThrottle = new AtomicInteger();

    AsyncExecutionStrategy(final Parameters params, final AsyncHttpClient asyncHttpClient, final ArrayList<Request> payloads) {
        this.params = params;
        this.asyncHttpClient = asyncHttpClient;
        this.payloads = payloads;
    }

    @Override
    public void invoke() {
        final ArrayList<ListenableFuture<Response>> futures = new ArrayList<>();
        LOG.info("Begin sending async requests");

        for (final Request request : payloads) {
            while (requestThrottle.get() == 5000) {
                try {
                    Thread.sleep(1l);
                } catch (InterruptedException e) {
                    LOG.error("Unable to sleep thread", e);
                }
            }
            futures.add(asyncHttpClient.executeRequest(request, new AsyncCompletionHandler<Response>() {
                @Override
                public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
                    super.onStatusReceived(status);
                    if (status.getStatusCode() != 200 && status.getStatusCode() != 204) {
                        LOG.warn("Request failed, status code: {}", status.getStatusCode() + status.getStatusText());
                        LOG.warn("request: {}", request.toString());
                    }
                    return STATE.CONTINUE;
                }

                @Override
                public Response onCompleted(Response response) throws Exception {
                    requestThrottle.decrementAndGet();
                    return response;
                }
            }));
            requestThrottle.incrementAndGet();
        }
        LOG.info("Requests sent, waiting for responses");

        while (requestThrottle.get() > 0) {
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                LOG.error("Unable to sleep thread", e);
            }
        }

        LOG.info("Throttle: {}", requestThrottle);
//        for (ListenableFuture<Response> future : futures) {
//            try {
//                Response response = future.get();
//                if (response.getStatusCode() != 200 && response.getStatusCode() != 204) {
//                    LOG.warn(response.getResponseBody());
//                }
//            } catch (InterruptedException | ExecutionException | IOException e) {
//                LOG.warn(e.toString());
//            }
//        }

        LOG.info("Responses received, ending...");
    }
}
