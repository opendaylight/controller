/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.sal.binding.codegen;

import java.lang.reflect.Method;
import org.opendaylight.yangtools.yang.binding.Notification;

@SuppressWarnings("all")
public class YangtoolsMappingHelper {
  public static boolean isNotificationCallback(final Method it) {
      return it.getName().startsWith("on") && (it.getParameterTypes().length == 1) &&
              Notification.class.isAssignableFrom(it.getParameterTypes()[0]);
  }

}