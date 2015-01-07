/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.store;

import java.io.Serializable;

import org.opendaylight.datasand.codec.AttributeDescriptor;
import org.opendaylight.datasand.codec.EncodeDataContainer;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class Criteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String operators[] = new String[] {" and ", " or ", ">=", "<=", "!=", "=", ">", "<", "like","is null", "not null", "skip"};
    private static final String STRING_CHAR = "'";

    public static final int OP_CODE_AND = 0;
    public static final int OP_CODE_OR = 1;
    public static final int OP_CODE_GTEQ = 2;
    public static final int OP_CODE_LTEQ = 3;
    public static final int OP_CODE_NOT_EQ = 4;
    public static final int OP_CODE_EQUAL = 5;
    public static final int OP_CODE_GT = 6;
    public static final int OP_CODE_LT = 7;
    public static final int OP_CODE_LIKE = 8;
    public static final int OP_CODE_NULL = 9;
    public static final int OP_CODE_NOT_NULL = 10;
    public static final int OP_CODE_SKIP = 11;

    private Criteria left = null;
    private Criteria right = null;

    private int operation = -1;

    private Object leftValue = null;
    private Object rightValue = null;
    private String criteria = null;

    private Criteria(){
    }

    public Criteria(final String data, final int parentOperation) {
        criteria = data;
        parse(data, parentOperation);
    }

    public static void encode(Object value,EncodeDataContainer edc){
        Criteria c = (Criteria)value;
        edc.getEncoder().encodeInt16(c.operation, edc);
        edc.getEncoder().encodeString(c.criteria, edc);
        edc.getEncoder().encodeObject(c.leftValue, edc);
        edc.getEncoder().encodeObject(c.rightValue, edc);
        if(c.left!=null){
            encode(c.left,edc);
        }else{
            edc.getEncoder().encodeNULL(edc);
        }
        if(c.right!=null){
            encode(c.right,edc);
        }else{
            edc.getEncoder().encodeNULL(edc);
        }
    }

    public static Criteria decode(EncodeDataContainer edc){
        if(edc.getEncoder().isNULL(edc)) return null;
        Criteria c = new Criteria();
        c.operation = edc.getEncoder().decodeInt16(edc);
        c.criteria = edc.getEncoder().decodeString(edc);
        c.leftValue = edc.getEncoder().decodeObject(edc);
        c.rightValue = edc.getEncoder().decodeObject(edc);
        c.left = (Criteria)decode(edc);
        c.right = (Criteria)decode(edc);
        return c;
    }

    private void parse(String data, int parentOperation) {

        data = data.trim();

        int index1 = data.indexOf('(');
        if (index1 != -1) {
            String leftCondition = data.substring(0, index1).trim();
            if (leftCondition.trim().equals("")) {
                int index2 = data.lastIndexOf(')');
                if (index2 < data.length() - 1) {
                    String rtValue = data.substring(index2 + 1).trim();
                    if (!rtValue.equals("")) {
                        left =
                            new Criteria(data.substring(index1 + 1, index2),
                                parentOperation);
                        data = data.substring(index2 + 1);
                    } else {
                        data = data.substring(1, index2);
                    }
                } else {
                    data = data.substring(1, index2);
                }
            } else {
                right = new Criteria(
                    data.substring(index1 + 1, data.length() - 1),
                    parentOperation);
                data = data.substring(0, index1);
            }
        }

        for (int i = 0; i < operators.length; i++) {
            index1 = data.indexOf(operators[i]);
            if (index1 != -1) {
                this.operation = i;
                if (left == null) {
                    left = new Criteria(data.substring(0, index1),this.operation);
                }
                if (left.leftValue != null && left.rightValue == null && left.right == null) {
                    leftValue = left.leftValue;
                    left = null;
                }
                if (right == null) {
                    right = new Criteria(data.substring(index1 + operators[i].length()),this.operation);
                }
                if (right.leftValue != null && right.rightValue == null
                    && right.right == null) {
                    rightValue = right.leftValue;
                    right = null;
                }
                return;
            }
        }

        if (data.startsWith(STRING_CHAR) && data.endsWith(STRING_CHAR)) {
            data = data.substring(1, data.length() - 1);
        }

        if (parentOperation == OP_CODE_LIKE && data.startsWith("%") && data.endsWith("%")
            && data.substring(1, data.length() - 1).indexOf("%") == -1) {
            data = data.substring(1, data.length() - 1);
        }

        leftValue = data;
    }

    public int checkValue(Object value) {
        if (leftValue != null && rightValue != null) {
            Object aSide = null;
            Object bSide = null;
            if (leftValue.equals("?")) {
                aSide = value;
            } else {
                aSide = leftValue;
            }

            if (rightValue.equals("?")) {
                bSide = value;
            } else {
                bSide = rightValue;
            }
            if (operation != OP_CODE_SKIP && operation != OP_CODE_NULL) {
                if (aSide == null && bSide != null) {
                    return 0;
                } else if (aSide != null && bSide == null) {
                    return 0;
                } else if (aSide == null && bSide == null) {
                    return 1;
                }
            }
            switch (operation) {
                case OP_CODE_EQUAL:
                    if (aSide.equals(bSide)) {
                        return 1;
                    } else {
                        return 0;
                    }
                case OP_CODE_NOT_EQ:
                    if (!aSide.equals(bSide)) {
                        return 1;
                    } else {
                        return 0;
                    }
                case OP_CODE_LIKE:
                    if (aSide.toString().indexOf(bSide.toString()) != -1) {
                        return 1;
                    } else {
                        return 0;
                    }
                case OP_CODE_NULL:
                    if (aSide == null) {
                        return 1;
                    } else {
                        return 0;
                    }
                case OP_CODE_NOT_NULL:
                    if (aSide != null) {
                        return 1;
                    } else {
                        return 0;
                    }
                case OP_CODE_GT:
                    if (aSide == null || bSide == null) {
                        return 0;
                    }
                    if (Double.parseDouble(aSide.toString().trim()) > Double
                        .parseDouble(bSide.toString().trim())) {
                        return 1;
                    } else {
                        return 0;
                    }
                case OP_CODE_GTEQ:
                    if (aSide == null || bSide == null) {
                        return 0;
                    }
                    if (Double.parseDouble(aSide.toString().trim()) >= Double
                        .parseDouble(bSide.toString().trim())) {
                        return 1;
                    } else {
                        return 0;
                    }

                case OP_CODE_LT:
                    if (aSide == null || bSide == null) {
                        return 0;
                    }
                    if (Double.parseDouble(aSide.toString().trim()) < Double
                        .parseDouble(bSide.toString().trim())) {
                        return 1;
                    } else {
                        return 0;
                    }
                case OP_CODE_LTEQ:
                    if (aSide == null || bSide == null) {
                        return 0;
                    }
                    if (Double.parseDouble(aSide.toString().trim()) <= Double
                        .parseDouble(bSide.toString().trim())) {
                        return 1;
                    } else {
                        return 0;
                    }
            }
        }

        int leftResult = 0;
        if (left != null) {
            leftResult = left.checkValue(value);
        }

        int rightResult = 0;
        if (right != null) {
            rightResult = right.checkValue(value);
        }

        if (operation == OP_CODE_SKIP) {
            if (rightResult == 0) {
                return 2;
            } else {
                return 3;
            }
        }

        if (operation == OP_CODE_AND) {
            if (leftResult == 0) {
                return 0;
            }
            if (rightResult == 0) {
                return 0;
            }
            if (leftResult >= 2) {
                return leftResult;
            }
            if (rightResult >= 2) {
                return rightResult;
            }

            return 1;
        }

        if (operation == OP_CODE_OR) {
            if (leftResult == 0 && rightResult == 0) {
                return 0;
            }
            if (leftResult >= 2) {
                return leftResult;
            }
            if (rightResult >= 2) {
                return rightResult;
            }
            return 1;
        }

        return 0;
    }

    public String toString() {
        return criteria;
    }

    public String getCriteriaForProperty(AttributeDescriptor col) {
        StringBuffer result = new StringBuffer();
        if (criteria == null) {
            return "";
        }

        if (leftValue != null && rightValue != null) {
            if (leftValue.toString().toLowerCase().equals(col.getColumnName().toLowerCase()) ||
                leftValue.toString().toLowerCase().equals(col.toString().toLowerCase()) ||
                leftValue.toString().toLowerCase().equals(col.getTableName().toLowerCase()+"."+col.getColumnName().toLowerCase())) {
                result.append("? ").append(operators[operation]).append(" ").append(rightValue);
            }else
            if (rightValue.toString().toLowerCase().equals(col.getColumnName().toLowerCase()) ||
                    rightValue.toString().toLowerCase().equals(col.toString().toLowerCase()) ||
                    rightValue.toString().toLowerCase().equals(col.getTableName().toLowerCase()+"."+col.getColumnName().toLowerCase())) {
                    result.append("? ").append(operators[operation]).append(" ").append(leftValue);
            }
            return result.toString();
        } else if (left != null && right != null) {
            String leftString = left.getCriteriaForProperty(col);
            String rightString = right.getCriteriaForProperty(col);
            if (!leftString.equals("") && !rightString.equals("")) {
                return leftString + " " + operators[operation] + " "
                    + rightString;
            } else if (!leftString.equals("")) {
                return leftString;
            } else if (!rightString.equals("")) {
                return rightString;
            }
            return "";
        } else if (leftValue != null && leftValue.toString().toLowerCase()
            .equals(col.toString().toLowerCase()) && right != null) {
            return "? " + operators[operation] + " (" + right
                .getCriteriaForProperty(col) + ")";
        } else if (rightValue != null && rightValue.toString().toLowerCase()
            .equals(col.toString().toLowerCase()) && left != null) {
            return "(" + left.getCriteriaForProperty(col) + ") "
                + operators[operation] + " ?";
        }
        return "";
    }

    public static void main(String args[]) {
        Criteria p = new Criteria("ip like '%101%' or (354>=0 or 456>=3)", -1);
        System.out.println(p.checkValue("192.268.4.4"));
        p = new Criteria("? like '%267%'", -1);
        System.out.println(p.checkValue("192.268.4.4"));

    }
}

