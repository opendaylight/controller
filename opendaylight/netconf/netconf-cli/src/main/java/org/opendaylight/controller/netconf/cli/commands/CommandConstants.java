/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands;

import java.net.URI;

import org.opendaylight.controller.netconf.cli.io.IOUtil;
import org.opendaylight.yangtools.yang.common.QName;

public class CommandConstants {

    // Local command ids are defined here, this links the implementation to the rpc definition in yang
    // Better way needs to be found to provide this link instead of hardcoded QNames (e.g. yang extension)
    public static final QName HELP_QNAME = new QName(URI.create("netconf:cli"), IOUtil.parseDate("2014-05-22"), "help");
    public static final QName CLOSE_QNAME = new QName(URI.create("netconf:cli"), IOUtil.parseDate("2014-05-22"),
            "close");
    public static final QName INPUT_ARG_HANDLER_EXT_QNAME = new QName(
            URI.create("urn:ietf:params:xml:ns:netconf:base:1.0:cli"), IOUtil.parseDate("2014-05-26"),
            "argument-handler");
}
