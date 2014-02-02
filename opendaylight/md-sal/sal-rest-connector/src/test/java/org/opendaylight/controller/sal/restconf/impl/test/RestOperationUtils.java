/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import com.google.common.base.Charsets;

public class RestOperationUtils {

    public static final String JSON = "+json";
    public static final String XML = "+xml";

    private RestOperationUtils() {
    }

    public static String createUri(String prefix, String encodedPart) throws UnsupportedEncodingException {
        return URI.create(prefix + URLEncoder.encode(encodedPart, Charsets.US_ASCII.name()).toString()).toASCIIString();
    }
}
