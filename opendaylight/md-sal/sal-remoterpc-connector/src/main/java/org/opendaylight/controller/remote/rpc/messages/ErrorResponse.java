/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import org.opendaylight.controller.sal.common.util.RpcErrors;
import org.opendaylight.yangtools.yang.common.RpcError;

import java.util.ArrayList;
import java.util.Collection;

public class ErrorResponse {

  Exception exception;
  Collection<RpcError> errors = new ArrayList<>();

  public ErrorResponse(Exception e) {
    this.exception = e;
    errors.add(RpcErrors.getRpcError(null, null, null, null, e.getMessage(), null, e.getCause()));
  }

  public Collection<RpcError> getErrors() {
    return errors;
  }
}
