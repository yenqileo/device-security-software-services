/*
 * This project is licensed as below.
 *
 * **************************************************************************
 *
 * Copyright 2020-2025 Altera Corporation. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * **************************************************************************
 */

package com.intel.bkp.crypto.x509.validation;

import com.intel.bkp.test.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ValidityVerifierTest {

    // openssl req -newkey rsa:2048 -nodes -keyout CAkey.pem -x509 -days 365 -out CA.pem
    // openssl req -new -newkey rsa:2048 -nodes -keyout invalid.key -out invalid.csr
    // openssl x509 -req -days 0 -in invalid.csr -CA CA.pem -CAkey CAkey.pem -CAcreateserial -out invalid.pem -sha256
    private static final String INVALID_CERT = "invalid.pem";
    private static final String ROOT_CERT_FILENAME = "IPCS.cer";

    private static X509Certificate invalidCert;
    private static X509Certificate rootCert;

    @InjectMocks
    private ValidityVerifier sut;

    @BeforeAll
    static void init() {
        invalidCert = FileUtils.loadCertificate(INVALID_CERT);
        rootCert = FileUtils.loadCertificate(ROOT_CERT_FILENAME);
    }

    @Test
    void verify_ValidCert_ReturnsTrue() {
        // when-then
        assertTrue(sut.verify(rootCert));
    }

    @Test
    void verify_ExpiredCert_ReturnsFalse() {
        // when-then
        assertFalse(sut.verify(invalidCert));
    }
}
