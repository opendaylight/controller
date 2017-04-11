/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.config;

import com.google.common.base.Optional;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/3/24.
 *
 * @author Han Jie
 */
public class SubChannelConfigParser {
    private static final Logger LOG = LoggerFactory.getLogger(SubChannelConfigParser.class);

    public static SubChannelConfigParams parse(Optional<Config> config) {
        LOG.info("parse config {}",config);
        SubChannelConfigParams configParams = new DefaultSubChannelConfigParamsImpl();
        if (config.isPresent()) {
            if(config.get().hasPathOrNull(("message-reply-timeout"))) {
                configParams.setMessageTimeoutInSeconds(config.get().getLong("message-reply-timeout"));
            }

            if(config.get().hasPathOrNull(("serializer-receive-timeout"))) {
                configParams.setSerializerTimeoutInSeconds(config.get().getLong("serializer-receive-timeout"));
            }

            if(config.get().hasPathOrNull(("chunk-size"))) {
                configParams.setChunkSize(config.get().getInt("chunk-size"));
            }

            return configParams;
        }
        else
        {
            return new DefaultSubChannelConfigParamsImpl();
        }
    }
}
