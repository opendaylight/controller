package org.opendaylight.crypto;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import org.junit.Before;
import org.junit.Test;

public class CryptoUtilTest {

    private static final List<String> FULL_CIPHER_SUITE_LIST = Lists.newArrayList("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
            "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
            "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_SHA",
            "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
            "TLS_ECDH_RSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_MD5",
            "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
            "TLS_DH_anon_WITH_AES_128_CBC_SHA256",
            "TLS_ECDH_anon_WITH_AES_128_CBC_SHA",
            "TLS_DH_anon_WITH_AES_128_CBC_SHA",
            "TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA",
            "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDH_anon_WITH_RC4_128_SHA",
            "SSL_DH_anon_WITH_RC4_128_MD5",
            "SSL_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_DSS_WITH_DES_CBC_SHA",
            "SSL_DH_anon_WITH_DES_CBC_SHA",
            "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
            "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
            "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
            "TLS_RSA_WITH_NULL_SHA256",
            "TLS_ECDHE_ECDSA_WITH_NULL_SHA",
            "TLS_ECDHE_RSA_WITH_NULL_SHA",
            "SSL_RSA_WITH_NULL_SHA",
            "TLS_ECDH_ECDSA_WITH_NULL_SHA",
            "TLS_ECDH_RSA_WITH_NULL_SHA",
            "TLS_ECDH_anon_WITH_NULL_SHA",
            "SSL_RSA_WITH_NULL_MD5",
            "TLS_KRB5_WITH_3DES_EDE_CBC_SHA",
            "TLS_KRB5_WITH_3DES_EDE_CBC_MD5",
            "TLS_KRB5_WITH_RC4_128_SHA",
            "TLS_KRB5_WITH_RC4_128_MD5",
            "TLS_KRB5_WITH_DES_CBC_SHA",
            "TLS_KRB5_WITH_DES_CBC_MD5",
            "TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA",
            "TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5",
            "TLS_KRB5_EXPORT_WITH_RC4_40_SHA",
            "TLS_KRB5_EXPORT_WITH_RC4_40_MD5");

    private static final List<String> RC4_CIPHERS = Lists.newArrayList("arcfour256", "arcfour128", "arcfour");

    @Test
    public void testFilterWithDefaultExcludesRC4() throws Exception {
        CryptoUtil.setCipherExcludes(CryptoUtil.DEFAULT_EXCLUDE_CIPHERS);
        final Collection<String> filtered = CryptoUtil.filterCiphers(RC4_CIPHERS);

        assertTrue(filtered.toString(), filtered.isEmpty());
    }

    @Test
    public void testFilterWithDefaultExcludes() throws Exception {
        CryptoUtil.setCipherExcludes(CryptoUtil.DEFAULT_EXCLUDE_CIPHERS);
        final Collection<String> filtered = CryptoUtil.filterCiphers(FULL_CIPHER_SUITE_LIST);

        assertThat(filtered.size(), is(22));
        assertThat(filtered, not(hasItems("SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA", "SSL_DH_anon_WITH_RC4_128_MD5",
                "TLS_ECDH_ECDSA_WITH_NULL_SHA", "SSL_DH_anon_WITH_DES_CBC_SHA")));
        assertThat(filtered, hasItems("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA"));
    }

    private SSLContext serverContext;
    private List<String> enabledCipherSuitesInControl;

    @Before
    public void setUp() throws Exception {
        serverContext = SSLContext.getInstance("TLS");
        serverContext.init(new KeyManager[]{}, new TrustManager[]{}, null);

        final SSLEngine controlEngine = serverContext.createSSLEngine();
        enabledCipherSuitesInControl = Arrays.asList(controlEngine.getEnabledCipherSuites());
    }

    @Test
    public void testCreateSecureSSLEngineNoExcludes() throws Exception {
        // Disable filtering
        CryptoUtil.setCipherExcludes(Lists.<String>newArrayList());
        SSLEngine secureSSLEngine = CryptoUtil.createSecureSSLEngine(serverContext);
        List<String> enabledCipherSuitesInFiltered = Arrays.asList(secureSSLEngine.getEnabledCipherSuites());

        assertEquals(enabledCipherSuitesInControl, enabledCipherSuitesInFiltered);
    }

    @Test
    public void testCreateSecureSSLEngineNoTLSAndSHA() {
        final SSLEngine secureSSLEngine;
        final List<String> enabledCipherSuitesInFiltered;
        CryptoUtil.setCipherExcludes(Lists.newArrayList("^TLS.*SHA$"));

        secureSSLEngine = CryptoUtil.createSecureSSLEngine(serverContext);
        enabledCipherSuitesInFiltered = Arrays.asList(secureSSLEngine.getEnabledCipherSuites());

        int enabledNoTlsSha = Collections2.filter(enabledCipherSuitesInControl, new Predicate<String>() {
            @Override
            public boolean apply(final String input) {
                return (input.endsWith("SHA") && input.startsWith("TLS")) == false;
            }
        }).size();

        assertEquals("Without SHA end: " + enabledCipherSuitesInFiltered, enabledNoTlsSha, enabledCipherSuitesInFiltered.size());
    }

    @Test
    public void testCreateSecureSSLEngineNoSHA() {
        CryptoUtil.setCipherExcludes(Lists.newArrayList(".*SHA"));

        final SSLEngine secureSSLEngine = CryptoUtil.createSecureSSLEngine(serverContext);
        final List<String> enabledCipherSuitesInFiltered = Arrays.asList(secureSSLEngine.getEnabledCipherSuites());

        int enabledNoSha = Collections2.filter(enabledCipherSuitesInControl, new Predicate<String>() {
            @Override
            public boolean apply(final String input) {
                return input.endsWith("SHA") == false;
            }
        }).size();

        assertEquals("Without SHA end: " + enabledCipherSuitesInFiltered, enabledNoSha, enabledCipherSuitesInFiltered.size());
    }

    @Test
    public void testCreateSecureSSLEngineNoTLS() {
        CryptoUtil.setCipherExcludes(Lists.newArrayList("TLS.*"));

        final SSLEngine secureSSLEngine = CryptoUtil.createSecureSSLEngine(serverContext);
        final List<String> enabledCipherSuitesInFiltered = Arrays.asList(secureSSLEngine.getEnabledCipherSuites());

        int enabledNoTls = Collections2.filter(enabledCipherSuitesInControl, new Predicate<String>() {
            @Override
            public boolean apply(final String input) {
                return input.startsWith("TLS") == false;
            }
        }).size();

        assertEquals("Without TLS start: " + enabledCipherSuitesInFiltered, enabledNoTls, enabledCipherSuitesInFiltered.size());
    }

    @Test
    public void testCreateSecureSSLEngineExcludeAll() {
        CryptoUtil.setCipherExcludes(enabledCipherSuitesInControl);

        final SSLEngine secureSSLEngine = CryptoUtil.createSecureSSLEngine(serverContext);
        final List<String> enabledCipherSuitesInFiltered = Arrays.asList(secureSSLEngine.getEnabledCipherSuites());

        assertEquals(0, enabledCipherSuitesInFiltered.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCipherRegex() throws Exception {
        CryptoUtil.setCipherExcludes(Lists.newArrayList("[[["));

    }
}