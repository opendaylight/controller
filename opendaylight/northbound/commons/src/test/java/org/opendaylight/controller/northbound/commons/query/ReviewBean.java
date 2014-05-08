package org.opendaylight.controller.northbound.commons.query;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 */
@XmlRootElement(name="review")
public class ReviewBean {
    @XmlElement(name="date")
    private  Date _publishedDate;
    @XmlElement(name="comment")
    private  String _comment;
    @XmlElement(name="reviewer")
    private  PersonBean _reviewer;
    @XmlElement
    private int _upVotes;
    @XmlElement
    private int _downVotes;
    public ReviewBean(){}

    public ReviewBean(String comment, PersonBean user) {
        _comment = comment;
        _reviewer = user;
        _publishedDate = new Date();
    }

    public void vote(int up, int down) {
        _upVotes += up;
        _downVotes += down;
    }

    @Override
    public String toString() {
        return "ReviewBean <publishedDate>" + _publishedDate + "</publishedDate> <comment>"
                + _comment + "</comment> <reviewer>" + _reviewer + "</reviewer> <upVotes>" + _upVotes
                + "</upVotes> <downVotes>" + _downVotes + "</downVotes>";
    }
}
