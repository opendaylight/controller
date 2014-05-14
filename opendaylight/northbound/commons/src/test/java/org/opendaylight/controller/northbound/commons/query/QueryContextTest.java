/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.northbound.commons.types.StringList;

public class QueryContextTest {

    protected static final List<PersonBean> people = new ArrayList<PersonBean>();
    protected static final List<BookBean> books = new ArrayList<BookBean>();

    public static void p(String msg) {
        System.out.println("=== " + msg);
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
        books.get(1).addReview(review1);
        books.get(2).addReview(review2).addReview(review1);

    }

    @Test
    public void testQueryContext() {
        QueryContext queryContext = new QueryContextImpl();
        Assert.assertNotNull(queryContext);
    }

    @Test
    public void testSimpleQuery() throws QueryException {
        QueryContext queryContext = new QueryContextImpl();
        Query<PersonBean> query = queryContext.createQuery(
                "person.id==200", PersonBean.class);
        Assert.assertNotNull(query);

        List<PersonBean> found = query.find(people);
        Assert.assertNotNull(found);
        Assert.assertTrue(found.size() == 1);
        Assert.assertEquals("Foo", found.get(0).firstName);
    }

    @Test
    public void testAndQuery() throws QueryException {
        QueryContext queryContext = new QueryContextImpl();
        Query<PersonBean> query = queryContext.createQuery(
                "person.id!=200;(person.city='San.*')", PersonBean.class);
        Assert.assertNotNull(query);

        List<PersonBean> found = query.find(people);
        Assert.assertNotNull(found);
        Assert.assertTrue(found.size() == 2);
        Assert.assertEquals("John", found.get(0).firstName);
        Assert.assertEquals("A", found.get(1).firstName);
    }

    @Test
    public void testOrQuery() throws QueryException {
        QueryContext queryContext = new QueryContextImpl();
        Query<PersonBean> query = queryContext.createQuery(
                "person.id==200,(person.city='San.*')", PersonBean.class);
        Assert.assertNotNull(query);

        List<PersonBean> found = query.find(people);
        Assert.assertNotNull(found);
        Assert.assertTrue(found.size() == 3);
        Assert.assertEquals("John", found.get(0).firstName);
        Assert.assertEquals("Foo", found.get(1).firstName);
        Assert.assertEquals("A", found.get(2).firstName);
    }

    @Test
    public void testXmlElementWrapper() throws QueryException {
        List<String> emails = new ArrayList<String>();
        emails.add("john@cisco.com");
        emails.add("john@gmail.com");
        people.get(0).setEmail(emails);

        p(toXml(people.get(0)));
        QueryContext queryContext = new QueryContextImpl();
        Query<PersonBean> query = queryContext.createQuery(
                "person.emails.email==john@cisco.com", PersonBean.class);
        Assert.assertNotNull(query);

        List<PersonBean> found = query.find(people);
        Assert.assertNotNull(found);
        Assert.assertEquals(1,found.size());
        Assert.assertEquals("John", found.get(0).firstName);
    }

    @Test
    public void testXmlWrapperOfWrapper() throws QueryException{
        WrapperList wrapper = new WrapperList();
        wrapper.item.add("Test1");
        wrapper.item.add("Test2");

        books.get(0).addWrapperList(wrapper);
        books.get(1).addWrapperList(wrapper);

        System.out.println(toXml(books.get(0)));
        QueryContext queryContext = new QueryContextImpl();
        Query<BookBean> query = queryContext.createQuery(
                "book.parent.child.items.item==Test1", BookBean.class);
        Assert.assertNotNull(query);
    }

    @Test
    public void testXmlElementWrapperListofList() throws QueryException {
        // create Stringlist
        List<String> testList = new ArrayList<String>();
        testList.add("A");
        testList.add("B");
        StringList itemList = new StringList(testList);
        books.get(0).addToTestList(itemList);

        System.out.println(toXml(books.get(0)));
        QueryContext queryContext = new QueryContextImpl();
        Query<BookBean> query = queryContext.createQuery(
                "book.test.testList.item==A", BookBean.class);
        Assert.assertNotNull(query);
    }

    @Test
    public void testPrimitiveIteratableTypes() throws QueryException {
        // Load data for this test
        List<String> sellers = new ArrayList<String>();
        sellers.add("Amazon");

        books.get(0).setSellerInfo(sellers);
        sellers.add("Barners & Nobles");
        books.get(1).setSellerInfo(sellers);
        sellers.add("Borders");
        sellers.remove("Amazon");
        sellers.add("BookShop");
        books.get(2).setSellerInfo(sellers);

        System.out.println(toXml(books.get(0)));

        QueryContext queryContext = new QueryContextImpl();
        Query<BookBean> query = queryContext.createQuery(
                "book.soldBy==Amazon", BookBean.class);
        Assert.assertNotNull(query);

        List<BookBean> found = query.find(books);
        Assert.assertNotNull(found);
        Assert.assertEquals(2,found.size());
        Assert.assertEquals("John", found.get(0).getauthor().firstName);

        query = queryContext.createQuery(
                "book.soldBy!=Amazon", BookBean.class);
        Assert.assertNotNull(query);

        found = query.find(books);
        System.out.println("books" +found);
        Assert.assertNotNull(found);
        Assert.assertEquals(1,found.size());
        Assert.assertEquals("A", found.get(0).getauthor().firstName);
    }

    @Test
    public void testCompositeIteratableTypes() throws QueryException {
        QueryContext queryContext = new QueryContextImpl();
        Query<BookBean> query = queryContext.createQuery("book.reviews.review.reviewer.firstName==X",
                BookBean.class);
        Assert.assertNotNull(query);

        List<BookBean> found = query.find(books);
        Assert.assertNotNull(found);
        Assert.assertEquals(2, found.size());
        Assert.assertEquals("John", found.get(0).getauthor().firstName);

        query = queryContext.createQuery("book.reviews.review.comment==kewl",
                BookBean.class);
        Assert.assertNotNull(query);

        found = query.find(books);
        Assert.assertNotNull(found);
        Assert.assertEquals(2, found.size());
        p("Book 0" + found.get(0));
        Assert.assertEquals("John", found.get(0).getauthor().firstName);

        query = queryContext.createQuery("book.reviews.review.reviewer.id>300",
                BookBean.class);
        Assert.assertNotNull(query);

        found = query.find(books);
        Assert.assertNotNull(found);
        Assert.assertEquals(2, found.size());
        p("Book 0" + found.get(0));
        Assert.assertEquals("John", found.get(0).getauthor().firstName);

        query = queryContext.createQuery("book.reviews.review.reviewer.firstName!=X",
                BookBean.class);
        Assert.assertNotNull(query);

        found = query.find(books);
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
        p("Book 0" + found.get(0));
        Assert.assertEquals("Foo", found.get(0).getauthor().firstName);
    }

    @Test
    public void testXMLAccessorType() {
        //Assert.fail("implement");
    }

    @Test
    public void testMethodAnnotation() throws QueryException {
        System.out.println(toXml(books.get(0)));
        QueryContext queryContext = new QueryContextImpl();
        Query<BookBean> query = queryContext.createQuery(
                "book.isbn==preA003", BookBean.class);
        Assert.assertNotNull(query);

        List<BookBean> found = query.find(books);
        Assert.assertNotNull(found);
        Assert.assertEquals(1,found.size());
        Assert.assertEquals("A", found.get(0).getauthor().firstName);
    }

    public static String toXml(Object element) {
        try {
            JAXBContext jc = JAXBContext.newInstance(element.getClass());
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshal(element, baos);
            return baos.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Test
    public void testXMLElementWrapperForCompositeTypes(){
        //Assert.fail("implement");
    }

}