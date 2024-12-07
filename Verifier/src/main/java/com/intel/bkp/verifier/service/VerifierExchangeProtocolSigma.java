/*
 * This project is licensed as below.
 *
 * **************************************************************************
 *
 * Copyright 2020-2024 Intel Corporation. All Rights Reserved.
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

package com.intel.bkp.verifier.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// NONPUBLIC
import static com.intel.bkp.utils.HexConverter.toHex;
import com.intel.bkp.verifier.model.dto.VerifierExchangeResponseDTO;
import com.intel.bkp.verifier.transport.model.TransportLayer;
import com.intel.bkp.core.manufacturing.model.PufType;
import com.intel.bkp.verifier.validators.ParameterValidator;
import org.apache.commons.lang3.StringUtils;
import static com.intel.bkp.verifier.model.VerifierExchangeResponse.ERROR;
import static com.intel.bkp.verifier.model.VerifierExchangeResponse.OK;
// NONPUBLIC-END

@Slf4j
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class VerifierExchangeProtocolSigma extends VerifierExchangeProtocol {
    // NONPUBLIC
    private final ParameterValidator parameterValidator = new ParameterValidator();
    private final InitSessionComponent initSessionComponent;
    private final CreateDeviceAttestationSubKeyComponent createDeviceAttestationSubKeyComponent;
    private final GetDeviceAttestationComponent getDeviceAttestationComponent;

    VerifierExchangeProtocolSigma() {
        this.initSessionComponent = new InitSessionComponent();
        this.createDeviceAttestationSubKeyComponent = new CreateDeviceAttestationSubKeyComponent();
        this.getDeviceAttestationComponent = new GetDeviceAttestationComponentSigma();
    }

    @Override
    public int createSubKeyInternal(String context, PufType pufType) {
        // this check is required to prevent SQL Injection
        if (!parameterValidator.validateContext(context)) {
            return ERROR.getCode();
        }

        try {
            final byte[] deviceId = initSessionComponent.initializeSessionForDeviceId();
            log.info("Creating attestation subkey will be performed for device of id: {}", toHex(deviceId));

            return createDeviceAttestationSubKeyComponent.perform(context, pufType, deviceId).getCode();
        } catch (Exception e) {
            log.error("Failed to perform creating of attestation subkey: {}", e.getMessage());
            log.debug("Stacktrace: ", e);
            return ERROR.getCode();
        }
    }

    @Override
    public VerifierExchangeResponseDTO getAttestationInternal(String refMeasurementHex) {
        final VerifierExchangeResponseDTO response = new VerifierExchangeResponseDTO();

        try {
            final byte[] deviceId = initSessionComponent.initializeSessionForDeviceId();
            response.setDeviceId(toHex(deviceId));
            log.info("Platform attestation will be performed for device of id: {}", toHex(deviceId));

            response.setStatus(getDeviceAttestationComponent.perform(refMeasurementHex, deviceId).getCode());
        } catch (Exception e) {
            log.error("Failed to perform platform attestation: {}", e.getMessage());
            log.debug("Stacktrace: ", e);
            response.setStatus(ERROR.getCode());
        }
        return response;
    }

    @Override
    public int healthCheckInternal(TransportLayer transportLayer) {
        try {
            final String result = toHex(transportLayer.sendCommand(getChipID));
            log.info("Health check response: {}", result);
            return StringUtils.isBlank(result)
                    ? ERROR.getCode()
                    : OK.getCode();
        } catch (Exception e) {
            log.error("Failed to perform health check using GET_CHIPID command: {}", e.getMessage());
            log.debug("Stacktrace: ", e);
            return ERROR.getCode();
        }
    }
    // NONPUBLIC-END
}
