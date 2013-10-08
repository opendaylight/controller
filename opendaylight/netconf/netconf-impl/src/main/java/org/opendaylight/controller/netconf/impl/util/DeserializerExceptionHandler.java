/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.util;

import java.util.Map;

import org.opendaylight.controller.netconf.api.NetconfDeserializerException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.util.messages.SendErrorExceptionUtil;

import com.google.common.collect.Maps;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public final class DeserializerExceptionHandler implements ChannelHandler {

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // NOOP
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // NOOP
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof NetconfDeserializerException) {
            handleDeserializerException(ctx, cause);
        }
    }

    private void handleDeserializerException(ChannelHandlerContext ctx, Throwable cause) {

        Map<String, String> info = Maps.newHashMap();
        info.put("cause", cause.getMessage());
        NetconfDocumentedException ex = new NetconfDocumentedException(cause.getMessage(),
                NetconfDocumentedException.ErrorType.rpc, NetconfDocumentedException.ErrorTag.malformed_message,
                NetconfDocumentedException.ErrorSeverity.error, info);

        SendErrorExceptionUtil.sendErrorMessage(ctx.channel(), ex);
    }
}
