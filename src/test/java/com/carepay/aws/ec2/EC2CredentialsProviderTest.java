package com.carepay.aws.ec2;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.time.Instant;

import com.carepay.aws.auth.Credentials;
import org.junit.Before;
import org.junit.Test;

import static com.carepay.aws.ec2.EC2CredentialsProvider.SECURITY_CREDENTIALS_URL;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EC2CredentialsProviderTest {
    private EC2CredentialsProvider credentialsProvider;
    private ResourceFetcher ec2metadata;

    @Before
    public void setUp() throws IOException {
        Clock clock = Clock.fixed(Instant.parse("2015-08-30T12:36:00.00Z"), UTC);
        HttpURLConnection ucRole = mock(HttpURLConnection.class);
        when(ucRole.getInputStream()).thenReturn(getClass().getResourceAsStream("/security-credentials-role.txt"));
        HttpURLConnection ucJson = mock(HttpURLConnection.class);
        when(ucJson.getInputStream()).thenReturn(getClass().getResourceAsStream("/ec2-credentials.json"));
        ec2metadata = new ResourceFetcher(url -> SECURITY_CREDENTIALS_URL.equals(url.toString()) ? ucRole : ucJson);
        credentialsProvider = new EC2CredentialsProvider(ec2metadata, clock, envname -> null);
    }

    @Test
    public void testGetCredentials() {
        assertThat(credentialsProvider.getCredentials().getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
        assertThat(credentialsProvider.getCredentials().getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
    }

    @Test
    public void testExpiry() throws IOException {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.parse("2020-08-30T12:36:00.00Z"));
        HttpURLConnection ucRole = mock(HttpURLConnection.class);
        when(ucRole.getInputStream()).thenReturn(getClass().getResourceAsStream("/security-credentials-role.txt"));
        HttpURLConnection ucJson = mock(HttpURLConnection.class);
        when(ucJson.getInputStream()).thenReturn(
                getClass().getResourceAsStream("/ec2-credentials.json"),
                getClass().getResourceAsStream("/ec2-credentials2.json"));
        ResourceFetcher ec2metadata = new ResourceFetcher(url -> SECURITY_CREDENTIALS_URL.equals(url.toString()) ? ucRole : ucJson);
        credentialsProvider = new EC2CredentialsProvider(ec2metadata, clock, envname -> null);
        final Credentials credentials = credentialsProvider.getCredentials();
        assertThat(credentials.getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
        assertThat(credentials.getToken()).isEqualTo("IQoJb3JpZ2luX2VjEDgaDmFwLXNvdXRoZWFzdC0xIkgwRgIhALrqkBgWQifI5JgcBuj0CDCzjXNyWuovk/ObXJFku+q5AiEAmrOO1EaQKfDbNvw1KDbVU5bWPQMz0oo3+4iTIOswSHMq1QIIURABGgw4MDE5NDI0OTkxNzYiDHm9hvH/iK6cb8aM1yqyAvvPBQt/aGH9O0kVhX7YQPli5VfKnq/bZQ4Sj2cKBEOWFBoVDKpnC9MrDUieKDmIh+35Us4wb2xK6Ouo42VQ7qILn4l/Oy6BmX1tIhealRxR3FhietK+u2Z4122fCy4EdAUWQO+N3ZAKIzlvoQ83yvDmGVbUkIt6e75vQ14kl17GhuuteI7PvgWX/gDBNSwL5y9qUdrfIlt7r5eEBTIcRgzPZdf/+8PXd9DjhZSoO37N7ii89b706fyPm/wLtqTVoFBFFQBo3JrOnMX64KQ45hbT3zWkq0wJNA1C6GFZHXGqmp72KP1GS5nBMbjuMsKlWDUUOVDvAx1dlglQvi+xEX/IRQXdrHsjUwcmK1dZxrdpDYO/HPzeYcdVGrvxQ3zBPYlAynvm91N9cDdDiMQgwyTlRDCb8ILuBTrTAlOEjyWAFdvYQyFQBxa1XJqClj4OjF5KAu0fAKuDoDV83y2N7AxpLdqnYuVnNMf/rzexbVTJeWRNSWZTA00t9LuZEjMPHtTI04B3vk5uzryCWGg1a72ifUd3GvgFvy9NSPShzTLIBum44IppbE0/ex7YwKbG2xjmEAK/MkRKGf/cNnTsiyc08kmu/SOBiPTODzQnLl9d7zPIvE6S+uzKORtadMhyH3GZkf+nKe9opiGFRNQPtIMRJZl7TW/YIsKwHquR01R8pFLrvwZ81981wQ8Zryb+EuYmayqz5mU8Wkhd5F95Y829mZtGvOxC+pUeL8Ynj7Z+B5D7LE33DU83HKQ0ZtcjpC5ZCQEL8BHpHpAo198Z3199XeFBjPD4uzepDu3eNw9lE/iPUXPvwdK5pX5/ASNWxu8gqK/o1qCTHL8Cgc98jbCwkzlU8nMG/NU+nbWs/w==");
        final Credentials credentials2 = credentialsProvider.getCredentials();
        assertThat(credentials2.getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
        assertThat(credentials2.getToken()).isEqualTo("another-token");
    }
}
