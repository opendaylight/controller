/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 */
public class QueryImpl<T> implements Query<T> {

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
                System.out.println("=== LE " + le.getOperator() +
                        "|" + le.getFirst() + "|" + le.getSecond());
                return (le.getOperator() == LogicalExpression.OP.AND) ?
                        le.getFirst().accept(this) && le.getSecond().accept(this) :
                            le.getFirst().accept(this) || le.getSecond().accept(this);
            }

            @Override
            public boolean visit(CompareExpression ce) {
                System.out.println("=== CE " + ce.getOperator() +
                        "|" + ce.getSelector() + "|" + ce.getArgument());
                try {
                    //TODO null check on expression
                    // check if the selector matches any of the fields
                    Object value = rootType.retrieve(object,
                            ce.getSelector().split("\\."), 1);
                    if (value instanceof String) {
                        System.out.println("Comparing [" + ce.getArgument() + "] "+ ce.getOperator() + " [" + value.toString() + "]");
                        switch(ce.getOperator()) {
                        case EQ :
                            return ce.getArgument().equals(value.toString());
                        case RE :
                            return Pattern.matches(ce.getArgument(), value.toString());
                        case NE:
                            return !ce.getArgument().equals(value.toString());
                        default:
                            System.out.println("Comparator : " + ce.getOperator()
                                    + " cannot apply to Strings");
                            return false;
                        }
                    } else {
                        // assume its a #
                        int valToMatch = Integer.parseInt(ce.getArgument());
                        int actualValue = (Integer)value;
                        System.out.println("Comparing: " + valToMatch + " " + ce.getOperator() + " " + actualValue);
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
                            System.out.println("Unrecognized compare operator: " + ce.getOperator());
                            return false;
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

}
