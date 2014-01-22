/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.websocket;

import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;
import org.opendaylight.controller.websocket.client.WebSocketIClient;
import org.opendaylight.controller.websocket.client.callback.IClientMessageCallback;
import org.opendaylight.controller.websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketClientTest {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientTest.class.toString());
    private static final String MESSAGE = "Take me to your leader!";
    private static final int port = 8080;
    private Thread webSocketServerThread;



    public void startWebSocketServer(){
        try {
            WebSocketServer webSocketServer = new WebSocketServer(port);
            webSocketServerThread = new Thread(webSocketServer);
            webSocketServerThread.setDaemon(false);
            webSocketServerThread.start();
        } catch (Exception e) {
            logger.trace("Error ");
        }
    }
    @Test
    public void connectAndSendData(){
        this.startWebSocketServer();
        URI uri = null;
        try {
            uri = new URI("http://localhost:8080/websocket");
        } catch (URISyntaxException e) {
            logger.trace("");
        }
        ClientMessageCallback messageCallback = new ClientMessageCallback();
        WebSocketIClient wsClient = new WebSocketIClient(uri,messageCallback);
        try {
            wsClient.connect();
        } catch (InterruptedException e) {
            logger.info("WebSocket client couldn't connect to : "+uri);
        }
        wsClient.writeAndFlush(MESSAGE);
        wsClient.writeAndFlush(new CloseWebSocketFrame());
        webSocketServerThread.interrupt();
    }

    private class ClientMessageCallback implements IClientMessageCallback{

        @Override
        public void onMessageReceived(Object message) {
           logger.info("received message {}",message);
           System.out.println("received message : "+message);
        }
    }
}
