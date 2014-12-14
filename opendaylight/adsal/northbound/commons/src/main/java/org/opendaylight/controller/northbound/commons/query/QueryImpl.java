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
    private static final boolean ALLOW_OBJECT_STRING_COMPARE = true;

    private final Expression expression;
    private final TypeInfo rootType ;
    /**
     * Set the expression and cache
     * @param type
     * @param expression
     */
    public QueryImpl(Class<T> type, Expression expression) {
        this.expression = expression;
        this.rootType = TypeInfo.createRoot(null, type);
    }

    @Override
    public List<T> find(Collection<T> collection) throws QueryException {
        // new arraylist for result
        List<T> result = new ArrayList<T>();
        for (T item : collection) {
            if (match(item, rootType)) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public int filter(Collection<T> collection) throws QueryException {
        // find items
        List<T> matched = new ArrayList<T>();
        for (T item : collection) {
            if (match(item, rootType)) {
                matched.add(item);
            }
        }
        collection.clear();
        collection.addAll(matched);
        return matched.size();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public int filter(T rootObject, Class<?> childClass) throws QueryException {
        // retrieve underlying collection
        TypeInfo childType = rootType.getCollectionChild(childClass);
        if (childType == null || !(childType instanceof IteratableTypeInfo)) {
            return 0;
        }
        Collection collection = (Collection)
                childType.getAccessor().getValue(rootObject);
        // get the child type of the collection type
        TypeInfo ti = childType.getCollectionChild(childClass);
        List matched = new ArrayList();
        for (Object item : collection) {
            if (match(item, ti)) {
                matched.add(item);
            }
        }
        collection.clear();
        collection.addAll(matched);
        return matched.size();
    }

    private boolean match(final Object object, final TypeInfo rootType)
            throws QueryException {
        return expression.accept(new Visitor () {
            @Override
            public boolean visit(LogicalExpression le) throws QueryException {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Logical exp {}|{}|{}", le.getOperator(), le.getFirst(),
                            le.getSecond());
                }
                return (le.getOperator() == LogicalExpression.OP.AND) ?
                        le.getFirst().accept(this) && le.getSecond().accept(this) :
                            le.getFirst().accept(this) || le.getSecond().accept(this);
            }

            @Override
            public boolean visit(CompareExpression ce) throws QueryException {
                boolean result = visitInternal(ce);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("=== Compare exp {}|{}|{} == {}", ce.getOperator(),
                            ce.getSelector(), ce.getArgument(), result);
                }
                return result;
            }

            public boolean visitInternal(CompareExpression ce) throws QueryException {
                String[] selector = ce.getSelector().split("\\.");
                if (!rootType.getName().equals(selector[0])) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Root name mismatch: {} != {}",
                                rootType.getName(), selector[0]);
                    }
                    return false;
                }
                Object value = rootType.retrieve(object, selector, 1);
                if(value == null){ // nothing to compare against
                    return false;
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Comparing [{}] {} [{}]", ce.getArgument(),
                            ce.getOperator(), value.toString());
                }
                if (value instanceof Collection) {
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
                        Object item = it.next();
                        if (compare(parse(ce.getArgument(), item), item, operator)) {
                            // if match found check the operator and return false for NE
                            return (ce.getOperator() != OP.NE);
                        }
                    }
                    // return true for NE and false for rest
                    return (ce.getOperator() == OP.NE);
                } else {
                    return compare(parse(ce.getArgument(), value), value,
                            ce.getOperator());
                }
            }

        });
    }

    private boolean compare(Object valueToMatch, Object actualValue, OP operator) {
        if (valueToMatch == null || actualValue == null) {
            return false;
        }
        if (ALLOW_OBJECT_STRING_COMPARE && (valueToMatch instanceof String)
                && !(actualValue instanceof String)) {
            actualValue = actualValue.toString();
        }

        int compareResult = -1;
        if (valueToMatch instanceof Comparable) {
            compareResult = ((Comparable)actualValue).compareTo(valueToMatch);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Not a comparable type: {} {}",
                        valueToMatch.getClass().getName(),
                        actualValue.getClass().getName());
            }
            return false;
        }
        switch(operator) {
            case EQ :
                return compareResult == 0;
            case RE :
                // Regex match,
                if (valueToMatch instanceof String) {
                    return Pattern.matches((String)valueToMatch, actualValue.toString());
                } else {
                    return compareResult == 0;
                }
            case NE:
                return compareResult != 0;
            case GT :
                return compareResult > 0;
            case GE :
                return compareResult >= 0;
            case LT :
                return compareResult < 0;
            case LE :
                return compareResult <= 0;
            default:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Unrecognized comparator - {}", operator);
                }
            return false;
        }
    }
    private Object parse(String arg, Object value) {
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof String) {
                return arg;
            } else if (value instanceof Byte) {
                return Byte.decode(arg);
            } else if (value instanceof Double) {
                return Double.parseDouble(arg);
            } else if (value instanceof Float) {
                return Float.parseFloat(arg);
            } else if (value instanceof Integer) {
                return Integer.parseInt(arg);
            } else if (value instanceof Long) {
                return Long.parseLong(arg);
            } else if (value instanceof Short) {
                return Short.parseShort(arg);
            }
        } catch (NumberFormatException ignore) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception parsing {}", arg, value);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Using string comparision for type - {}",
                    value.getClass().getName());
        }
        // Not a number or string. Convert to a string and compare as last resort
        return ALLOW_OBJECT_STRING_COMPARE ? arg.toString() : null;
    }

}
