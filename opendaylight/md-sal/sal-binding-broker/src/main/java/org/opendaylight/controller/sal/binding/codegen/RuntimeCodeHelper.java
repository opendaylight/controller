/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen;

import com.google.common.base.Objects;
import java.lang.reflect.Field;
import java.util.Map;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeSpecification;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

@SuppressWarnings("all")
public class RuntimeCodeHelper {
  /**
   * Helper method to return delegate from ManagedDirectedProxy with use of reflection.
   *
   * Note: This method uses reflection, but access to delegate field should be
   * avoided and called only if neccessary.
   */
  public static <T extends RpcService> T getDelegate(final RpcService proxy) {
    try {
      Class<? extends RpcService> _class = proxy.getClass();
      final Field field = _class.getField(RuntimeCodeSpecification.DELEGATE_FIELD);
      boolean _equals = Objects.equal(field, null);
      if (_equals) {
        UnsupportedOperationException _unsupportedOperationException = new UnsupportedOperationException("Unable to get delegate from proxy");
        throw _unsupportedOperationException;
      }
      try {
        Object _get = field.get(proxy);
        return ((T) _get);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    } catch (Throwable _e_1) {
      throw Exceptions.sneakyThrow(_e_1);
    }
  }

  /**
   * Helper method to set delegate to ManagedDirectedProxy with use of reflection.
   *
   * Note: This method uses reflection, but setting delegate field should not occur too much
   * to introduce any significant performance hits.
   */
  public static void setDelegate(final RpcService proxy, final RpcService delegate) {
    try {
      Class<? extends RpcService> _class = proxy.getClass();
      final Field field = _class.getField(RuntimeCodeSpecification.DELEGATE_FIELD);
      boolean _equals = Objects.equal(field, null);
      if (_equals) {
        UnsupportedOperationException _unsupportedOperationException = new UnsupportedOperationException("Unable to set delegate to proxy");
        throw _unsupportedOperationException;
      }
      boolean _or = false;
      boolean _equals_1 = Objects.equal(delegate, null);
      if (_equals_1) {
        _or = true;
      } else {
        Class<? extends Object> _type = field.getType();
        Class<? extends RpcService> _class_1 = delegate.getClass();
        boolean _isAssignableFrom = _type.isAssignableFrom(_class_1);
        _or = (_equals_1 || _isAssignableFrom);
      }
      if (_or) {
        field.set(proxy, delegate);
      } else {
        IllegalArgumentException _illegalArgumentException = new IllegalArgumentException("delegate class is not assignable to proxy");
        throw _illegalArgumentException;
      }
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  /**
   * Helper method to set delegate to ManagedDirectedProxy with use of reflection.
   *
   * Note: This method uses reflection, but setting delegate field should not occur too much
   * to introduce any significant performance hits.
   */
  public static void setDelegate(final Object proxy, final Object delegate) {
    try {
      Class<? extends Object> _class = proxy.getClass();
      final Field field = _class.getField(RuntimeCodeSpecification.DELEGATE_FIELD);
      boolean _equals = Objects.equal(field, null);
      if (_equals) {
        UnsupportedOperationException _unsupportedOperationException = new UnsupportedOperationException("Unable to set delegate to proxy");
        throw _unsupportedOperationException;
      }
      boolean _or = false;
      boolean _equals_1 = Objects.equal(delegate, null);
      if (_equals_1) {
        _or = true;
      } else {
        Class<? extends Object> _type = field.getType();
        Class<? extends Object> _class_1 = delegate.getClass();
        boolean _isAssignableFrom = _type.isAssignableFrom(_class_1);
        _or = (_equals_1 || _isAssignableFrom);
      }
      if (_or) {
        field.set(proxy, delegate);
      } else {
        IllegalArgumentException _illegalArgumentException = new IllegalArgumentException("delegate class is not assignable to proxy");
        throw _illegalArgumentException;
      }
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  public static Map<InstanceIdentifier<? extends Object>,? extends RpcService> getRoutingTable(final RpcService target, final Class<? extends BaseIdentity> tableClass) {
    try {
      Class<? extends RpcService> _class = target.getClass();
      String _routingTableField = RuntimeCodeSpecification.getRoutingTableField(tableClass);
      final Field field = _class.getField(_routingTableField);
      boolean _equals = Objects.equal(field, null);
      if (_equals) {
        UnsupportedOperationException _unsupportedOperationException = new UnsupportedOperationException(
          "Unable to get routing table. Table field does not exists");
        throw _unsupportedOperationException;
      }
      try {
        Object _get = field.get(target);
        return ((Map<InstanceIdentifier<? extends Object>,? extends RpcService>) _get);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    } catch (Throwable _e_1) {
      throw Exceptions.sneakyThrow(_e_1);
    }
  }

  public static void setRoutingTable(final RpcService target, final Class<? extends BaseIdentity> tableClass, final Map<InstanceIdentifier<? extends Object>,? extends RpcService> routingTable) {
    try {
      Class<? extends RpcService> _class = target.getClass();
      String _routingTableField = RuntimeCodeSpecification.getRoutingTableField(tableClass);
      final Field field = _class.getField(_routingTableField);
      boolean _equals = Objects.equal(field, null);
      if (_equals) {
        UnsupportedOperationException _unsupportedOperationException = new UnsupportedOperationException(
          "Unable to set routing table. Table field does not exists");
        throw _unsupportedOperationException;
      }
      field.set(target, routingTable);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}