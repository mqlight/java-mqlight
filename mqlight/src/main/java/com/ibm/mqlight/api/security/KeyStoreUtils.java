/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.ibm.mqlight.api.security;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

import org.apache.commons.ssl.PKCS8Key;

import com.ibm.mqlight.api.impl.LogbackLogging;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

/**
 * Utilities for creating and maintaining {@link KeyStore} instances.
 *
 */
public class KeyStoreUtils {

    private static final Logger logger = LoggerFactory.getLogger(KeyStoreUtils.class);

    static {
        LogbackLogging.setup();
    }
    
    /**
     * Attempts to load the specified file as a {@link KeyStore}, determining the key store type from the file extension.
     * 
     * @param file
     * @param password
     * @return A {@link KeyStore} instance loaded from the specified file.
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    public static KeyStore loadKeyStore(File file, String password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        final String methodName = "loadKeyStore";
        logger.entry(methodName, file);
        
        final String fileName = file.getName();
        final java.security.KeyStore keyStore;
        if (fileName.matches("(?i).*\\.jks")) {
            keyStore = java.security.KeyStore.getInstance("JKS");
        } else if (fileName.matches("(?i).*\\.jceks")) {
            keyStore = java.security.KeyStore.getInstance("JCEKS");
        } else if (fileName.matches("(?i).*\\.p12") || fileName.matches("(?i).*\\.pkcs12") ) {
            keyStore = java.security.KeyStore.getInstance("PKCS12");
        } else if (fileName.matches("(?i).*\\.kdb")) {
            keyStore = java.security.KeyStore.getInstance("CMSKS");
        } else {
            final String keyStoreType = java.security.KeyStore.getDefaultType();
            keyStore = java.security.KeyStore.getInstance(keyStoreType);
        }
        
        java.io.FileInputStream fileInputStream = null;
        try {
            fileInputStream = new java.io.FileInputStream(file);
            final char [] passwordChars;
            if (password == null) {
                passwordChars = null;
                // TODO would be useful if we could attempt to read the password from an associated stash file.
            } else {
                passwordChars = password.toCharArray();
            }
            
            keyStore.load(fileInputStream, passwordChars);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch(IOException e) {
                    logger.data(methodName, (Object)"failed to close file: ", file, " reason", e);
                }
            }
        }
        
        logger.exit(methodName, keyStore);
        
        return keyStore;
    }
    
    /**
     * Adds a private key to the specified key store from the passed private key file and certificate chain.
     * 
     * @param keyStore
     *            The key store to receive the private key.
     * @param pemKeyFile
     *            A PEM format file containing the private key.
     * @param passwordChars
     *            The password that protects the private key.
     * @param certChain The certificate chain to associate with the private key.
     * @throws IOException
     * @throws GeneralSecurityException 
     */
    public static void addPrivateKey(KeyStore keyStore, File pemKeyFile, char[] passwordChars, List<Certificate> certChain) throws IOException, GeneralSecurityException {
        final String methodName = "addPrivateKey";
        logger.entry(methodName, pemKeyFile, certChain);

        PrivateKey privateKey = createPrivateKey(pemKeyFile, passwordChars);
        
        keyStore.setKeyEntry("key", privateKey, passwordChars, certChain.toArray(new Certificate[certChain.size()]));
        
        logger.exit(methodName);
    }
    
    /**
     * Creates a {@link PrivateKey} instance from the passed private key PEM format file and certificate chain.
     * 
     * @param pemKeyFile
     *            A PEM format file containing the private key.
     * @param passwordChars
     *            The password that protects the private key.
     * @throws IOException
     * @throws GeneralSecurityException 
     */
    public static PrivateKey createPrivateKey(File pemKeyFile, char[] passwordChars) throws IOException, GeneralSecurityException {
        final String methodName = "createPrivateKey";
        logger.entry(methodName, pemKeyFile);

        // Read the private key from the PEM format file
        final PemFile privateKeyPemFile = new PemFile(pemKeyFile);
        final byte[] privateKeyBytes = privateKeyPemFile.getPrivateKeyBytes();
        
        final PrivateKey privateKey;
        if (privateKeyPemFile.containsEncryptedPrivateKey()) {
            // We should be able to do the follows (using standard JDK classes):
            //    EncryptedPrivateKeyInfo encryptPrivateKeyInfo = new EncryptedPrivateKeyInfo(privateKeyBytes);
            //    Cipher cipher = Cipher.getInstance(encryptPrivateKeyInfo.getAlgName());
            //    PBEKeySpec pbeKeySpec = new PBEKeySpec(passwordChars);
            //    SecretKeyFactory secFac = SecretKeyFactory.getInstance(encryptPrivateKeyInfo.getAlgName());
            //    Key pbeKey = secFac.generateSecret(pbeKeySpec);
            //    AlgorithmParameters algParams = encryptPrivateKeyInfo.getAlgParameters();
            //    cipher.init(Cipher.DECRYPT_MODE, pbeKey, algParams);
            //    KeySpec keySpec = encryptPrivateKeyInfo.getKeySpec(cipher);
            //    privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
            // but this can fail with a: "Unsupported key protection algorithm" if key was generated with openssl
            //
            // Instead we use the Apache commons SSL PKCS8Key class from Julius Davies (see not-yet-commons-ssl in Maven)
            // which seems more reliable
            final PKCS8Key key = new PKCS8Key(privateKeyBytes, passwordChars);
            privateKey = key.getPrivateKey();
        } else {
            final KeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            InvalidKeySpecException keyFactoryException = null;
            PrivateKey key = null;
            for (String alg : new String[] { "RSA", "DSA", "DiffieHellman", "EC" }) {
                try {
                    key = KeyFactory.getInstance(alg).generatePrivate(keySpec);
                    break;
                } catch (InvalidKeySpecException e) {
                    if (keyFactoryException == null) keyFactoryException = e;
                }
            }
            if (key == null) throw keyFactoryException;
            privateKey = key;
        }
        
        logger.exit(methodName, privateKey);
        
        return privateKey;
    }
}
