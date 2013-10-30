/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl.mapping;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import org.opendaylight.controller.netconf.util.xml.ExiParameters;
import org.opendaylight.controller.netconf.util.xml.ExiUtil;

public class ExiEncoderHandler extends MessageToByteEncoder<Object> {

    public static final String HANDLER_NAME;
    static {
        HANDLER_NAME = "exiEncoder";
    }

    private ExiParameters parameters;

    public ExiEncoderHandler(ExiParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out)
            throws Exception {
        try {
            ExiUtil.encode(msg, out, this.parameters);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode exi message.");
        }
    }
}
