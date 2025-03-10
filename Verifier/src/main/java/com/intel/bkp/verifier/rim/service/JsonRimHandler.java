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

package com.intel.bkp.verifier.rim.service;

import com.intel.bkp.fpgacerts.dice.tcbinfo.MeasurementHolder;
import com.intel.bkp.fpgacerts.dice.tcbinfo.TcbInfoMeasurement;
import com.intel.bkp.fpgacerts.rim.IRimHandler;
import com.intel.bkp.verifier.rim.model.Rim;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static com.intel.bkp.utils.HexConverter.fromHex;

@Slf4j
@NoArgsConstructor
public class JsonRimHandler implements IRimHandler<String> {

    private final RimParser rimParser = new RimParser();
    private final RimToTcbInfoMeasurementsMapper rimMapper = new RimToTcbInfoMeasurementsMapper();

    @Override
    public String getFormatName() {
        return "JSON";
    }

    @Override
    public String parse(String hex) {
        return new String(fromHex(hex), StandardCharsets.UTF_8);
    }

    public MeasurementHolder getMeasurements(String json) {
        final Rim referenceMeasurement = rimParser.parse(json);
        final var holder = new MeasurementHolder();
        holder.setReferenceMeasurements(getReferenceMeasurements(referenceMeasurement));
        return holder;
    }

    private List<TcbInfoMeasurement> getReferenceMeasurements(Rim referenceMeasurement) {
        return Optional.of(referenceMeasurement)
            .map(rimMapper::map)
            .orElse(List.of());
    }
}

