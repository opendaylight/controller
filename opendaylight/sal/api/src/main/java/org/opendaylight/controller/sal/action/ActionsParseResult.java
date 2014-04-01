/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.sal.utils.Status;

/**
 * Represents the result of the parsing a List of Action objects expressed in
 * string form. In case some action strings are not parse-able, the result will
 * report the error with the offending strings as a Status object.
 */
public class ActionsParseResult {
    private Status status;
    private List<Action> list;

    public ActionsParseResult(List<Action> list, Status status) {
        this.list = list;
        this.status = status;
    }

    /**
     * Returns whether the parsing was successful for all the string
     *
     * @return true if all the strings were converted in Action objects
     */
    public boolean isSuccess() {
        return status.isSuccess();
    }

    /**
     * Returns the list of Action objects that were created by parsing the
     * action strings
     *
     * @return The list of successfully parsed Action objects
     */
    public List<Action> getActionlist() {
        return new ArrayList<Action>(list);
    }
}
