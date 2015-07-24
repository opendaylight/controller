/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml;

import java.util.Arrays;

public enum TestOption {
    testOnly, set, testThenSet;

    public static TestOption getFromXmlName(String testOptionXmlName) {
        switch (testOptionXmlName) {
        case "test-only":
            return testOnly;
        case "test-then-set":
            return testThenSet;
        case "set":
            return set;
        default:
            throw new IllegalArgumentException("Unsupported test option " + testOptionXmlName + " supported: "
                    + Arrays.toString(TestOption.values()));
        }
    }

    public static TestOption getDefault() {
        return testThenSet;
    }

}
