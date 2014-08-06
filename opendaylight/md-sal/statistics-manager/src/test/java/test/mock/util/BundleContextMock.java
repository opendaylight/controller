package test.mock.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;

public class BundleContextMock implements BundleContext {
    @Override
    public String getProperty(String s) {
        return null;
    }

    @Override
    public Bundle getBundle() {
        return null;
    }

    @Override
    public Bundle installBundle(String s, InputStream inputStream) throws BundleException {
        return null;
    }

    @Override
    public Bundle installBundle(String s) throws BundleException {
        return null;
    }

    @Override
    public Bundle getBundle(long l) {
        return null;
    }

    @Override
    public Bundle[] getBundles() {
        return new Bundle[0];
    }

    @Override
    public void addServiceListener(ServiceListener serviceListener, String s) throws InvalidSyntaxException {

    }

    @Override
    public void addServiceListener(ServiceListener serviceListener) {

    }

    @Override
    public void removeServiceListener(ServiceListener serviceListener) {

    }

    @Override
    public void addBundleListener(BundleListener bundleListener) {

    }

    @Override
    public void removeBundleListener(BundleListener bundleListener) {

    }

    @Override
    public void addFrameworkListener(FrameworkListener frameworkListener) {

    }

    @Override
    public void removeFrameworkListener(FrameworkListener frameworkListener) {

    }

    @Override
    public ServiceRegistration<?> registerService(String[] strings, Object o, Dictionary<String, ?> stringDictionary) {
        return null;
    }

    @Override
    public ServiceRegistration<?> registerService(String s, Object o, Dictionary<String, ?> stringDictionary) {
        return null;
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> sClass, S s, Dictionary<String, ?> stringDictionary) {
        return null;
    }

    @Override
    public ServiceReference<?>[] getServiceReferences(String s, String s2) throws InvalidSyntaxException {
        return new ServiceReference<?>[0];
    }

    @Override
    public ServiceReference<?>[] getAllServiceReferences(String s, String s2) throws InvalidSyntaxException {
        return new ServiceReference<?>[0];
    }

    @Override
    public ServiceReference<?> getServiceReference(String s) {
        return null;
    }

    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> sClass) {
        return null;
    }

    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> sClass, String s) throws InvalidSyntaxException {
        return null;
    }

    @Override
    public <S> S getService(ServiceReference<S> sServiceReference) {
        return null;
    }

    @Override
    public boolean ungetService(ServiceReference<?> serviceReference) {
        return false;
    }

    @Override
    public File getDataFile(String s) {
        return null;
    }

    @Override
    public Filter createFilter(String s) throws InvalidSyntaxException {
        return null;
    }

    @Override
    public Bundle getBundle(String s) {
        return null;
    }
}
