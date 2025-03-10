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

package com.intel.bkp.verifier.interfaces;

import com.intel.bkp.verifier.model.dto.VerifierExchangeResponseDTO;

public interface VerifierExchange {

    /**
     * Create device attestation sub key.
     *
     * @param transportId device identifier config for communication specific to transport layer:
     *     "host:[HPS hostname or IP]; port:[HPS port number]"
     *     i.e. "host:127.0.0.1; port:50001"
     * @param context random hex value provided as seed to SDM and cached by Verifier, max 28 bytes length
     * @param pufType enum string value
     *
     * @return result where 0 is SUCCESS, -1 is ERROR
     */
    int createDeviceAttestationSubKey(String transportId, String context, String pufType);

    /**
     * Get device attestation.
     *
     * @param transportId device identifier config for communication specific to transport layer:
     *     "host:[HPS hostname or IP]; port:[HPS port number]"
     *     i.e. "host:127.0.0.1; port:50001"
     * @param refMeasurementHex hex content of Reference Integrity Manifest (RIM) file,
     *     which describes which part of evidence received from device should match with provided reference evidence
     *
     * @return DTO with result where 0 is SUCCESS, 1 is FAIL, -1 is ERROR along with deviceId of the platform attested
     */
    VerifierExchangeResponseDTO getDeviceAttestation(String transportId, String refMeasurementHex);

    /**
     * Perform health check to test chosen transfer layer implementation.
     *
     * @param transportId device identifier config for communication specific to transport layer:
     *     "host:[HPS hostname or IP]; port:[HPS port number]"
     *     i.e. "host:127.0.0.1; port:50001"
     *
     * @return result where 0 is SUCCESS, -1 is ERROR
     */
    int healthCheck(String transportId);
}
