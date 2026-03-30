/*
 * aSlicer - 3D model processing tool.
 * Copyright (C) 2026 cz.ad.print3d.aslicer contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.ad.print3d.aslicer.logic.core.security;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data using Bouncy Castle FIPS.
 * Uses AES-GCM for authenticated encryption.
 */
public class CryptoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoService.class);
    private static final String KEY_ALIAS = "aslicer-secret-key";
    private static final String KEYSTORE_TYPE = "BCFKS";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private static CryptoService instance;

    private final Path keyStorePath;
    private final char[] password;
    private SecretKey secretKey;

    /**
     * Gets the default CryptoService instance.
     *
     * @return the default CryptoService instance
     */
    public synchronized static CryptoService getInstance() {
        if (instance == null) {
            instance = new CryptoService();
        }
        return instance;
    }

    /**
     * Creates a CryptoService with default configuration path and password.
     */
    public CryptoService() {
        this(Paths.get(System.getProperty("user.home"), ".aslicer", "aslicer.bcfks"), "aslicer-fips-pwd".toCharArray());
    }

    /**
     * Creates a CryptoService with specified key store path and password.
     *
     * @param keyStorePath the path to the BCFKS key store
     * @param password     the password for the key store
     */
    public CryptoService(Path keyStorePath, char[] password) {
        this.keyStorePath = keyStorePath;
        this.password = password;
        init();
    }

    private void init() {
        SecurityInitializer.init();
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, BouncyCastleFipsProvider.PROVIDER_NAME);
            if (Files.exists(keyStorePath)) {
                try (InputStream is = Files.newInputStream(keyStorePath)) {
                    keyStore.load(is, password);
                }
            } else {
                Files.createDirectories(keyStorePath.getParent());
                keyStore.load(null, password);

                KeyGenerator keyGen = KeyGenerator.getInstance("AES", BouncyCastleFipsProvider.PROVIDER_NAME);
                keyGen.init(256);
                SecretKey key = keyGen.generateKey();

                keyStore.setKeyEntry(KEY_ALIAS, key, password, null);
                try (OutputStream os = Files.newOutputStream(keyStorePath)) {
                    keyStore.store(os, password);
                }
                LOGGER.info("Created new FIPS key store at {}", keyStorePath);
            }

            this.secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, password);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize CryptoService", e);
            throw new SecurityException("Could not initialize FIPS crypto service", e);
        }
    }

    /**
     * Encrypts the given plain text.
     *
     * @param plainText the text to encrypt
     * @return Base64 encoded cipher text with IV prepended, or null if input is null
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM, BouncyCastleFipsProvider.PROVIDER_NAME);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            LOGGER.error("Encryption failed", e);
            throw new SecurityException("Encryption failed", e);
        }
    }

    /**
     * Decrypts the given Base64 encoded cipher text.
     *
     * @param cipherTextBase64 the Base64 encoded cipher text with IV prepended
     * @return the decrypted plain text, or null if input is null
     */
    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherTextBase64);
            if (combined.length < IV_LENGTH_BYTE) {
                throw new IllegalArgumentException("Invalid cipher text");
            }

            byte[] iv = new byte[IV_LENGTH_BYTE];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            byte[] cipherText = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM, BouncyCastleFipsProvider.PROVIDER_NAME);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("Decryption failed", e);
            throw new SecurityException("Decryption failed", e);
        }
    }
}
