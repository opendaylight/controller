/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.impl;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.yang.store.api.YangStoreService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.regex.Pattern;

public class YangStoreActivator implements BundleActivator {
    private static final Logger logger = LoggerFactory.getLogger(YangStoreActivator.class);

    @Override
    public void start(BundleContext context) throws Exception {
        // get blacklist
        Optional<Pattern> maybeBlacklistPattern = Optional.absent();
        String blacklist = context.getProperty("yangstore.blacklist");
        if (blacklist != null) {
            try {
                maybeBlacklistPattern = Optional.of(Pattern.compile(blacklist));
            } catch (RuntimeException e) {
                logger.error("Cannot parse blacklist regex " + blacklist, e);
                throw e;
            }
        }
        ExtenderYangTracker extenderYangTracker = new ExtenderYangTracker(maybeBlacklistPattern, context);
        Dictionary<String, ?> properties = new Hashtable<>();
        context.registerService(YangStoreService.class, extenderYangTracker, properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }
}
