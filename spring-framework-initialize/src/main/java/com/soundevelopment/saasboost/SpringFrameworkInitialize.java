package com.soundevelopment.saasboost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SpringFrameworkInitialize implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringFrameworkInitialize.class);

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        LOGGER.info("executing ApplicationContextInitializer");
        ConfigurableEnvironment env = ctx.getEnvironment();

        String resourcesBucket = env.getRequiredProperty("SAAS_BOOST_RESOURCES_BUCKET");
        String tenantId = env.getRequiredProperty("TENANT_ID");

        Properties tenantProperties = loadTenantProperties(resourcesBucket, tenantId);
        LOGGER.info("Setting tenant config properties on the ApplicationContext");
        for (Map.Entry<Object, Object> property : tenantProperties.entrySet()) {
            LOGGER.info(property.getKey() + " => " + property.getValue());
        }
        ctx.getEnvironment().getPropertySources()
                .addFirst(new PropertiesPropertySource("tenant.properties", tenantProperties));
    }

    Properties loadTenantProperties(String resourcesBucket, String tenantId) {
        Properties tenantProperties = new Properties();
        S3Client s3 = S3Client.builder().build();
        try {
            String configFile = "tenants/" + tenantId + "/" + tenantId + ".zip";
            LOGGER.debug("Checking for existence of s3://" + resourcesBucket + "/" + configFile);
            s3.headObject(request -> request
                    .bucket(resourcesBucket)
                    .key(configFile)
            );
            LOGGER.debug("Calling get-object on s3://" + resourcesBucket + "/" + configFile);
            ResponseInputStream<GetObjectResponse> response = s3.getObject(request -> request
                    .bucket(resourcesBucket)
                    .key(configFile)
            );
            tenantProperties = unzip(response);
        } catch (NoSuchKeyException exists) {
            LOGGER.warn("No config file for tenant " + tenantId);
        } catch (S3Exception s3error) {
            String error = s3error.awsErrorDetails().errorMessage();
            LOGGER.error("Error loading config file from S3 " + s3error.getMessage());
            if (error != null) {
                LOGGER.error(error);
            }
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            s3error.printStackTrace(pw);
            LOGGER.error(sw.getBuffer().toString());
        }
        return tenantProperties;
    }

    Properties unzip(InputStream archive) {
        LOGGER.debug("unzipping tenant config archive");
        Properties properties = new Properties();
        try {
            ZipInputStream zip = new ZipInputStream(archive);
            byte[] buffer = new byte[1024];
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                if (entry.getName().endsWith(".properties")) {
                    LOGGER.debug("Extracting properties file " + entry.getName());
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int len;
                    while ((len = zip.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    int existingProperties = properties.size();
                    // Could get fancy here and use Piped I/O to avoid the memory copy
                    properties.load(new ByteArrayInputStream(baos.toByteArray()));
                    int newProperties = properties.size();
                    LOGGER.debug("Added " + (newProperties - existingProperties) + " properties from tenant config archive");
                } else {
                    LOGGER.debug("Skipping non properties file " + entry.getName());
                }
                entry = zip.getNextEntry();
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return properties;
    }
}