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

package com.intel.bkp.bkps.config;

import com.intel.bkp.bkps.security.KeystoreManagerChooser;
import com.intel.bkp.core.exceptions.JceSecurityProviderException;
import com.intel.bkp.core.security.ISecurityProvider;
import com.intel.bkp.core.security.JceSecurityProvider;
import com.intel.bkp.core.security.SecurityProviderParamsSetter;
import com.intel.bkp.core.security.SecurityProviderParamsValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.security.Provider;
import java.security.Security;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class JceSecurityConfiguration {

    private final ApplicationProperties properties;

    @Bean
    public ISecurityProvider securityService() {

        if (properties.getSecurityProviderParams() == null) {
            log.info("Security provider configuration is empty. Set default values");
            properties.setSecurityProviderParams(SecurityProviderParamsSetter.setDefaultSecurityProviderParams());
        }

        new SecurityProviderParamsValidator(properties.getSecurityProviderParams()).validateParams();
        log.info("Security provider configuration: {}", properties.getSecurityProviderParams().toString());

        initializeJceProvider(properties.getSecurityProviderParams().getProvider().getClassName());

        return new JceSecurityProvider(properties.getSecurityProviderParams(),
            () -> KeystoreManagerChooser.choose(properties.getSecurityProviderParams().getProvider().getFileBased())
        );
    }

    private void initializeJceProvider(String name) {
        try {
            Security.addProvider((Provider) Class.forName(name).getConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException
            | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            throw new JceSecurityProviderException("Failed to initialize security provider using class: " + name, e);
        }
    }
}
