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

package com.intel.bkp.crypto.aesctr;

import com.intel.bkp.crypto.exceptions.EncryptionProviderException;
import com.intel.bkp.crypto.interfaces.IEncryptionProvider;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.ProviderException;
import java.util.Optional;

public abstract class AesCounterModeProvider implements IEncryptionProvider {

    protected abstract SecretKey getSecretKey();

    protected abstract Provider getProvider();

    protected abstract String getCipherType();

    protected abstract IIvProvider getIvProvider();

    public byte[] encrypt(byte[] data) throws EncryptionProviderException {
        return perform(data, Cipher.ENCRYPT_MODE);
    }

    public byte[] decrypt(byte[] data) throws EncryptionProviderException {
        return perform(data, Cipher.DECRYPT_MODE);
    }

    private byte[] perform(byte[] data, int mode) throws EncryptionProviderException {
        SecretKey key = getSecretKeyInternal();

        try {
            final Cipher cipher = Cipher.getInstance(getCipherTypeInternal(), getProviderInternal());
            cipher.init(mode, key, new IvParameterSpec(getIvProviderInternal().generate()));
            return cipher.doFinal(data);
        } catch (ProviderException | NoSuchAlgorithmException | InvalidKeyException
            | InvalidAlgorithmParameterException | BadPaddingException | NoSuchPaddingException
            | IllegalBlockSizeException e) {
            if (mode == Cipher.ENCRYPT_MODE) {
                throw new EncryptionProviderException("AES CTR encryption failed.", e);
            } else {
                throw new EncryptionProviderException("AES CTR decryption failed.", e);
            }
        }
    }

    private SecretKey getSecretKeyInternal() throws EncryptionProviderException {
        return Optional.ofNullable(getSecretKey())
            .orElseThrow(() -> new EncryptionProviderException("Key is not set."));
    }

    private Provider getProviderInternal() throws EncryptionProviderException {
        return Optional.ofNullable(getProvider())
            .orElseThrow(() -> new EncryptionProviderException("Provider is not set."));
    }

    private IIvProvider getIvProviderInternal() throws EncryptionProviderException {
        return Optional.ofNullable(getIvProvider())
            .orElseThrow(() -> new EncryptionProviderException("IV provider is not set."));
    }

    private String getCipherTypeInternal() throws EncryptionProviderException {
        return Optional.ofNullable(getCipherType())
            .filter(s -> !s.isEmpty() && !s.isBlank())
            .orElseThrow(() -> new EncryptionProviderException("Cipher type is not set."));
    }
}
