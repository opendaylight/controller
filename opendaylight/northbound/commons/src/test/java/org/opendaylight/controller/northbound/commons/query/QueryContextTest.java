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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueryContextTest {

  protected static final List<PersonBean> people = new ArrayList<PersonBean>();
  protected static final List<BookBean> books = new ArrayList<BookBean>();
  @Test
  public void getQueryContext() {
    QueryContext queryContext = new QueryContextImpl();
    Assert.assertNotNull(queryContext);
  }

  @Test
  public void getQuery() {
    QueryContext queryContext = new QueryContextImpl();
    Query<PersonBean> query = queryContext.createQuery(
        "people.id==200;(people.city='San.*')", PersonBean.class);
    Assert.assertNotNull(query);

    List<PersonBean> found = query.filter(people);
    Assert.assertNotNull(found);
    Assert.assertTrue(found.size() == 1);
    Assert.assertEquals("Foo", found.get(0).firstName);
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

}