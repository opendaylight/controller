/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 */
/*package*/ class QueryImpl<T> implements Query<T> {
  public static final Logger LOGGER = LoggerFactory.getLogger(QueryImpl.class);

  private final Expression expression;
  private final TypeInfo rootType ;
  /**
   * Set the expression and cache
   * @param type
   * @param expression
   */
  public QueryImpl(Class<T> type, Expression expression) {
    this.expression = expression;
    this.rootType = TypeInfo.createRoot(type.getName(),type);
  }

  @Override
  public List<T> filter(List<T> resourceList) {
    // new arraylist for result
    List<T> result = new ArrayList<T>();
    for (T resource : resourceList) {
      if (isFound(resource)) {
        result.add(resource);
      }
    }
    return result;
  }

  private boolean isFound(final T object) {
    return expression.accept(new Visitor () {
      @Override
      public boolean visit(LogicalExpression le) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Logical exp {}|{}|{}", le.getOperator(), le.getFirst(),
              le.getSecond());
        }
        return (le.getOperator() == LogicalExpression.OP.AND) ?
            le.getFirst().accept(this) && le.getSecond().accept(this) :
            le.getFirst().accept(this) || le.getSecond().accept(this);
      }

      @Override
      public boolean visit(CompareExpression ce) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("=== Compare exp {}|{}|{} ", ce.getOperator(),
              ce.getSelector(), ce.getArgument());
        }
        try {
          Object value = rootType.retrieve(object,
              ce.getSelector().split("\\."), 1);
          if (value == null) {
            return false;
          }

          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Comparing [{}] {} [{}]", ce.getArgument(),
                ce.getOperator(), value.toString());
          }
          if (value instanceof String) {
            switch(ce.getOperator()) {
              case EQ :
                return ce.getArgument().equals(value.toString());
              case RE :
                return Pattern.matches(ce.getArgument(), value.toString());
              case NE:
                return !ce.getArgument().equals(value.toString());
              case GT :
                return ce.getArgument().compareTo(value.toString()) > 0;
              case GE :
                return ce.getArgument().compareTo(value.toString()) >= 0;
              case LT :
                return ce.getArgument().compareTo(value.toString()) < 0;
              case LE :
                return ce.getArgument().compareTo(value.toString()) <= 0;
              default:
                if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug("Unrecognized comparator - {}", ce.getOperator());
                }
                return false;
            }
          } else if (value instanceof Integer) {
            // assume its a #
            int valToMatch = Integer.parseInt(ce.getArgument());
            int actualValue = (Integer)value;
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Comparing [{}] {} [{}]", ce.getArgument(),
                  ce.getOperator(), value.toString());
            }
            switch(ce.getOperator()) {
              case EQ :
              case RE :
                return actualValue == valToMatch;
              case NE :
                return actualValue != valToMatch;
              case GT :
                return actualValue > valToMatch;
              case GE :
                return actualValue >= valToMatch;
              case LT :
                return actualValue < valToMatch;
              case LE :
                return actualValue <= valToMatch;
              default:
                if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug("Unrecognized comparator - {}", ce.getOperator());
                }
                return false;
            }
          } else {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Unrecognized value type - {}", value.getClass().getName());
            }
            return false;
          }
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      }
    });
  }

}
