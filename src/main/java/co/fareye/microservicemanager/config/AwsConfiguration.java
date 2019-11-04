package co.fareye.microservicemanager.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static com.amazonaws.services.s3.internal.Constants.MB;

@Configuration
public class AwsConfiguration implements EnvironmentAware {
    private final Logger log = LoggerFactory.getLogger(AwsConfiguration.class);
    private RelaxedPropertyResolver s3Properties;


    @Value("${app.s3.accessKey}")
    private String accessKey;

    @Value("${app.s3.secretKey}")
    private String secretKey;


    @Override
    public void setEnvironment(Environment environment) {
        this.s3Properties = new RelaxedPropertyResolver(environment, "app.s3.");
    }

    @Bean
    public AmazonS3 amazonS3(){
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);
        AmazonS3 s3Client;
        log.debug("Access Key: {}, Secret Key: {}", accessKey, secretKey);
        if(accessKey == null || accessKey.isEmpty() || secretKey ==null || secretKey.isEmpty()) {
            log.debug("IAM");
            s3Client = new AmazonS3Client(new InstanceProfileCredentialsProvider(), clientConfig);
        } else {
            log.debug("Credential");
            AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            s3Client = new AmazonS3Client(credentials, clientConfig);
        }
        return s3Client;
    }

    @Bean
    public TransferManager transferManager(){

        TransferManager tm = TransferManagerBuilder.standard()
                .withS3Client(amazonS3())
                .withDisableParallelDownloads(false)
                .withMinimumUploadPartSize(Long.valueOf(5 * MB))
                .withMultipartUploadThreshold(Long.valueOf(16 * MB))
                .withMultipartCopyPartSize(Long.valueOf(5 * MB))
                .withMultipartCopyThreshold(Long.valueOf(100 * MB))
                .withExecutorFactory(()->createExecutorService(20))
                .build();

        return tm;
    }

    private ThreadPoolExecutor createExecutorService(int threadNumber) {
        ThreadFactory threadFactory = new ThreadFactory() {
            private int threadCount = 1;

            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("jsa-amazon-s3-transfer-manager-worker-" + threadCount++);
                return thread;
            }
        };
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(threadNumber, threadFactory);
    }




}
