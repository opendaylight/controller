package org.opendaylight.controller.northbound.commons.query;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 */
@XmlRootElement(name="book")
public class BookBean {
  @XmlElement(name="name")
  private String _name;
  @XmlElement
  private String _isbn;
  @XmlElement(name="author")
  private PersonBean _author;
  @XmlElement(name="reviews")
  private List<ReviewBean> _reviews = new ArrayList<ReviewBean>();

  public BookBean(String name, String id, PersonBean person) {
    _name = name;
    _isbn = id;
    _author = person;
  }

  public BookBean addReview(ReviewBean review) {
    _reviews.add(review);
    return this;
  }


}
