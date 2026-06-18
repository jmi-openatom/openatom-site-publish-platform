package cn.jmi.openatom.sitepublish.service;

import cn.jmi.openatom.sitepublish.config.SslProperties;
import org.springframework.stereotype.Component;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class TlsCertificateClient {

    public CertificateResult request(String domain, SslProperties properties) throws Exception {
        try (Socket connection = new Socket()) {
            connection.connect(
                    new InetSocketAddress(properties.proxyHost(), properties.proxyPort()),
                    properties.connectTimeoutSeconds() * 1_000
            );
            connection.setSoTimeout(properties.handshakeTimeoutSeconds() * 1_000);

            SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) socketFactory
                    .createSocket(connection, domain, properties.proxyPort(), true)) {
                SSLParameters parameters = socket.getSSLParameters();
                parameters.setServerNames(List.of(new SNIHostName(domain)));
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                socket.setSSLParameters(parameters);
                socket.startHandshake();

                X509Certificate certificate = (X509Certificate) socket.getSession()
                        .getPeerCertificates()[0];
                certificate.checkValidity();
                return new CertificateResult(
                        LocalDateTime.ofInstant(
                                certificate.getNotAfter().toInstant(),
                                ZoneId.systemDefault()
                        ),
                        certificate.getIssuerX500Principal().getName()
                );
            }
        }
    }

    public record CertificateResult(LocalDateTime expiresAt, String issuer) {
    }
}
