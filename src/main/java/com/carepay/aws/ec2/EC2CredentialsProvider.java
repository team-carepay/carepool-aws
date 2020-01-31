package com.carepay.aws.ec2;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.carepay.aws.Credentials;
import com.carepay.aws.CredentialsProvider;
import com.carepay.aws.util.URLOpener;

import static java.time.ZoneOffset.UTC;

/**
 * EC2 implementation of AWS credentials provider. (using http://169.254.169.254/latest/meta-data)
 */
public class EC2CredentialsProvider implements CredentialsProvider {
    static final String SECURITY_CREDENTIALS_URL = "http://169.254.169.254/latest/meta-data/iam/security-credentials/";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private final EC2MetaData ec2metadata;
    private final Clock clock;

    private URL url;
    private Credentials lastCredentials;
    private LocalDateTime expiryDate;

    public EC2CredentialsProvider() {
        this(new EC2MetaData(URLOpener.DEFAULT), Clock.systemUTC());
    }

    public EC2CredentialsProvider(final EC2MetaData ec2metadata, final Clock clock) {
        this.ec2metadata = ec2metadata;
        this.clock = clock;
    }

    @Override
    public Credentials getCredentials() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), UTC);
        if (url == null) { // first time we need to determine the IAM role used by EC2
            try {
                final URL securityCredentialsUrl = new URL(SECURITY_CREDENTIALS_URL);
                final String role = ec2metadata.queryMetaDataAsString(securityCredentialsUrl);
                if (role != null) {
                    this.url = new URL(securityCredentialsUrl, role);
                } else {
                    return null;
                }
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        if ((expiryDate == null || expiryDate.isBefore(now))) {
            Map<String, String> map = ec2metadata.queryMetaData(url);
            lastCredentials = new Credentials(map.get("AccessKeyId"), map.get("SecretAccessKey"), map.get("Token"));
            final String expirationString = map.get("Expiration");
            if (lastCredentials.isValid() && expirationString != null) {
                expiryDate = LocalDateTime.parse(expirationString, DATE_FORMATTER);
            }
        }
        return lastCredentials;
    }
}
