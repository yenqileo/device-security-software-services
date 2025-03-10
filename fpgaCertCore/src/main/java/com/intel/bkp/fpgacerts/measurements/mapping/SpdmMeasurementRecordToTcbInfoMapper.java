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

package com.intel.bkp.fpgacerts.measurements.mapping;

import com.intel.bkp.fpgacerts.dice.tcbinfo.TcbInfoMeasurement;
import com.intel.bkp.fpgacerts.exceptions.SectionTypeException;
import com.intel.bkp.fpgacerts.measurements.model.SectionType;
import com.intel.bkp.fpgacerts.measurements.model.SpdmDmtfMeasurementHeader;
import com.intel.bkp.fpgacerts.measurements.model.SpdmDmtfMeasurementRecordHeaderBuilder;
import com.intel.bkp.fpgacerts.measurements.model.SpdmMeasurementRecordHeader;
import com.intel.bkp.utils.ByteBufferSafe;
import com.intel.bkp.utils.ByteSwap;
import com.intel.bkp.utils.ByteSwapOrder;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SpdmMeasurementRecordToTcbInfoMapper
    extends BaseMeasurementRecordToTcbInfoMapper<SpdmMeasurementRecordHeader> {

    static final int PR_SECTION_INDEX_SHIFT = 0x40;
    private static final int DMTF_MEASUREMENT_FLAG = 1;

    @Override
    public Optional<TcbInfoMeasurement> map(SpdmMeasurementRecordHeader header, ByteBufferSafe recordContentBuffer) {
        log.debug("Parsing measurement: {}", header);
        if (isDmtfMeasurement(header)) {
            final var dmtfHeader = parseDmtfHeader(recordContentBuffer);
            try {
                final SectionType sectionType = SectionType.fromSpdmParameters(header.getIndex(), dmtfHeader.getType());
                return Optional.of(mapInternal(header, recordContentBuffer, sectionType));
            } catch (SectionTypeException e) {
                log.warn("Could not parse section - section unknown: {}", header);
            }
        } else {
            log.warn("Could not parse section - MeasurementSpec other than 1 is not supported: {}", header);
        }

        recordContentBuffer.skip(getMeasurementSize(header));
        return Optional.empty();
    }

    @Override
    protected int getPrSectionIndex(SpdmMeasurementRecordHeader header) {
        return ByteSwap.getSwappedInt(header.getIndex() - PR_SECTION_INDEX_SHIFT + 1, ByteSwapOrder.B2L);
    }

    @Override
    protected int getMeasurementSize(SpdmMeasurementRecordHeader header) {
        final short measurementSize = header.getMeasurementSize();
        return isDmtfMeasurement(header)
               ? measurementSize - SpdmDmtfMeasurementRecordHeaderBuilder.HEADER_SIZE
               : measurementSize;
    }

    private static boolean isDmtfMeasurement(SpdmMeasurementRecordHeader header) {
        return DMTF_MEASUREMENT_FLAG == header.getMeasurementSpec();
    }

    private static SpdmDmtfMeasurementHeader parseDmtfHeader(ByteBufferSafe recordContentBuffer) {
        return new SpdmDmtfMeasurementRecordHeaderBuilder()
            .parse(recordContentBuffer)
            .build();
    }

}
