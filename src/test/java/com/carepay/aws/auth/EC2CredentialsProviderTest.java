package com.carepay.aws.auth;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.carepay.aws.net.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.carepay.aws.auth.EC2CredentialsProvider.SECURITY_CREDENTIALS_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EC2CredentialsProviderTest {
    private EC2CredentialsProvider credentialsProvider;
    private WebClient ec2metadata;

    @BeforeEach
    void setUp() throws IOException {
        HttpURLConnection ucRole = mock(HttpURLConnection.class);
        when(ucRole.getInputStream()).thenReturn(getClass().getResourceAsStream("/security-credentials-role.txt"));
        HttpURLConnection ucJson = mock(HttpURLConnection.class);
        when(ucJson.getInputStream()).thenReturn(getClass().getResourceAsStream("/ec2-credentials.json"));
        ec2metadata = new WebClient(url -> SECURITY_CREDENTIALS_URL.equals(url.toString()) ? ucRole : ucJson);
        credentialsProvider = new EC2CredentialsProvider(ec2metadata, envname -> null);
    }

    @Test
    void testGetCredentials() {
        assertThat(credentialsProvider.getCredentials().getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
    }

    @Test
    public void testExpiry() throws IOException {
        HttpURLConnection ucRole = mock(HttpURLConnection.class);
        when(ucRole.getInputStream()).thenReturn(getClass().getResourceAsStream("/security-credentials-role.txt"));
        HttpURLConnection ucJson = mock(HttpURLConnection.class);
        when(ucJson.getInputStream()).thenReturn(
                getClass().getResourceAsStream("/ec2-credentials.json"),
                getClass().getResourceAsStream("/ec2-credentials2.json"));
        WebClient ec2metadata = new WebClient(url -> SECURITY_CREDENTIALS_URL.equals(url.toString()) ? ucRole : ucJson);
        credentialsProvider = new EC2CredentialsProvider(ec2metadata, envname -> null);
        final Credentials credentials = credentialsProvider.getCredentials();
        assertThat(credentials.getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
        assertThat(credentials.getSessionToken()).isEqualTo("IQoJb3JpZ2luX2VjEDgaDmFwLXNvdXRoZWFzdC0xIkgwRgIhALrqkBgWQifI5JgcBuj0CDCzjXNyWuovk/ObXJFku+q5AiEAmrOO1EaQKfDbNvw1KDbVU5bWPQMz0oo3+4iTIOswSHMq1QIIURABGgw4MDE5NDI0OTkxNzYiDHm9hvH/iK6cb8aM1yqyAvvPBQt/aGH9O0kVhX7YQPli5VfKnq/bZQ4Sj2cKBEOWFBoVDKpnC9MrDUieKDmIh+35Us4wb2xK6Ouo42VQ7qILn4l/Oy6BmX1tIhealRxR3FhietK+u2Z4122fCy4EdAUWQO+N3ZAKIzlvoQ83yvDmGVbUkIt6e75vQ14kl17GhuuteI7PvgWX/gDBNSwL5y9qUdrfIlt7r5eEBTIcRgzPZdf/+8PXd9DjhZSoO37N7ii89b706fyPm/wLtqTVoFBFFQBo3JrOnMX64KQ45hbT3zWkq0wJNA1C6GFZHXGqmp72KP1GS5nBMbjuMsKlWDUUOVDvAx1dlglQvi+xEX/IRQXdrHsjUwcmK1dZxrdpDYO/HPzeYcdVGrvxQ3zBPYlAynvm91N9cDdDiMQgwyTlRDCb8ILuBTrTAlOEjyWAFdvYQyFQBxa1XJqClj4OjF5KAu0fAKuDoDV83y2N7AxpLdqnYuVnNMf/rzexbVTJeWRNSWZTA00t9LuZEjMPHtTI04B3vk5uzryCWGg1a72ifUd3GvgFvy9NSPShzTLIBum44IppbE0/ex7YwKbG2xjmEAK/MkRKGf/cNnTsiyc08kmu/SOBiPTODzQnLl9d7zPIvE6S+uzKORtadMhyH3GZkf+nKe9opiGFRNQPtIMRJZl7TW/YIsKwHquR01R8pFLrvwZ81981wQ8Zryb+EuYmayqz5mU8Wkhd5F95Y829mZtGvOxC+pUeL8Ynj7Z+B5D7LE33DU83HKQ0ZtcjpC5ZCQEL8BHpHpAo198Z3199XeFBjPD4uzepDu3eNw9lE/iPUXPvwdK5pX5/ASNWxu8gqK/o1qCTHL8Cgc98jbCwkzlU8nMG/NU+nbWs/w==");
        final Credentials credentials2 = credentialsProvider.getCredentials();
        assertThat(credentials2.getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
        assertThat(credentials2.getSessionToken()).isEqualTo("another-token");
    }
}
