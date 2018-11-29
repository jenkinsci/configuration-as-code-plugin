package io.jenkins.plugins.casc.yaml;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */

public class YamlSource<T> {

    public final T source;

    public final YamlReader<T> reader;

    public YamlSource(T source, YamlReader<T> reader) {
        this.source = source;
        this.reader = reader;
    }

    public static YamlSource<InputStream> of(InputStream in) {
        return new YamlSource<>(in, READ_FROM_INPUTSTREAM);
    }

    public static YamlSource<String> of(URL url) {
        return new YamlSource<>(url.toExternalForm(), READ_FROM_URL);
    }

    public Reader read() throws IOException {
        return reader.open(source);
    }

    public String source() {
        return source.toString();
    }

    public static final YamlReader<String> READ_FROM_URL = config -> {
        final URL url = URI.create(config).toURL();
        URLConnection connection = url.openConnection();
        String user = System.getenv(ConfigurationAsCode.CASC_JENKINS_CONFIG_USER_ENV);
        String password = System.getenv(ConfigurationAsCode.CASC_JENKINS_CONFIG_PASSWORD_ENV);
        String token = System.getenv(ConfigurationAsCode.CASC_JENKINS_CONFIG_TOKEN_ENV);
        if ((StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password))
                || StringUtils.isNotBlank(token)) {
            connection.setRequestProperty("Authorization", getAuthString(user, password, token));
        }
        return new InputStreamReader(connection.getInputStream(), UTF_8);
    };

    public static final YamlReader<Path> READ_FROM_PATH = Files::newBufferedReader;

    public static final YamlReader<InputStream> READ_FROM_INPUTSTREAM = in -> new InputStreamReader(in, UTF_8);

    public static final YamlReader<HttpServletRequest> READ_FROM_REQUEST = req -> {
        // TODO get encoding from req.getContentType()
        return new InputStreamReader(req.getInputStream(), UTF_8);
    };

    @Override
    public String toString() {
        return "YamlSource: " + source;
    }

    static String getAuthString(String user, String password, String token) {
        if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password)) {
            return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(Charset.forName("UTF-8")));
        } else if (StringUtils.isNotBlank(token)) {
            return "Bearer " + token;
        }
        return null;
    }

}
