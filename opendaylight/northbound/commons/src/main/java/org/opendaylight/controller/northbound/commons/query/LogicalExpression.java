/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

/*package*/ class LogicalExpression implements Expression {

    public static enum OP { AND, OR }

    private final OP _op;
    private final Expression _arg1;
    private final Expression _arg2;

    public LogicalExpression(OP op, Expression first, Expression second) {
        _op = op;
        _arg1 = first;
        _arg2 = second;
    }

    public OP getOperator() {
        return _op;
    }

    public Expression getFirst() {
        return _arg1;
    }

    public Expression getSecond() {
        return _arg2;
    }

    @Override
    public boolean accept(Visitor visitor) throws QueryException {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(_arg1.toString())
        .append(_op.toString())
        .append(_arg2.toString());
        return sb.toString();
    }

}
