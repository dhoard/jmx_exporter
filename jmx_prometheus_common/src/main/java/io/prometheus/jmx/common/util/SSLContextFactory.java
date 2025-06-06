/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.common.util;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Class to implement SSLContextFactory */
public class SSLContextFactory {

    private static final String[] PROTOCOLS = {"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1"};

    /** Constructor */
    private SSLContextFactory() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to create an SSLContext
     *
     * @param keyStoreType keyStoreType
     * @param keyStoreFilename keyStoreFilename
     * @param keyStorePassword keyStorePassword
     * @param certificateAlias certificateAlias
     * @param trustStoreType trustStoreType
     * @param trustStoreFilename trustStoreFilename
     * @param trustStorePassword trustStorePassword
     * @return the return value
     * @throws GeneralSecurityException GeneralSecurityException
     * @throws IOException IOException
     */
    public static SSLContext createSSLContext(
            String keyStoreType,
            String keyStoreFilename,
            String keyStorePassword,
            String certificateAlias,
            String trustStoreType,
            String trustStoreFilename,
            String trustStorePassword)
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        KeyStore trustStore = null;

        try (InputStream inputStream = Files.newInputStream(Paths.get(keyStoreFilename))) {
            // Load the keystore
            keyStore.load(
                    inputStream, keyStorePassword != null ? keyStorePassword.toCharArray() : null);

            // Loop through the certificate aliases in the keystore
            // building a set of certificate aliases that don't match
            // the requested certificate alias
            Set<String> certificateAliasesToRemove = new HashSet<>();
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String keyStoreCertificateAlias = aliases.nextElement();
                if (!keyStoreCertificateAlias.equals(certificateAlias)) {
                    certificateAliasesToRemove.add(keyStoreCertificateAlias);
                }
            }

            // Remove the certificate aliases that don't
            // match the requested certificate alias from the keystore
            for (String certificateAliasToRemove : certificateAliasesToRemove) {
                keyStore.deleteEntry(certificateAliasToRemove);
            }

            // Validate the keystore contains the certificate alias that is requested
            if (!keyStore.containsAlias(certificateAlias)) {
                throw new GeneralSecurityException(
                        format(
                                "certificate alias [%s] not found in keystore [%s]",
                                certificateAlias, keyStoreFilename));
            }
        }

        if (trustStoreFilename != null) {
            trustStore = KeyStore.getInstance(trustStoreType);
            try (InputStream inputStream = Files.newInputStream(Paths.get(trustStoreFilename))) {
                trustStore.load(
                        inputStream,
                        trustStorePassword != null ? trustStorePassword.toCharArray() : null);
            }
        }

        // Create and initialize an SSLContext

        KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        keyManagerFactory.init(
                keyStore, keyStorePassword != null ? keyStorePassword.toCharArray() : null);

        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        trustManagerFactory.init(trustStore == null ? keyStore : trustStore);

        SSLContext sslContext = createSSLContext();

        sslContext.init(
                keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                new SecureRandom());

        return sslContext;
    }

    /**
     * Method to create an SSLContext, looping through more secure to less secure TLS protocols
     *
     * @return the return value
     * @throws GeneralSecurityException GeneralSecurityException
     */
    private static SSLContext createSSLContext() throws GeneralSecurityException {
        // Loop through potential protocols since there doesn't appear
        // to be a way to get the most secure supported protocol
        for (String protocol : PROTOCOLS) {
            try {
                return SSLContext.getInstance(protocol);
            } catch (Throwable t) {
                // INTENTIONALLY BLANK
            }
        }

        throw new GeneralSecurityException("No supported TLS protocols found");
    }
}
