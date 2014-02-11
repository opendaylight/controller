/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.persist.impl.osgi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;

import com.google.common.collect.Lists;

class MockNetconfEndpoint implements AutoCloseable {

    public static final int READ_SOCKET_TIMEOUT = 3000;

    public static final String MSG_SEPARATOR = "]]>]]>\n";

    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private List<String> receivedMessages = Lists.newCopyOnWriteArrayList();
    private Thread innerThread;

    MockNetconfEndpoint(String capability, String netconfPort, List<MessageSequence> messageSequence) {
        helloMessage = helloMessage.replace("capability_place_holder", capability);
        start(netconfPort, messageSequence);
    }

    private String helloMessage = "<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
            "<capabilities>\n" +
            "<capability>capability_place_holder</capability>\n" +
            "</capabilities>\n" +
            "<session-id>1</session-id>\n" +
            "</hello>\n" +
            MSG_SEPARATOR;

    public static String conflictingVersionErrorMessage;
    static {
        try {
            conflictingVersionErrorMessage = XmlUtil.toString(XmlFileLoader
                    .xmlFileToDocument("netconfMessages/conflictingversion/conflictingVersionResponse.xml")) + MSG_SEPARATOR;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String okMessage = "<rpc-reply message-id=\"1\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
            "<ok/>\n" +
            "</rpc-reply>" +
            MSG_SEPARATOR ;

    private void start(final String port, final List<MessageSequence> messagesToSend) {
        innerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int clientCounter = 0;

                while (stopped.get() == false) {
                    try (ServerSocket s = new ServerSocket(Integer.valueOf(port))) {
                        s.setSoTimeout(READ_SOCKET_TIMEOUT);

                        Socket clientSocket = s.accept();
                        clientCounter++;
                        clientSocket.setSoTimeout(READ_SOCKET_TIMEOUT);

                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                        // Negotiate
                        sendMessage(out, helloMessage);
                        receiveMessage(in);

                        // Accept next message (edit-config)
                        receiveMessage(in);

                        for (String message : getMessageSequenceForClient(messagesToSend, clientCounter)) {
                            sendMessage(out, message);
                            receiveMessage(in);
                        }
                    } catch (SocketTimeoutException e) {
                        // No more activity on netconf endpoint, close
                        return;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            private Iterable<? extends String> getMessageSequenceForClient(List<MessageSequence> messagesToSend,
                    int clientCounter) {
                if (messagesToSend.size() <= clientCounter) {
                    return messagesToSend.get(messagesToSend.size() - 1).getMessages();
                } else {
                    return messagesToSend.get(clientCounter - 1).getMessages();
                }
            }

            private void receiveMessage(BufferedReader in) throws Exception {
                String message = readMessage(in);
                if(message == null || message.equals(""))
                    return;
                receivedMessages.add(message);
            }

            private String readMessage(BufferedReader in) throws IOException {
                int c;
                StringBuilder b = new StringBuilder();

                while((c = in.read()) != -1) {
                    b.append((char)c);
                    if(b.toString().endsWith("]]>]]>"))
                        break;
                }

                return b.toString();
            }

            private void sendMessage(PrintWriter out, String message) throws InterruptedException {
                out.print(message);
                out.flush();
            }

        });
        innerThread.setName("Mocked-netconf-endpoint-inner-thread");
        innerThread.start();
    }

    public List<String> getReceivedMessages() {
        return receivedMessages;
    }

    public void close() throws IOException, InterruptedException {
        stopped.set(true);
        innerThread.join();
    }

    static class MessageSequence {
        private List<String> messages;

        MessageSequence(List<String> messages) {
            this.messages = messages;
        }

        MessageSequence(String... messages) {
            this(Lists.newArrayList(messages));
        }

        public Collection<String> getMessages() {
            return messages;
        }
    }
}
