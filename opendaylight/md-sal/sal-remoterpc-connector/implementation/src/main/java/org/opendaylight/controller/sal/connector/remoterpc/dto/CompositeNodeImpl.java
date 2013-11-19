/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc.dto;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CompositeNodeImpl implements CompositeNode, Serializable {

  private QName key;
  private List<Node<?>> children;

  @Override
  public List<Node<?>> getChildren() {
    return children;
  }

  @Override
  public List<CompositeNode> getCompositesByName(QName children) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CompositeNode> getCompositesByName(String children) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public List<SimpleNode<?>> getSimpleNodesByName(QName children) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public List<SimpleNode<?>> getSimpleNodesByName(String children) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public CompositeNode getFirstCompositeByName(QName container) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public SimpleNode<?> getFirstSimpleByName(QName leaf) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public MutableCompositeNode asMutable() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public QName getKey() {
    return key;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public List<Node<?>> setValue(List<Node<?>> value) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public int size() {
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean isEmpty() {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean containsKey(Object key) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean containsValue(Object value) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public List<Node<?>> get(Object key) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public List<Node<?>> put(QName key, List<Node<?>> value) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public List<Node<?>> remove(Object key) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void putAll(Map<? extends QName, ? extends List<Node<?>>> m) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void clear() {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Set<QName> keySet() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Collection<List<Node<?>>> values() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Set<Entry<QName, List<Node<?>>>> entrySet() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public QName getNodeType() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public CompositeNode getParent() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public List<Node<?>> getValue() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ModifyAction getModificationAction() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}
