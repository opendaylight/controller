/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import org.jolokia.jvmagent.JolokiaServer;
import org.jolokia.jvmagent.JvmAgentConfig;

public class JolokiaHelper {
    private static JolokiaServer jolokiaServer;

    /**
     * Bind to port 17777. By convention, ports above 10000 are used for testing
     * and < 10000 for production
     *
     * @return url that can be passed to new J4pClient(url)
     */
    public static String startTestingJolokia() {
        return startJolokia("localhost", 17777);
    }

    /**
     * @return url that can be passed to new J4pClient(url)
     * @throws IOException
     */
    public static String startJolokia(String host, int port) {
        String agentArgs = "host=" + host + ",port=" + port;
        JvmAgentConfig config = new JvmAgentConfig(agentArgs);
        Exception lastException = null;
        for (int i = 0; i < 10; i++) {
            try {
                jolokiaServer = new JolokiaServer(config, false);
                jolokiaServer.start();
                return "http://" + host + ":" + port + "/jolokia/";
            } catch (Exception e) {
                lastException = e;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
        throw new RuntimeException(lastException);
    }

    public static void stopJolokia() {
        jolokiaServer.stop();
    }
}
