/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.handler.ssh.virtualsocket;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

/**
 * Handler class providing Socket functionality to OIO client application. By using VirtualSocket user can
 * use OIO application in asynchronous environment and NIO EventLoop. Using VirtualSocket OIO applications
 * are able to use full potential of NIO environment.
 */
public class VirtualSocket extends Socket implements ChannelHandler {
    private final ChannelInputStream chis = new ChannelInputStream();
    private final ChannelOutputStream chos = new ChannelOutputStream();
    private ChannelHandlerContext ctx;


    public InputStream getInputStream() {
        return this.chis;
    }

    public OutputStream getOutputStream() {
        return this.chos;
    }

    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;

        if (ctx.channel().pipeline().get("outputStream") == null) {
            ctx.channel().pipeline().addFirst("outputStream", chos);
        }

        if (ctx.channel().pipeline().get("inputStream") == null) {
            ctx.channel().pipeline().addFirst("inputStream", chis);
        }
    }

    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().pipeline().get("outputStream") != null) {
            ctx.channel().pipeline().remove("outputStream");
        }

        if (ctx.channel().pipeline().get("inputStream") != null) {
            ctx.channel().pipeline().remove("inputStream");
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) throws Exception {
        ctx.fireExceptionCaught(throwable);
    }

    public VirtualSocket() {super();}

    @Override
    public void connect(SocketAddress endpoint) throws IOException {}

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {}

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {}

    @Override
    public InetAddress getInetAddress() {
        InetSocketAddress isa = getInetSocketAddress();

        if (isa == null) throw new VirtualSocketException();

        return getInetSocketAddress().getAddress();
    }

    @Override
    public InetAddress getLocalAddress() {return null;}

    @Override
    public int getPort() {
        return getInetSocketAddress().getPort();
    }

    private InetSocketAddress getInetSocketAddress() {
        return (InetSocketAddress)getRemoteSocketAddress();
    }

    @Override
    public int getLocalPort() {return -1;}

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return this.ctx.channel().remoteAddress();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return this.ctx.channel().localAddress();
    }

    @Override
    public SocketChannel getChannel() {return null;}

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {}

    @Override
    public boolean getTcpNoDelay() throws SocketException {return false;}

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {}

    @Override
    public int getSoLinger() throws SocketException {return -1;}

    @Override
    public void sendUrgentData(int data) throws IOException {}

    @Override
    public void setOOBInline(boolean on) throws SocketException {}

    @Override
    public boolean getOOBInline() throws SocketException {return false;}

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {}

    @Override
    public synchronized int getSoTimeout() throws SocketException {return -1;}

    @Override
    public synchronized void setSendBufferSize(int size) throws SocketException {}

    @Override
    public synchronized int getSendBufferSize() throws SocketException {return -1;}

    @Override
    public synchronized void setReceiveBufferSize(int size) throws SocketException {}

    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {return -1;}

    @Override
    public void setKeepAlive(boolean on) throws SocketException {}

    @Override
    public boolean getKeepAlive() throws SocketException {return false;}

    @Override
    public void setTrafficClass(int tc) throws SocketException {}

    @Override
    public int getTrafficClass() throws SocketException {return -1;}

    @Override
    public void setReuseAddress(boolean on) throws SocketException {}

    @Override
    public boolean getReuseAddress() throws SocketException {return false;}

    @Override
    public synchronized void close() throws IOException {}

    @Override
    public void shutdownInput() throws IOException {}

    @Override
    public void shutdownOutput() throws IOException {}

    @Override
    public String toString() {
        return "Virtual socket InetAdress["+getInetAddress()+"], Port["+getPort()+"]";
    }

    @Override
    public boolean isConnected() {return false;}

    @Override
    public boolean isBound() {return false;}

    @Override
    public boolean isClosed() {return false;}

    @Override
    public boolean isInputShutdown() {return false;}

    @Override
    public boolean isOutputShutdown() {return false;}

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {}
}
