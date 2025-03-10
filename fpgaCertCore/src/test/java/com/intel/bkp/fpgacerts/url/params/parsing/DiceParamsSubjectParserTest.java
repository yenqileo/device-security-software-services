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

package com.intel.bkp.fpgacerts.url.params.parsing;

import com.intel.bkp.fpgacerts.exceptions.InvalidDiceCertificateSubjectException;
import com.intel.bkp.fpgacerts.exceptions.X509Exception;
import com.intel.bkp.fpgacerts.url.params.DiceParams;
import com.intel.bkp.test.CertificateUtils;
import org.bouncycastle.asn1.x509.Extension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiceParamsSubjectParserTest {

    private static final String TEST_FOLDER = "certs/dice/";
    private static final String DEVICE_ID_ENROLLMENT_CERT = "device_id_enrollment_certificate.der";
    private static final String EXPECTED_SKI = "DI931bRmuixmLyW4WJYySeQiDaQ";
    private static final String EXPECTED_UID = "065effc1e44f3506";
    private static final String SKI_OID = Extension.subjectKeyIdentifier.getId();

    private static X509Certificate deviceIdEnrollmentCert;

    @Mock
    private static X509Certificate certificate;

    private final DiceParamsSubjectParser sut = DiceParamsSubjectParser.instance();

    @BeforeAll
    static void init() {
        deviceIdEnrollmentCert = CertificateUtils.readCertificate(TEST_FOLDER, DEVICE_ID_ENROLLMENT_CERT);
    }

    @Test
    void parse() {
        // when
        final DiceParams result = sut.parse(deviceIdEnrollmentCert);

        // then
        assertEquals(EXPECTED_SKI, result.getId());
        assertEquals(EXPECTED_UID, result.getUid());
    }

    @Test
    void parse_certWithoutSki_Throws() {
        // given
        when(certificate.getExtensionValue(SKI_OID)).thenReturn(null);

        // when-then
        assertThrows(X509Exception.class, () -> sut.parse(certificate));
    }

    @Test
    void parse_certWithSubjectDNThatIsNotInDiceFormat_Throws() {
        // given
        when(certificate.getExtensionValue(SKI_OID)).thenReturn(deviceIdEnrollmentCert.getExtensionValue(SKI_OID));
        mockSubject(certificate, "CN=ValidCommonName:ButNotInDiceFormat");

        // when-then
        assertThrows(InvalidDiceCertificateSubjectException.class, () -> sut.parse(certificate));
    }

    private void mockSubject(X509Certificate cert, String subject) {
        final X500Principal principal = mock(X500Principal.class);
        when(cert.getSubjectX500Principal()).thenReturn(principal);
        when(principal.getName()).thenReturn(subject);
    }
}
