package co.fareye.microservicemanager.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class S3UrlService implements EnvironmentAware {

    private final static Logger log = LoggerFactory.getLogger(S3UrlService.class);

    private RelaxedPropertyResolver s3Properties;
    private RelaxedPropertyResolver googleProperties;


    @Override
    public void setEnvironment(Environment environment) {
        this.s3Properties = new RelaxedPropertyResolver(environment, "app.s3.");
        this.googleProperties = new RelaxedPropertyResolver(environment, "app.google.");

    }

    public String getS3MicroServiceBucketName() {
        return s3Properties.getProperty("microServiceBucketName", "fareye.releases");
    }

    public String getGoogleAccessKeyFilePath(){
        return googleProperties.getProperty("accessKeyFilePath");
    }


    public String getS3AccessKey() {
        return s3Properties.getProperty("accessKey", "");
    }

    public String getS3SecretKey() {
        return s3Properties.getProperty("secretKey", "");
    }



}

