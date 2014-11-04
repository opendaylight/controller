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
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.northbound.commons.query.CompareExpression.OP;

public class ExpresssionTest {

    private static final List<PersonBean> people = new ArrayList<PersonBean>();
    private static final ArrayList<BookBean> books = new ArrayList<BookBean>();

    public static void p(String msg) {
        //System.out.println("======= " + msg);
    }

    public static boolean matches(Expression exp, final PersonBean person) throws Exception {

        boolean result = exp.accept(new Visitor() {
            @Override
            public boolean visit(LogicalExpression le) throws QueryException {
                p("=== LE " + le.getOperator() + "|" + le.getFirst() + "|" + le.getSecond());
                return (le.getOperator() == LogicalExpression.OP.AND) ?
                        le.getFirst().accept(this) && le.getSecond().accept(this) :
                            le.getFirst().accept(this) || le.getSecond().accept(this);
            }

            @Override
            public boolean visit(CompareExpression ce) {
                p("=== CE " + ce.getOperator() + "|" + ce.getSelector() + "|" + ce.getArgument());
                if (person == null) {
                    return false;
                }
                try {
                    // check if the selector matches any of the fields
                    Field field = PersonBean.class.getDeclaredField(ce.getSelector());
                    if (field == null) {
                        p("No field found by name : " + ce.getSelector());
                        return false;
                    }
                    Object value = field.get(person);
                    if (value instanceof String) {
                        p("Comparing [" + ce.getArgument() + "] "+ ce.getOperator() + " [" + value.toString() + "]");
                        if (ce.getOperator() == OP.EQ) {
                            return ce.getArgument().equals(value.toString());
                        } else if (ce.getOperator() == OP.RE) {
                            return Pattern.matches(ce.getArgument(), value.toString());
                        } else if (ce.getOperator() == OP.NE) {
                            return !ce.getArgument().equals(value.toString());
                        } else {
                            p("Comparator : " + ce.getOperator() + " cannot apply to Strings");
                            return false;
                        }
                    } else {
                        // assume its a #
                        int valToMatch = Integer.parseInt(ce.getArgument());
                        int actualValue = (Integer)value;
                        p("Comparing: " + valToMatch + " " + ce.getOperator() + " " + actualValue);
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
                            p("Unrecognized compare operator: " + ce.getOperator());
                            return false;
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        p("RESULT: " + result);
        return result;
    }

    @BeforeClass
    public static void load() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");

        people.add(new PersonBean(100, "John", "Doe", "San Jose"));
        people.add(new PersonBean(200, "Foo", "Bar", "San Francisco"));
        people.add(new PersonBean(300, "A", "B", "San Francisco"));
        people.add(new PersonBean(400, "X", "Y", "New York"));

        books.add(new BookBean("Book1", "A001", people.get(0)));
        books.add(new BookBean("Book2", "A002", people.get(1)));
        books.add(new BookBean("Book3", "A003", people.get(2)));

        ReviewBean review1 = new ReviewBean("cool", people.get(2));
        ReviewBean review2 = new ReviewBean("kewl", people.get(3));
        ReviewBean review3 = new ReviewBean("+++", people.get(0));
        ReviewBean review4 = new ReviewBean("---", people.get(1));

        books.get(0).addReview(review1).addReview(review2).addReview(review3).addReview(review4);
        books.get(1).addReview(review1).addReview(review2).addReview(review3).addReview(review4);
        books.get(2).addReview(review1).addReview(review2).addReview(review3).addReview(review4);
    }

    @Test
    public void testCXFQueries() throws Exception {
        // following queries copied from apache cxf
        Assert.assertFalse(matches(parseQuery("id=gt=100;name=Fred"), null));
        Assert.assertFalse(matches(parseQuery("id=gt=100;name==Fred"), null));
        Assert.assertFalse(matches(parseQuery("id=lt=123"), null));
        Assert.assertFalse(matches(parseQuery("date=le=2010-03-11"), null));
        Assert.assertFalse(matches(parseQuery("time=le=2010-03-11T18:00:00"), null));
        Assert.assertFalse(matches(parseQuery("name==CXF;version=ge=2.2"), null));
        Assert.assertFalse(matches(parseQuery("(age=lt=25,age=gt=35);city==London"), null));
        Assert.assertFalse(matches(parseQuery("date=lt=2000-01-01;date=gt=1999-01-01;(sub==math,sub==physics)"), null));
    }

    public Expression parseQuery(String query) throws Exception {
        p("PARSING query: " + query);
        // FiqlParser is a parser generated by javacc
        Expression exp = FiqlParser.parse(query);
        p(exp.toString());
        return exp;
    }

    public int find(String query) throws Exception {
        int found = 0;
        Expression exp = parseQuery(query);
        TypeInfo.createRoot("person", PersonBean.class);
        for (PersonBean person : people) {
            if (matches(exp, person)) found++;
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
        TypeInfo bookType = TypeInfo.createRoot("book", BookBean.class);
        Assert.assertEquals("John", bookType.retrieve(books.get(0),
                "book.author.firstName".split("\\."), 1));
        Object result = bookType.retrieve(books.get(0),
                "book.reviews.review.comment".split("\\."), 1);
        Assert.assertTrue( result instanceof List);
        List<Object> commentList = (List<Object>) result;
        Assert.assertTrue(commentList.contains("cool"));
    }

    @Test
    public void testQueryAPI() throws Exception {
        QueryContext qc = new QueryContextImpl();

        // find all books written by author with firstName "John"
        Query q1 = qc.createQuery("book.author.firstName==John", BookBean.class);
        Collection<BookBean> r1 = q1.find(books);
        p("Filtered books: " + r1.size());
        Assert.assertEquals(1, r1.size());

        // find all books reviewed by people in a city "San*"
        Query q2 = qc.createQuery("book.reviews.review.reviewer.city=San.*", BookBean.class);
        Collection<BookBean> r2 = q2.find(books);

        p("Filtered books: " + r2.size());
        Assert.assertEquals(3, r2.size());

        // find all books reviewed by people in a city "San*"
        Query q3 = qc.createQuery("book==foo", BookBean.class);
        Collection<BookBean> r3 = q3.find(books);
        Assert.assertEquals(0, r3.size());
    }

    @Test
    public void testFilter() throws Exception {
        Library library = new Library((List)books.clone());
        QueryContext qc = new QueryContextImpl();
        // find all books written by author with firstName "John"
        Query q1 = qc.createQuery("book.author.firstName==John", Library.class);
        int sizeBefore = library.getList().size();
        System.out.println(q1.filter(library, BookBean.class));
        Assert.assertEquals(1, library.getList().size());
    }
}
