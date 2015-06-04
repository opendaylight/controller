package org.opendaylight.crypto;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing reliable configuration for secure transport protocols e.g. TLS
 */
public final class CryptoUtil {

    private static final Logger LOG = LoggerFactory.getLogger(CryptoUtil.class);

    private CryptoUtil() {}

    /**
     * Default list of excluded ciphers/cipher-suits.
     * This list was created by examining default enabled ciphers in Oracle JVM 1.7.0_80.
     * RC4 is excluded due to https://tools.ietf.org/html/rfc7465.
     *
     * Additional resource: List of weak/strong ciphers in JVM http://www.techstacks.com/howto/j2se5_ssl_cipher_strength.html.
     */
    // TODO keep cipher-suits for TLS, cipher for SSH and key exchange for SSH in a single list ?
    public static final List<String> DEFAULT_EXCLUDE_CIPHERS = Lists.newArrayList(
            // TLS cipher-suits
            ".*EXPORT.*", ".*NULL.*", ".*RC4.*", ".*anon.*", ".*KRB5.*",
            "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_RSA_WITH_DES_CBC_SHA",
            // SSH ciphers
            ".*arcfour.*");
            // TODO what about SSH key exchange ? mina ssh default key ex algos: DHGEX256, DHGEX, ECDHP256, ECDHP384, ECDHP521, DHG14, DHG1

    /**
     * System property for setting the list of ciphers to exclude
     */
    private static final String EXLUDE_CIPHER_LIST_PROPERTY = "org.opendaylight.crypto.cipher.excludes";

    private static final Splitter REGEX_SEPARATOR = Splitter.on(Pattern.compile(","));

    private static volatile CipherFilter currentCipherFilter;

    static {
        final String property = System.getProperty(EXLUDE_CIPHER_LIST_PROPERTY);
        final Iterable<String> excludeCipherPatterns;
        if (property != null) {
            excludeCipherPatterns = REGEX_SEPARATOR.split(property);
        } else {
            excludeCipherPatterns = DEFAULT_EXCLUDE_CIPHERS;
        }

        LOG.info("Following cipher-suits/ciphers will be disabled: {}" + excludeCipherPatterns);
        setCipherExcludes(excludeCipherPatterns);
    }

    @VisibleForTesting
    static void setCipherExcludes(Iterable<String> excludeCiphers) {
        currentCipherFilter = new CipherFilter(excludeCiphers);
    }

    /**
     * Return filtered iterable where prohibited ciphers/cipher-suits are removed
     *
     * @param allCiphers iterable of ciphers/cipher-suits to be filtered
     * @return filtered view of input iterable
     */
    public static Iterable<String> filterCiphers(Iterable<String> allCiphers) {
        return currentCipherFilter.filterCiphers(allCiphers);
    }

    /**
     * Return filtered collection where prohibited ciphers/cipher-suits are removed
     *
     * @param allCiphers collection of ciphers/cipher-suits to be filtered
     * @return filtered view of input collection
     */
    public static Collection<String> filterCiphers(Collection<String> allCiphers) {
        return currentCipherFilter.filterCiphers(allCiphers);
    }

    /**
     * Instantiate SSLEngine from provided SSLContext and filter its enabled ciphers.
     * The list of ciphers to be excluded can be configured using system property: <br/>
     * {@link #EXLUDE_CIPHER_LIST_PROPERTY}
     * <p/>
     *
     * Or the default list will be used: <br/>
     * {@link #DEFAULT_EXCLUDE_CIPHERS}
     *
     * @param sslContext context from which the engine will be created
     * @return new SSLEngine with filtered list of enabled ciphers
     */
    public static SSLEngine createSecureSSLEngine(SSLContext sslContext) {
        final SSLEngine sslEngine = sslContext.createSSLEngine();
        String[] enabledCipherSuites = sslEngine.getEnabledCipherSuites();
        Preconditions.checkArgument(enabledCipherSuites.length > 0, "No enabled cipher suits in ssl engine: %s", sslEngine);
        sslEngine.setEnabledCipherSuites(currentCipherFilter.filterCiphers(enabledCipherSuites));
        return sslEngine;
    }

}
