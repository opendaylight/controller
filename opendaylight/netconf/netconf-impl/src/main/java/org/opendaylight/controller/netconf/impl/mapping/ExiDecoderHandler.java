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
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import org.opendaylight.controller.netconf.util.xml.ExiParameters;
import org.opendaylight.controller.netconf.util.xml.ExiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExiDecoderHandler extends ByteToMessageDecoder {

    public static final String HANDLER_NAME;

    static {
        HANDLER_NAME = "exiDecoder";
    }

    private final static Logger logger = LoggerFactory
            .getLogger(ExiDecoderHandler.class);

    private ExiParameters parameters;

    public ExiDecoderHandler(ExiParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in,
            List<Object> out) {
        try {
            ExiUtil.decode(in, out, this.parameters);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to decode exi message.");
        }
    }

}
