/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.opendaylight.controller.northbound.commons.query.CompareExpression.OP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
          if(value == null){ // nothing to compare against
            return false;
          }
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Comparing [{}] {} [{}]", ce.getArgument(),
                ce.getOperator(), value.toString());
          }
          if (value instanceof Collection){
            Collection<?> collection = (Collection<?>) value;
            if(collection.size() == 0 && ce.getOperator() == OP.NE) {
              // collection doesn't contain query string
              return true;
            }
            // If there are elements iterate
            Iterator<?> it = collection.iterator();
            OP operator = ce.getOperator();
            if (operator == OP.NE) {
              // negate the operator
              operator = OP.EQ;
            }
            while (it.hasNext()) {
              if (compareObj(ce.getArgument(), it.next(), operator)) {
                // if match found check the operator and return false for NE
                return (ce.getOperator() != OP.NE);
              }
            }
            // return true for NE and false for rest
            return (ce.getOperator() == OP.NE);
          }
          else
            return compareObj(ce.getArgument(), value, ce.getOperator());
        } catch (Exception e) {
          e.printStackTrace();
          throw new IllegalStateException(e);
        }
      }
    });
  }

  private boolean compareObj(String valueToMatch, Object actualValue, OP operator){
    if (actualValue != null) {
      if (actualValue instanceof String) {
        return compare(valueToMatch,actualValue.toString(),operator);
      } else if (actualValue instanceof Integer) {
        // assume its a #
        int valToMatch = Integer.parseInt(valueToMatch);
        int actualVal = (Integer)actualValue;
        return compare(valToMatch, actualVal, operator);
      } else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Unrecognized value type - {}", actualValue.getClass().getName());
        }
        return false;
      }
    }
    return false;
  }

  private boolean compare(String valueToMatch, String actualValue, OP operator){
    switch(operator) {
    case EQ :
      return valueToMatch.equals(actualValue);
    case RE :
      return Pattern.matches(valueToMatch, actualValue);
    case NE:
      return !valueToMatch.equals(actualValue);
    case GT :
      return valueToMatch.compareTo(actualValue) > 0;
    case GE :
      return valueToMatch.compareTo(actualValue) >= 0;
    case LT :
      return valueToMatch.compareTo(actualValue) < 0;
    case LE :
      return valueToMatch.compareTo(actualValue) <= 0;
    default:
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Unrecognized comparator - {}", operator);
      }
      return false;
    }
  }

  private boolean compare(int valueToMatch, int actualValue, OP operator){
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Comparing [{}] {} [{}]", valueToMatch,
          operator, actualValue);
    }
    switch(operator) {
    case EQ :
    case RE :
      return actualValue == valueToMatch;
    case NE :
      return actualValue != valueToMatch;
    case GT :
      return actualValue > valueToMatch;
    case GE :
      return actualValue >= valueToMatch;
    case LT :
      return actualValue < valueToMatch;
    case LE :
      return actualValue <= valueToMatch;
    default:
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Unrecognized comparator - {}", operator);
      }
      return false;
    }
  }
}
