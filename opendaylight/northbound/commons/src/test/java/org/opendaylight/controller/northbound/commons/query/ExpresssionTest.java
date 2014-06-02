/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.northbound.commons.query.CompareExpression.OP;

public class ExpresssionTest {

    protected static final List<PersonBean> people = new ArrayList<PersonBean>();
    protected static final List<BookBean> books = new ArrayList<BookBean>();

    public static boolean matches(String query, final PersonBean person) throws Exception {
        System.out.println("PARSING query: " + query);
        FiqlParser parser = new FiqlParser(new java.io.StringReader(query));
        Expression exp = parser.START();
        System.out.println(exp);

        boolean result = exp.accept(new Visitor () {
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
                if (person == null) {
                    return false;
                }
                try {
                    // check if the selector matches any of the fields
                    Field field = PersonBean.class.getDeclaredField(ce.getSelector());
                    if (field == null) {
                        System.out.println("No field found by name : " + ce.getSelector());
                        return false;
                    }
                    Object value = field.get(person);
                    if (value instanceof String) {
                        System.out.println("Comparing [" + ce.getArgument() + "] "+ ce.getOperator() + " [" + value.toString() + "]");
                        if (ce.getOperator() == OP.EQ) {
                            return ce.getArgument().equals(value.toString());
                        } else if (ce.getOperator() == OP.RE) {
                            return Pattern.matches(ce.getArgument(), value.toString());
                        } else if (ce.getOperator() == OP.NE) {
                            return !ce.getArgument().equals(value.toString());
                        } else {
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
        System.out.println("RESULT: " + result);
        return result;
    }

    @BeforeClass
    public static void load() {
        people.add(new PersonBean(100, "John", "Doe", "San Jose"));
        people.add(new PersonBean(200, "Foo", "Bar", "San Francisco"));
        people.add(new PersonBean(300, "A", "B", "San Francisco"));
        people.add(new PersonBean(400, "X", "Y", "New York"));

        books.add(new BookBean("Book1", "A001", people.get(0)));
        books.add(new BookBean("Book2", "A002", people.get(1)));
        books.add(new BookBean("Book3", "A003", people.get(2)));

        ReviewBean review1 = new ReviewBean("cool", people.get(2));
        ReviewBean review2 = new ReviewBean("kewl", people.get(3));
        books.get(0).addReview(review1).addReview(review2);
        books.get(1).addReview(review1).addReview(review2);
        books.get(2).addReview(review1).addReview(review2);
    }

    @Test
    public void testCXFQueries() throws Exception {
        // following queries copied from apache cxf
        Assert.assertFalse(matches("id=gt=100;name=Fred", null));
        Assert.assertFalse(matches("id=gt=100;name==Fred", null));
        Assert.assertFalse(matches("id=lt=123", null));
        Assert.assertFalse(matches("date=le=2010-03-11", null));
        Assert.assertFalse(matches("time=le=2010-03-11T18:00:00", null));
        Assert.assertFalse(matches("name==CXF;version=ge=2.2", null));
        Assert.assertFalse(matches("(age=lt=25,age=gt=35);city==London", null));
        Assert.assertFalse(matches("date=lt=2000-01-01;date=gt=1999-01-01;(sub==math,sub==physics)", null));
    }

    public int find(String query) throws Exception {
        int found = 0;
        for (PersonBean person : people) {
            if (matches(query, person)) found++;
        }
        return found;
    }

    @Test
    public void testPeopleQueries() throws Exception {
        Assert.assertTrue(find("id==200") == 1);
        Assert.assertTrue(find("id!=100;(city='San.*')") == 2);
        Assert.assertTrue(find("id>200;(city='San.*')") == 1);
        Assert.assertTrue(find("city='San.*'") == 3);
    }

    @Test
    public void testTypeTree() throws Exception {
        TypeInfo bookType = TypeInfo.createRoot("book",
                org.opendaylight.controller.northbound.commons.query.BookBean.class);
        System.out.println(bookType.retrieve(books.get(0),
                "book.author.firstName".split("\\."), 1));
        System.out.println(bookType.retrieve(books.get(0),
                "book.reviews.comment".split("\\."), 1));
    }
}
