package org.opendaylight.controller.northbound.commons.exception;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;

public class CommonsNorthboundExceptionTest extends TestCase {

    @Test
    public void testMethodNotAllowed() {
        MethodNotAllowed mna = new MethodNotAllowed();
        Assert.assertTrue(mna.getStatusCode() == 405);
        Assert.assertTrue(mna.getReasonPhrase().equals("Method Not Allowed"));
        Assert.assertTrue(mna.getFamily().equals(
                Response.Status.Family.CLIENT_ERROR));
    }

    @Test
    public void testInternalServerErrorException() {
        try {
            throw new InternalServerErrorException("Internal Server Exception");
        } catch (InternalServerErrorException e) {
            Assert.assertTrue(e instanceof InternalServerErrorException);
            Assert.assertTrue(e.getResponse().getEntity()
                    .equals("Internal Server Exception"));
        }
    }

    @Test
    public void testMethodNotAllowedException() {
        try {
            throw new MethodNotAllowedException("Method Not Allowed Exception");
        } catch (MethodNotAllowedException e) {
            Assert.assertTrue(e instanceof MethodNotAllowedException);
            Assert.assertTrue(e.getResponse().getEntity()
                    .equals("Method Not Allowed Exception"));
        }
    }

    @Test
    public void testNotAcceptableException() {
        try {
            throw new NotAcceptableException("Not Acceptable Exception");
        } catch (NotAcceptableException e) {
            Assert.assertTrue(e instanceof NotAcceptableException);
            Assert.assertTrue(e.getResponse().getEntity()
                    .equals("Not Acceptable Exception"));
        }
    }

    @Test
    public void testResourceConflictException() {
        try {
            throw new ResourceConflictException("Resource Conflict Exception");
        } catch (ResourceConflictException e) {
            Assert.assertTrue(e instanceof ResourceConflictException);
            Assert.assertTrue(e.getResponse().getEntity()
                    .equals("Resource Conflict Exception"));
        }
    }

    @Test
    public void testResourceForbiddenException() {
        try {
            throw new ResourceForbiddenException("Resource Forbidden Exception");
        } catch (ResourceForbiddenException e) {
            Assert.assertTrue(e instanceof ResourceForbiddenException);
            Assert.assertTrue(e.getResponse().getEntity()
                    .equals("Resource Forbidden Exception"));
        }
    }

    @Test
    public void testResourceGoneException() {
        try {
            throw new ResourceGoneException("Resource Gone Exception");
        } catch (ResourceGoneException e) {
            Assert.assertTrue(e instanceof ResourceGoneException);
            Assert.assertTrue(e.getResponse().getEntity()
                    .equals("Resource Gone Exception"));
        }
    }

    @Test
    public void testResourceNotFoundException() {
        try {
            throw new ResourceNotFoundException("Resource Not Found Exception");
        } catch (ResourceNotFoundException e) {
            Assert.assertTrue(e instanceof ResourceNotFoundException);
            Assert.assertTrue(e.getResponse().getEntity()
                    .equals("Resource Not Found Exception"));
        }
    }

    @Test
    public void testServiceUnavailableException() {
        try {
            throw new ServiceUnavailableException(
                    "Service Unavailable Exception");
        } catch (ServiceUnavailableException e) {
            Assert.assertTrue(e instanceof ServiceUnavailableException);
            Assert.assertTrue(e.getResponse().getEntity()
                    .equals("Service Unavailable Exception"));
        }
    }

    @Test
    public void testUnauthorizedException() {
        try {
            throw new UnauthorizedException("Unauthorized Exception");
        } catch (UnauthorizedException e) {
            Assert.assertTrue(e instanceof UnauthorizedException);
            Assert.assertTrue(e.getResponse().getEntity()
                    .equals("Unauthorized Exception"));
        }
    }

    @Test
    public void testUnsupportedMediaTypeException() {
        try {
            throw new UnsupportedMediaTypeException(
                    "Unsupported Media Type Exception");
        } catch (UnsupportedMediaTypeException e) {
            Assert.assertTrue(e instanceof UnsupportedMediaTypeException);
            Assert.assertTrue(e.getResponse().getEntity()
                    .equals("Unsupported Media Type Exception"));
        }
    }

}
