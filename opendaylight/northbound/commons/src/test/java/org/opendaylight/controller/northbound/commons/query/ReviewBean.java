package org.opendaylight.controller.northbound.commons.query;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

/**
 */
@XmlRootElement(name="review")
public class ReviewBean {
  @XmlElement(name="date")
  private Date _publishedDate;
  @XmlElement(name="comment")
  private String _comment;
  @XmlElement(name="reviewer")
  private PersonBean _reviewer;
  @XmlElement
  private int _upVotes;
  @XmlElement
  private int _downVotes;

  public ReviewBean(String comment, PersonBean user) {
    _comment = comment;
    _reviewer = user;
    _publishedDate = new Date();
  }

  public void vote(int up, int down) {
    _upVotes += up;
    _downVotes += down;
  }
}
