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

package com.intel.bkp.core.psgcertificate;

import com.intel.bkp.core.psgcertificate.exceptions.PsgCertificateChainWrongSizeException;
import com.intel.bkp.core.psgcertificate.exceptions.PsgInvalidLeafCertificateException;
import com.intel.bkp.core.psgcertificate.exceptions.PsgInvalidParentCertificatesException;
import com.intel.bkp.core.psgcertificate.exceptions.PsgInvalidRootCertificateException;
import com.intel.bkp.core.psgcertificate.exceptions.PsgInvalidSignatureException;
import com.intel.bkp.core.psgcertificate.model.CertificateEntryWrapper;
import com.intel.bkp.core.psgcertificate.model.PsgCertificateType;
import com.intel.bkp.core.psgcertificate.model.PsgCurveType;
import com.intel.bkp.core.psgcertificate.model.PsgPublicKeyMagic;
import com.intel.bkp.core.psgcertificate.model.PsgSignatureCurveType;
import com.intel.bkp.crypto.constants.CryptoConstants;
import com.intel.bkp.crypto.curve.EcSignatureAlgorithm;
import com.intel.bkp.test.CertificateUtils;
import com.intel.bkp.test.KeyGenUtils;
import com.intel.bkp.test.SigningUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import static com.intel.bkp.core.psgcertificate.model.PsgSignatureCurveType.SECP256R1;
import static com.intel.bkp.core.psgcertificate.model.PsgSignatureCurveType.SECP384R1;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class PsgCertificateHelperTest {

    private final PsgCertificateHelper sut = new PsgCertificateHelper();

    @Test
    void getCertificateChainList_properlyParseCertificate() {
        // given
        String certChain = """
            klQJFwAAAQEAAAB4AAAAcQAAAAAAAAAAQGVmQwAAAGAAAAAAVDJmSAAAAAAAAAAAya2F1W0d3j9A
            DJVlaI+eTxVZ3s/bo3DUqecSOuolIADT3kRuuXcUjxgTH0PLY9083YSjvQW3qnzRPAHxGy2rztM+
            fsAcEEOlxDjtDsEuo3iyxZthzsp37GB3vwtqdZ1ydIgVIAAAADEAAAAwMFSIIACyX7rxh6JYJl1w
            DD5CF1Yb82Y4nq+3XhxObBKX5AlXURKVnB1BcHEyCoj7oUJPi05zwUiFh3P4f/IOAUcTNGtsGz+b
            EwcAKjvWpiKV3dMzhys+mm8SZtejGckyVioytO+JJZA2AAAAmAAAAIAAAAAAAAAAAAAAAAAAAAAA
            AAAAAJMQURgAAAAwAAAAMFQyZkgAAAAAAAAAAPbj1fueGTJ+o+7059WPUiaeAug0Q09BxXIiunnN
            pcqBXj95VWv75oZCW8JFxZ/I0AwWDUOBAXmogSc8SCNXe0NHJfjRSeGhT8lowHNttjsU1YUPn4ZU
            nI05E6yfsZ0DNw==""";
        // when
        List<CertificateEntryWrapper> certificateChainList = sut.getCertificateChainList(certChain);
        // then
        assertEquals(2, certificateChainList.size());
        assertEquals(PsgCertificateType.LEAF, certificateChainList.get(0).getType());
        assertEquals(PsgCertificateType.ROOT, certificateChainList.get(1).getType());
    }

    @Test
    void getCertificateChainList_WithNoMagicNumbers_properlyParseCertificate() {
        // given
        String certChain = "6r6S8bSqOGWlG2s5Iaa=";
        // when
        List<CertificateEntryWrapper> certificateChainList = sut.getCertificateChainList(certChain);
        // then
        assertEquals(0, certificateChainList.size());
    }

    @Test
    void verifyParentsInChainByPubKey_succeedsIfNoError() throws PsgInvalidParentCertificatesException,
        PsgInvalidSignatureException {
        // given
        KeyPair rootKeyPair = KeyGenUtils.genEc384();
        KeyPair leafKeyPair = KeyGenUtils.genEc384();
        KeyPair leafSecondKeyPair = KeyGenUtils.genEc384();

        assert rootKeyPair != null;
        assert leafKeyPair != null;
        assert leafSecondKeyPair != null;

        List<CertificateEntryWrapper> certificateChainList = new LinkedList<>();

        byte[] rootContent = new PsgCertificateRootEntryBuilder()
            .publicKey(getPsgPublicKeyBuilder(rootKeyPair, PsgCurveType.SECP384R1))
            .build()
            .array();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.ROOT, rootContent));

        byte[] leafContent = new PsgCertificateEntryBuilder()
            .withSignature(getPsgSignatureBuilder(PsgSignatureCurveType.SECP384R1))
            .publicKey(getPsgPublicKeyBuilder(leafKeyPair, PsgCurveType.SECP384R1))
            .signData(dataToSign -> SigningUtils.signEcData(
                dataToSign, rootKeyPair.getPrivate(), CryptoConstants.SHA384_WITH_ECDSA
            ), SECP384R1)
            .build()
            .array();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, leafContent));

        byte[] leafSecondContent = new PsgCertificateEntryBuilder()
            .withSignature(getPsgSignatureBuilder(PsgSignatureCurveType.SECP384R1))
            .publicKey(getPsgPublicKeyBuilder(leafSecondKeyPair, PsgCurveType.SECP384R1))
            .signData(dataToSign -> SigningUtils.signEcData(
                dataToSign, leafKeyPair.getPrivate(), CryptoConstants.SHA384_WITH_ECDSA
            ), SECP384R1)
            .build()
            .array();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, leafSecondContent));

        // when
        new PsgCertificateHelper().verifyParentsInChainByPubKey(certificateChainList);
    }

    @Test
    void verifyParentsInChainByPubKey_WithEc256_succeedsIfNoError()
        throws PsgInvalidParentCertificatesException, PsgInvalidSignatureException {
        // given
        KeyPair rootKeyPair = KeyGenUtils.genEc256();
        KeyPair leafKeyPair = KeyGenUtils.genEc256();
        KeyPair leafSecondKeyPair = KeyGenUtils.genEc256();

        assert rootKeyPair != null;
        assert leafKeyPair != null;
        assert leafSecondKeyPair != null;

        List<CertificateEntryWrapper> certificateChainList = new LinkedList<>();

        byte[] rootContent = new PsgCertificateRootEntryBuilder()
            .publicKey(getPsgPublicKeyBuilder(rootKeyPair, PsgCurveType.SECP256R1))
            .build()
            .array();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.ROOT, rootContent));

        byte[] leafContent = new PsgCertificateEntryBuilder()
            .withSignature(getPsgSignatureBuilder(PsgSignatureCurveType.SECP256R1))
            .publicKey(getPsgPublicKeyBuilder(leafKeyPair, PsgCurveType.SECP256R1))
            .signData(dataToSign -> SigningUtils.signEcData(
                dataToSign, rootKeyPair.getPrivate(), CryptoConstants.SHA256_WITH_ECDSA
            ), SECP256R1)
            .build()
            .array();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, leafContent));

        byte[] leafSecondContent = new PsgCertificateEntryBuilder()
            .withSignature(getPsgSignatureBuilder(PsgSignatureCurveType.SECP256R1))
            .publicKey(getPsgPublicKeyBuilder(leafSecondKeyPair, PsgCurveType.SECP256R1))
            .signData(dataToSign -> SigningUtils.signEcData(
                dataToSign, leafKeyPair.getPrivate(), CryptoConstants.SHA256_WITH_ECDSA
            ), SECP256R1)
            .build()
            .array();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, leafSecondContent));

        // when
        new PsgCertificateHelper().verifyParentsInChainByPubKey(certificateChainList);
    }

    @Test
    void verifyParentsInChainByPubKey_withInvalidParent_throwException() {
        // given
        KeyPair rootKeyPair = KeyGenUtils.genEc384();
        KeyPair leafKeyPair = KeyGenUtils.genEc384();
        assert rootKeyPair != null;
        assert leafKeyPair != null;

        List<CertificateEntryWrapper> certificateChainList = new LinkedList<>();

        byte[] rootContent = new PsgCertificateRootEntryBuilder()
            .publicKey(getPsgPublicKeyBuilder(rootKeyPair, PsgCurveType.SECP384R1))
            .build()
            .array();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.ROOT, rootContent));

        byte[] leafContent = new PsgCertificateEntryBuilder()
            .withSignature(getPsgSignatureBuilder(PsgSignatureCurveType.SECP384R1))
            .publicKey(getPsgPublicKeyBuilder(leafKeyPair, PsgCurveType.SECP384R1))
            .signData(dataToSign -> SigningUtils.signEcData(
                dataToSign, leafKeyPair.getPrivate(), CryptoConstants.SHA384_WITH_ECDSA
            ), SECP384R1)
            .build()
            .array();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, leafContent));

        assertThrows(PsgInvalidParentCertificatesException.class,
            () -> sut.verifyParentsInChainByPubKey(certificateChainList));
    }

    @Test
    void verifyParentsInChainByPubKey_withWrongSignature_throwException() {
        try (final var mock = MockitoAnnotations.openMocks(this)) {
            // given
            PsgCertificateHelper spy = spy(sut);
            doThrow(new PsgInvalidSignatureException("Failed to check signature", new Exception()))
                .when(spy).sigVerify(any(), any());
            KeyPair rootKeyPair = KeyGenUtils.genEc384();
            KeyPair leafKeyPair = KeyGenUtils.genEc384();

            assert rootKeyPair != null;
            assert leafKeyPair != null;
            List<CertificateEntryWrapper> certificateChainList = new LinkedList<>();
            byte[] leafContent = new PsgCertificateEntryBuilder()
                .withSignature(getPsgSignatureBuilder(PsgSignatureCurveType.SECP384R1))
                .publicKey(getPsgPublicKeyBuilder(leafKeyPair, PsgCurveType.SECP384R1))
                .signData(dataToSign -> SigningUtils.signEcData(
                    dataToSign, leafKeyPair.getPrivate(), CryptoConstants.SHA384_WITH_ECDSA
                ), SECP384R1)
                .build()
                .array();
            certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, leafContent));

            byte[] rootContent = new PsgCertificateRootEntryBuilder()
                .publicKey(getPsgPublicKeyBuilder(rootKeyPair, PsgCurveType.SECP384R1))
                .build()
                .array();
            certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.ROOT, rootContent));

            // when
            assertThrows(PsgInvalidSignatureException.class,
                () -> spy.verifyParentsInChainByPubKey(certificateChainList));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void verifyChainListSize_withOneCertificate_throwsException() {
        // given
        List<CertificateEntryWrapper> certificateChainList = new LinkedList<>();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.ROOT, new byte[4]));

        assertThrows(PsgCertificateChainWrongSizeException.class,
            () -> sut.verifyChainListSizeInternal(certificateChainList));
    }

    @Test
    void verifyChainListSize_withFourCertificates_throwsException() {
        // given
        List<CertificateEntryWrapper> certificateChainList = new LinkedList<>();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, new byte[4]));
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, new byte[4]));
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, new byte[4]));
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.ROOT, new byte[4]));

        assertThrows(PsgCertificateChainWrongSizeException.class,
            () -> sut.verifyChainListSizeInternal(certificateChainList));
    }

    @Test
    void verifyChainListSizeInternal_withThreeCertificatesAndTwoRootCerts_throwsException() {
        // given
        List<CertificateEntryWrapper> certificateChainList = new LinkedList<>();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, new byte[4]));
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.ROOT, new byte[4]));
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.ROOT, new byte[4]));

        assertThrows(PsgCertificateChainWrongSizeException.class,
            () -> sut.verifyChainListSizeInternal(certificateChainList));
    }

    @Test
    void verifyChainListSizeInternal_notThrowsAnything() throws PsgCertificateChainWrongSizeException {
        // given
        List<CertificateEntryWrapper> certificateChainList = new LinkedList<>();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, new byte[4]));
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.ROOT, new byte[4]));

        // when
        sut.verifyChainListSizeInternal(certificateChainList);
    }

    @Test
    void verifyRootCertificateInternal_notThrowsAnything()
        throws PsgCertificateChainWrongSizeException, PsgInvalidRootCertificateException {
        // given
        KeyPair rootKeyPair = KeyGenUtils.genEc384();
        assert rootKeyPair != null;
        List<CertificateEntryWrapper> certificateChainList = new LinkedList<>();
        byte[] rootContent = new PsgCertificateRootEntryBuilder()
            .publicKey(getPsgPublicKeyBuilder(rootKeyPair, PsgCurveType.SECP384R1))
            .build()
            .array();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.ROOT, rootContent));

        // when
        sut.verifyRootCertificateInternal(certificateChainList, rootContent);

    }

    @Test
    void verifyRootCertificateInternal_withNoRootCert_throwsException() {
        // given
        List<CertificateEntryWrapper> certificateChainList = new LinkedList<>();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, new byte[4]));

        assertThrows(PsgCertificateChainWrongSizeException.class,
            () -> sut.verifyRootCertificateInternal(certificateChainList, new byte[4]));
    }

    @Test
    void verifyRootCertificateInternal_withIncorrectCert_throwsException() {
        // given
        KeyPair rootKeyPair = KeyGenUtils.genEc384();
        KeyPair leafKeyPair = KeyGenUtils.genEc384();
        assert rootKeyPair != null;
        assert leafKeyPair != null;
        List<CertificateEntryWrapper> certificateChainList = new LinkedList<>();
        byte[] rootContent = new PsgCertificateRootEntryBuilder()
            .publicKey(getPsgPublicKeyBuilder(rootKeyPair, PsgCurveType.SECP384R1))
            .build()
            .array();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.ROOT, rootContent));
        byte[] wrongRootContent = new PsgCertificateRootEntryBuilder()
            .publicKey(getPsgPublicKeyBuilder(leafKeyPair, PsgCurveType.SECP384R1))
            .build()
            .array();

        assertThrows(PsgInvalidRootCertificateException.class,
            () -> sut.verifyRootCertificateInternal(certificateChainList, wrongRootContent));
    }

    @Test
    void sigVerify_withPublicKey_Success() throws PsgInvalidSignatureException {
        // given
        KeyPair keyPair = KeyGenUtils.genEc384();
        X509Certificate x509Certificate = CertificateUtils.generateCertificate(keyPair);
        byte[] testData = "TestDataToSignAndVerify".getBytes();
        byte[] signed = SigningUtils.signEcData(testData, keyPair.getPrivate(), CryptoConstants.SHA384_WITH_ECDSA);
        PsgSignatureBuilder psgSignature = new PsgSignatureBuilder().signature(signed, SECP384R1);

        // when
        boolean verify = PsgCertificateHelper.sigVerify(
            EcSignatureAlgorithm.ECDSA_P384, x509Certificate.getPublicKey(), testData, psgSignature.getCurvePoint()
        );

        // then
        assertTrue(verify);
    }

    @Test
    void sigVerify_withInvalidCertificateOrder_ReturnsFalse() throws PsgInvalidSignatureException {
        // given
        PsgCertificateCommon child = mock(PsgCertificateCommon.class);
        PsgCertificateCommon parent = mock(PsgCertificateCommon.class);
        PsgCertificateHelper spy = spy(sut);

        // when
        boolean result = spy.sigVerify(child, parent);

        // then
        assertFalse(result);
    }

    @Test
    void parseRootCertificate_ThrowsException() {
        assertThrows(PsgInvalidRootCertificateException.class, () -> sut.parseRootCertificate(new byte[4]));
    }

    @Test
    void findLeafCertificateInChain_Success()
        throws PsgCertificateChainWrongSizeException, PsgInvalidLeafCertificateException {
        // given
        KeyPair leafKeyPair = KeyGenUtils.genEc384();
        KeyPair leafKeyPairSecond = KeyGenUtils.genEc384();

        assert leafKeyPair != null;
        assert leafKeyPairSecond != null;
        List<CertificateEntryWrapper> certificateChainList = new LinkedList<>();
        byte[] leafContent = new PsgCertificateEntryBuilder()
            .withSignature(getPsgSignatureBuilder(PsgSignatureCurveType.SECP384R1))
            .publicKey(getPsgPublicKeyBuilder(leafKeyPair, PsgCurveType.SECP384R1))
            .signData(dataToSign -> SigningUtils.signEcData(
                dataToSign, leafKeyPair.getPrivate(), CryptoConstants.SHA384_WITH_ECDSA
            ), SECP384R1)
            .build()
            .array();
        PsgCertificateEntry leafContentSecond = new PsgCertificateEntryBuilder()
            .withSignature(getPsgSignatureBuilder(PsgSignatureCurveType.SECP384R1))
            .publicKey(getPsgPublicKeyBuilder(leafKeyPairSecond, PsgCurveType.SECP384R1))
            .signData(dataToSign -> SigningUtils.signEcData(
                dataToSign, leafKeyPairSecond.getPrivate(), CryptoConstants.SHA384_WITH_ECDSA
            ), SECP384R1)
            .build();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, leafContent));
        certificateChainList
            .add(new CertificateEntryWrapper(PsgCertificateType.LEAF, leafContentSecond.array()));

        // when
        final PsgCertificateEntryBuilder leafCertificateInChain = sut.findLeafCertificateInChain(certificateChainList);

        // then
        assertArrayEquals(leafContentSecond.array(), leafCertificateInChain.build().array());
    }

    @Test
    void findLeafCertificateInChain_WithEmptyArray_ThrowsException() {
        // then
        assertThrows(PsgCertificateChainWrongSizeException.class,
            () -> sut.findLeafCertificateInChain(new LinkedList<>()));
    }

    @Test
    void findLeafCertificateInChain_WithWrongCertificate_ThrowsException() {
        // given
        final List<CertificateEntryWrapper> certificateChainList = new LinkedList<>();
        certificateChainList.add(new CertificateEntryWrapper(PsgCertificateType.LEAF, new byte[5]));

        assertThrows(PsgInvalidLeafCertificateException.class,
            () -> sut.findLeafCertificateInChain(certificateChainList));
    }

    private PsgPublicKeyBuilder getPsgPublicKeyBuilder(KeyPair keyPair, PsgCurveType psgCurveType) {
        return new PsgPublicKeyBuilder()
            .magic(PsgPublicKeyMagic.M1_MAGIC)
            .publicKey(keyPair.getPublic(), psgCurveType);
    }

    private PsgSignatureBuilder getPsgSignatureBuilder(PsgSignatureCurveType psgSignatureCurveType) {
        return PsgSignatureBuilder.empty(psgSignatureCurveType);
    }
}
