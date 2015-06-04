package org.opendaylight.crypto;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple filter the utilizes regex patterns to match excluded ciphers
 */
final class CipherFilter {

    private static final Logger LOG = LoggerFactory.getLogger(CipherFilter.class);

    static final Function<String, CipherMatcher> CIPHER_EXCLUDE_STRING_TO_EXCLUSION = new Function<String, CipherMatcher>() {
        @Override
        public CipherMatcher apply(final String toBeExcluded) {
            try {
                return new RegexCipherMatcher(toBeExcluded);
            } catch (PatternSyntaxException e) {
                LOG.error("Unable to parse pattern for cipher exclusion {}", toBeExcluded, e);
                throw new IllegalArgumentException("Unable to compile pattern from " + toBeExcluded, e);
            }
        }
    };

    private Predicate<String> predicate;

    CipherFilter(final Iterable<String> defaultExcludeCiphers) {
        // wrap transformed list into a regular collection to prevent lazy initialization in every iteration
        final List<CipherMatcher> cipherExclusions =
                Lists.newArrayList(Iterables.transform(defaultExcludeCiphers, CIPHER_EXCLUDE_STRING_TO_EXCLUSION));

        predicate = new Predicate<String>() {
            @Override
            public boolean apply(final String input) {
                for (CipherMatcher matcher : cipherExclusions) {
                    if (matcher.matches(input)) {
                        LOG.debug("Disabling cipher-suite/cipher {}", input);
                        return false;
                    }
                }
                return true;
            }
        };
    }

    Collection<String> filterCiphers(Collection<String> allCiphers) {
        return Collections2.filter(allCiphers, predicate);
    }

    Iterable<String> filterCiphers(Iterable<String> allCiphers) {
        return Iterables.filter(allCiphers, predicate);
    }

    String[] filterCiphers(final String[] allCiphers) {
        return Iterables.toArray(filterCiphers(Arrays.asList(allCiphers)), String.class);
    }

    interface CipherMatcher {

        boolean matches(String cipher);
    }

    private static class RegexCipherMatcher implements CipherMatcher {
        private final Pattern pattern;

        public RegexCipherMatcher(final String toBeExcluded) {
            this.pattern = Pattern.compile(toBeExcluded);
        }

        @Override
        public boolean matches(final String cipher) {
            return pattern.matcher(cipher).matches();
        }
    }
}
