/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

import java.util.Stack;

/*package*/ class ExpressionBuilder {
    private final Stack<Expression> _stack = new Stack<Expression>();
    private LogicalExpression.OP _lastOp = null;

    public ExpressionBuilder() {}

    public ExpressionBuilder withAnd() {
        _lastOp = LogicalExpression.OP.AND;
        return this;
    }

    public ExpressionBuilder withOr() {
        _lastOp = LogicalExpression.OP.OR;
        return this;
    }

    public ExpressionBuilder withTerm(Expression exp) {
        if (_lastOp != null) {
            exp = new LogicalExpression(_lastOp, _stack.pop(), exp);
            _lastOp = null;
        }
        _stack.push(exp);
        return this;
    }

    public Expression build() {
        return _stack.pop();
    }

}
