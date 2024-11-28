/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.convertors;

import java.security.KeyStoreException;
import io.fabric8.kubernetes.api.model.Secret;
import hudson.Extension;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl;

/**
 * SecretToCredentialConvertor that converts {@link CertificateCredentialsImpl}.
 */
@Extension
public class CertificateCredentialsConvertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "certificate".equals(type);
    }

    @Override
    public CertificateCredentialsImpl convert(Secret secret) throws CredentialsConvertionException {
        // ensure we have some data
        SecretUtils.requireNonNull(secret.getData(), "certificate definition contains no data");

        String passwordBase64 = SecretUtils.getNonNullSecretData(secret, "password", "certificate credential is missing the password entry");

        String password = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(passwordBase64), "certificate credential has an invalid password (must be base64 encoded UTF-8)");

        String certBase64 = SecretUtils.getNonNullSecretData(secret, "certificate", "certificate credential is missing the certificate entry");

        byte[] certData = SecretUtils.requireNonNull(SecretUtils.base64Decode(certBase64), "certificate credential has an invalid certificate (must be base64 encoded data)");
        SecretBytes sb = SecretBytes.fromBytes(certData);

        try {
            CertificateCredentialsImpl certificateCredentialsImpl = new CertificateCredentialsImpl(SecretUtils.getCredentialScope(secret), SecretUtils.getCredentialId(secret), SecretUtils.getCredentialDescription(secret), password, new CertificateCredentialsImpl.UploadedKeyStoreSource(sb));
            if (certificateCredentialsImpl.getKeyStore().size() == 0) {
                throw new CredentialsConvertionException("certificate credential has an invalid certificate (encoded data is not a valid PKCS#12 format certificate understood by Java)");
            }
            return certificateCredentialsImpl;
        } catch (KeyStoreException | IllegalArgumentException ex) {
            throw new CredentialsConvertionException("certificate credential has an invalid certificate (encoded data is not a valid PKCS#12 format certificate understood by Java - " + ex.getMessage() + " )");
        }
    }

}
