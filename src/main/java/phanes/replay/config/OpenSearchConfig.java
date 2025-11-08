package phanes.replay.config;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContexts;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.host}")
    private String host;
    @Value("${opensearch.port}")
    private int port;
    @Value("${opensearch.username}")
    private String username;
    @Value("${opensearch.password}")
    private String password;
    @Value("${opensearch.ssl:false}")
    private boolean ssl;
    @Value("${opensearch.truststore.path}")
    private String trustStorePath;
    @Value("${opensearch.truststore.password}")
    private String trustStorePassword;

    @Bean
    public OpenSearchClient openSearchClient() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
        final ApacheHttpClient5TransportBuilder transportBuilder = transportBuilder();
        final OpenSearchTransport transport = transportBuilder.build();
        return new OpenSearchClient(transport);
    }

    private ApacheHttpClient5TransportBuilder transportBuilder() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
        final HttpHost opensearchHost = new HttpHost("https", host, port);
        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(opensearchHost),
                new UsernamePasswordCredentials(username, password.toCharArray())
        );
        final ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(opensearchHost);
        if (ssl) {
            SSLContext sslContext = sslContext();
            final DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE);
            final PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                    .setTlsStrategy(tlsStrategy)
                    .build();
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setConnectionManager(connectionManager)
            );
        } else {
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            );
        }
        return builder;
    }

    private SSLContext sslContext() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, KeyManagementException {
        if (trustStorePath != null && !trustStorePath.isEmpty()) {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (InputStream is = Files.newInputStream(Paths.get(trustStorePath))) {
                trustStore.load(is, trustStorePassword.toCharArray());
            }
            return SSLContexts.custom()
                    .loadTrustMaterial(trustStore, null)
                    .build();
        }
        return SSLContexts.custom()
                .loadTrustMaterial(null, new TrustAllStrategy())
                .build();
    }
}