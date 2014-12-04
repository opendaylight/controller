/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.html
 */

package org.opendaylight.controller.netconf.it;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.netconf.it.NetconfITSecureTest.getSessionListener;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.TestingNetconfClient;
import org.opendaylight.controller.netconf.test.tool.Main.Params;
import org.opendaylight.controller.netconf.test.tool.NetconfDeviceSimulator;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class NetconfITSecureTestTool
{

    //set up port both for testool device and test
    public static final int PORT = 17833;
    private static final InetSocketAddress TLS_ADDRESS = new InetSocketAddress("127.0.0.1", PORT);

    private String xmlFile = "netconfMessages/editConfig.xml";

    private ExecutorService msgExec = Executors.newFixedThreadPool(8);

    Collection<Future<?>> tasks = new LinkedList<Future<?>>();

    final NetconfDeviceSimulator netconfDeviceSimulator = new NetconfDeviceSimulator();

    @Before
    public void setUp() throws Exception {

        //Set up parameters for testtool device
        Params params = new Params();
        params.debug = true;
        params.deviceCount = 1;
        params.startingPort = PORT;
        params.ssh = true;
        params.exi = true;

        final List<Integer> openDevices = netconfDeviceSimulator.start(params);
    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Test all requests are handled properly and no mismatch occurs in listener
     */
    @Test(timeout = 6*60*1000)
    public void testSecureStress() throws Exception {

        final int requests = 4000;

        List<Future<?>> tasks = new ArrayList<>();

        final NetconfClientDispatcher dispatch = new NetconfClientDispatcherImpl(new NioEventLoopGroup(), new NioEventLoopGroup(), new HashedWheelTimer());

        final NetconfDeviceCommunicator sessionListener = getSessionListener();

        try (TestingNetconfClient netconfClient = new TestingNetconfClient("testing-ssh-client", dispatch, NetconfITSecureTest.getClientConfiguration(sessionListener, TLS_ADDRESS));)
        {

            final AtomicInteger responseCounter = new AtomicInteger(0);
            final List<ListenableFuture<RpcResult<NetconfMessage>>> futures = Lists.newArrayList();

            for (int i = 0; i < requests; i++) {

                NetconfMessage getConfig = XmlFileLoader.xmlFileToNetconfMessage(xmlFile);

                getConfig = NetconfITSecureTest.changeMessageId(getConfig,i);

                Runnable worker = new NetconfITSecureTestToolRunnable(getConfig,i, sessionListener, futures, responseCounter);

                tasks.add(msgExec.submit(worker));

            }

            msgExec.shutdown();

            // Wait for every future
            for (final Future<?> task : tasks){
                try
                {

                    task.get(3, TimeUnit.MINUTES);
                } catch (final TimeoutException e) {
                    fail(String.format("Request %d is not responding", tasks.indexOf(task)));
                }
            }

            for (final ListenableFuture<RpcResult<NetconfMessage>> future : futures) {
                try {

                    future.get(3, TimeUnit.MINUTES);
                } catch (final TimeoutException e) {
                    fail(String.format("Reply %d is not responding", futures.indexOf(future)));
                }
            }

            sleep(5000);

            assertEquals(requests, responseCounter.get());

        }
    }

    class NetconfITSecureTestToolRunnable implements Runnable {

        private NetconfMessage getConfig;
        private int it;
        private NetconfDeviceCommunicator sessionListener;
        private List<ListenableFuture<RpcResult<NetconfMessage>>> futures;
        private AtomicInteger responseCounter;

        public NetconfITSecureTestToolRunnable(NetconfMessage getConfig, int it, NetconfDeviceCommunicator sessionListener, List<ListenableFuture<RpcResult<NetconfMessage>>> futures, AtomicInteger responseCounter){
            this.getConfig = getConfig;
            this.it = it;
            this.sessionListener = sessionListener;
            this.futures = futures;
            this.responseCounter = responseCounter;
        }

        @Override
        public void run(){

            ListenableFuture<RpcResult<NetconfMessage>> netconfMessageFuture;

            netconfMessageFuture = sessionListener.sendRequest(getConfig, QName.create("namespace", "2012-12-12", "get"));

            futures.add(netconfMessageFuture);
            Futures.addCallback(netconfMessageFuture, new FutureCallback<RpcResult<NetconfMessage>>() {

                    @Override
                    public void onSuccess(final RpcResult<NetconfMessage> result) {

                        if(result.isSuccessful()&result.getErrors().isEmpty()) {
                            responseCounter.incrementAndGet();
                        } else {

                            fail(String.format("Message result not ok %s", result.getErrors().toString()));

                        }
                    }

                    @Override
                    public void onFailure(final Throwable t) {

                        fail(String.format("Message failed %s", Throwables.getStackTraceAsString(t)));

                    }
                }
            );
        }
    }

}
