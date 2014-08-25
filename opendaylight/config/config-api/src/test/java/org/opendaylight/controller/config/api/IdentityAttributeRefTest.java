package org.opendaylight.controller.config.api;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;

import javax.management.*;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class IdentityAttributeRefTest {

    IdentityAttributeRef attr = new IdentityAttributeRef("attr");

    @Test
    public void testConstructor() throws Exception {
        String param = new String("attr");
        Assert.assertEquals(attr.getqNameOfIdentity(), param);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor2() throws Exception {
        IdentityAttributeRef attr = new IdentityAttributeRef(null);
    }

    @Test
    public void testHashCode() throws Exception {
        Assert.assertEquals(attr.hashCode(), new String("attr").hashCode());
    }

    @Test
    public void testEqual() throws Exception {
        Assert.assertEquals(attr, attr);
    }

    @Test
    public void testEqual2() throws Exception {
        Assert.assertEquals(attr, new IdentityAttributeRef("attr"));
    }

    @Test
    public void testNotEqual() throws Exception {
        Assert.assertNotEquals(attr, new IdentityAttributeRef("different"));
    }

    @Test
    public void testResolveIdentity() throws Exception {
        DependencyResolver res = mock(DependencyResolver.class);
        IdentityAttributeRef a = new IdentityAttributeRef("abcd");
        doReturn(SubIdentity.class).when(res).resolveIdentity(a, Identity.class);
        a.resolveIdentity(res, Identity.class);
        verify(res).resolveIdentity(a, Identity.class);
    }

    @Test
    public void testValidateIdentity() throws Exception {
        DependencyResolver res = mock(DependencyResolver.class);
        JmxAttribute jmxAttr = new JmxAttribute("abc");
        doNothing().when(res).validateIdentity(attr, Identity.class, jmxAttr);
        attr.validateIdentity(res, Identity.class, jmxAttr);
        verify(res).validateIdentity(attr, Identity.class, jmxAttr);
    }

    static class Identity extends BaseIdentity {}

    static class SubIdentity extends Identity {}
}
