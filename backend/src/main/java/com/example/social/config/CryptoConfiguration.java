package com.example.social.config;

import com.example.social.common.crypto.AesGcmStringConverter;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class CryptoConfiguration {
    @Bean
    static BeanFactoryPostProcessor configureJpaEncryption(Environment environment) {
        String encodedKey = environment.getRequiredProperty("app.encryption.key-base64");
        return beanFactory -> AesGcmStringConverter.configure(encodedKey);
    }
}
