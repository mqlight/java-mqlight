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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.KeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.mqlight.api.impl.LogbackLogging;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

/**
 * Represents a PEM format file, enabling private keys and certificates to be extracted.
 *
 */
public class PemFile {
    private static final Logger logger = LoggerFactory.getLogger(PemFile.class);

    static {
        LogbackLogging.setup();
    }

    /** RegEx pattern to detect a certificate. */
    private static final Pattern CERTIFICATE_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+([a-z0-9+/=\\r\\n]+)-+END\\s+.*CERTIFICATE[^-]*-+",
            Pattern.CASE_INSENSITIVE);
    
    /** RegEx pattern to detect an encrypted private key. */
    private static final Pattern ENCRYPTED_KEY_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*ENCRYPTED PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+([a-z0-9+/=\\r\\n]+)-+END\\s+.*ENCRYPTED PRIVATE\\s+KEY[^-]*-+",
            Pattern.CASE_INSENSITIVE);
    
    /** RegEx pattern to detect a private key. */
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+([a-z0-9+/=\\r\\n]+)-+END\\s+.*PRIVATE\\s+KEY[^-]*-+",
            Pattern.CASE_INSENSITIVE);

    /** The PEM format file. */
    private final File pemFile;
    
    /**
     * Creates an instance for the specified file.
     * <p>
     * Note that the file will be only be read on first use. 
     * 
     * @param pemFile A PEM format file.
     */
    public PemFile(File pemFile) {
        this.pemFile = pemFile;
    }
    
    /**
     * Obtains the list of certificates stored in the PEM file.
     * 
     * @return The list of certificates stored in the PEM file.
     * @throws CertificateException If a parsing error occurs.
     * @throws IOException If the PEM file cannot be read for any reason.
     */
    public List<Certificate> getCertificates() throws CertificateException, IOException {
        final String methodName = "getCertificates";
        logger.entry(this, methodName);
        
        final String fileData = getPemFileData();

        final List<ByteBuf> certDataList = new ArrayList<ByteBuf>();
        final Matcher m = CERTIFICATE_PATTERN.matcher(fileData);
        int start = 0;
        while (m.find(start)) {            
            final ByteBuf base64CertData = Unpooled.copiedBuffer(m.group(1), Charset.forName("US-ASCII"));
            final ByteBuf certData = Base64.decode(base64CertData);
            base64CertData.release();
            certDataList.add(certData);
            start = m.end();
        }
        
        if (certDataList.isEmpty()) {
            final CertificateException exception = new CertificateException("No certificates found in PEM file: " + pemFile);
            logger.throwing(this,  methodName, exception);
            throw exception;
        }
        
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final List<Certificate> certificates = new ArrayList<Certificate>();

        try {
            for (ByteBuf certData: certDataList) {
                certificates.add(cf.generateCertificate(new ByteBufInputStream(certData)));
            }
        } finally {
            for (ByteBuf certData: certDataList) certData.release();
        }
        
        logger.exit(this, methodName, certificates);
        
        return certificates;
    }

    /**
     * Obtains the private key data as a byte array from the PEM file.
     * <p>
     * Within the PEM file the private key data is base 64 encoded. This method will decode the data for the returned private key
     * data.
     * <p>
     * Note that for an encrypted private key the data will remain encrypted.
     * 
     * @return The private key data.
     * @throws KeyException If a private key cannot be found in the PEM file. 
     * @throws IOException If the PEM file cannot be read for any reason.
     */
    public byte[] getPrivateKeyBytes() throws KeyException, IOException {
        final String methodName = "getPrivateKeyBytes";
        logger.entry(this, methodName);

        final String fileData = getPemFileData();

        Matcher m = KEY_PATTERN.matcher(fileData);
        final byte[] keyBytes;
        final String base64KeyDataStr;
        if (m.find()) {
            base64KeyDataStr = m.group(1);
        } else {
            m = ENCRYPTED_KEY_PATTERN.matcher(fileData);
            if (m.find()) {
                base64KeyDataStr = m.group(1);
            } else {
                final KeyException exception = new KeyException("Private key not found in PEM file: " + pemFile);
                logger.throwing(this, methodName, exception);
                throw exception;
            }
        }

        final ByteBuf base64KeyData = Unpooled.copiedBuffer(base64KeyDataStr, Charset.forName("US-ASCII"));
        final ByteBuf keyData = Base64.decode(base64KeyData);
        base64KeyData.release();
        keyBytes = new byte[keyData.readableBytes()];
        keyData.readBytes(keyBytes).release();

        logger.exit(this, methodName, keyBytes);

        return keyBytes;
    }
    
    /**
     * @return {@code true} If the PEM file contains an encrypted private key, {@code false} otherwise.
     * @throws IOException If the PEM file cannot be read for any reason.
     */
    public boolean containsEncryptedPrivateKey() throws IOException {
        final String methodName = "containsEncryptedPrivateKey";
        logger.entry(this, methodName);
        
        final String fileData = getPemFileData();
        final Matcher m = ENCRYPTED_KEY_PATTERN.matcher(fileData);
        final boolean result = m.find();
        
        logger.exit(this, methodName, result);
        
        return result;
    }

    /** A cache for the contents of the PEM file. */
    private String pemFileData = null;
    
    /**
     * Read the PEM file data, caching it locally, such that the file is only read on first use.
     * 
     * @return The PEM file data are a String.
     * @throws IOException If the PEM file cannot be read for any reason.
     */
    private String getPemFileData() throws IOException {
        final String methodName = "getPemFileData";
        logger.entry(this, methodName);
        
        if (pemFileData != null) {
            logger.exit(this, methodName, pemFileData);
            return pemFileData;
        }
        
        final StringBuilder sb = new StringBuilder();
        final InputStreamReader isr = new InputStreamReader(new FileInputStream(pemFile), "US-ASCII");
        try {
            int ch;
            while ((ch = isr.read()) != -1) {
                sb.append((char)ch);
            }
        } finally {
            try {
                isr.close();
            } catch (IOException e) {
                logger.data(this, methodName, "Failed to close input stream reader. Reason: ", e);
            }
        }
        
        pemFileData = sb.toString();

        logger.exit(this, methodName, pemFileData);

        return pemFileData;
    }
}
