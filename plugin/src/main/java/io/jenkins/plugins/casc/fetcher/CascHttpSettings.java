package io.jenkins.plugins.casc.fetcher;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

@Extension
public class CascHttpSettings extends GlobalConfiguration {

    private List<RemoteConfig> remoteConfigs = new ArrayList<>();

    public CascHttpSettings() {
        load();
    }

    public static CascHttpSettings get() {
        return GlobalConfiguration.all().get(CascHttpSettings.class);
    }

    public List<RemoteConfig> getRemoteConfigs() {
        return remoteConfigs != null ? unmodifiableList(remoteConfigs) : emptyList();
    }

    @DataBoundSetter
    public void setRemoteConfigs(List<RemoteConfig> remoteConfigs) {
        this.remoteConfigs = remoteConfigs != null ? new ArrayList<>(remoteConfigs) : new ArrayList<>();
        save();
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        if (!json.has("remoteConfigs")) {
            setRemoteConfigs(emptyList());
        }
        save();
        return true;
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return "Configuration as Code HTTP Fetcher";
    }

    public static RemoteConfig getConfigForUrl(String url) {
        CascHttpSettings settings = get();
        if (settings == null || settings.getRemoteConfigs().isEmpty() || StringUtils.isBlank(url)) {
            return null;
        }

        URI targetUri;
        try {
            targetUri = new URI(url);
        } catch (URISyntaxException e) {
            return null;
        }

        RemoteConfig bestMatch = null;
        int longestMatchLength = -1;

        for (RemoteConfig config : settings.getRemoteConfigs()) {
            String prefix = config.getUrlPrefix();
            if (StringUtils.isBlank(prefix)) {
                continue;
            }

            URI prefixUri;
            try {
                prefixUri = new URI(prefix);
            } catch (URISyntaxException e) {
                continue;
            }

            if (matches(targetUri, prefixUri)) {
                int matchLength = prefix.length();
                if (matchLength > longestMatchLength) {
                    longestMatchLength = matchLength;
                    bestMatch = config;
                }
            }
        }

        return bestMatch;
    }

    private static boolean matches(URI target, URI prefix) {
        if (prefix.getScheme() != null && !prefix.getScheme().equalsIgnoreCase(target.getScheme())) {
            return false;
        }
        if (prefix.getHost() != null && !prefix.getHost().equalsIgnoreCase(target.getHost())) {
            return false;
        }
        int prefixPort = getEffectivePort(prefix);
        int targetPort = getEffectivePort(target);
        if (prefixPort != targetPort) {
            return false;
        }

        String targetPath = target.getPath() == null ? "/" : target.getPath();
        String prefixPath = prefix.getPath() == null ? "/" : prefix.getPath();

        if (!prefixPath.endsWith("/")) {
            prefixPath += "/";
        }
        if (!targetPath.endsWith("/")) {
            targetPath += "/";
        }

        return targetPath.startsWith(prefixPath);
    }

    private static int getEffectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            return 80;
        }
        return -1;
    }

    public enum AuthMethod {
        NONE("No Authentication"),
        BASIC("Basic Authentication (Username / Password)"),
        BEARER("Bearer Token"),
        API_KEY("API Key (Custom Header)");

        private final String description;

        AuthMethod(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class RemoteConfig implements Describable<RemoteConfig> {

        private String urlPrefix;
        private String credentialId;
        private AuthMethod authMethod;
        private String headerName;

        @DataBoundConstructor
        public RemoteConfig(String urlPrefix, String credentialId, AuthMethod authMethod) {
            this.urlPrefix = urlPrefix;
            this.credentialId = credentialId;
            this.authMethod = authMethod != null ? authMethod : AuthMethod.NONE;
        }

        public String getUrlPrefix() {
            return urlPrefix;
        }

        @DataBoundSetter
        public void setUrlPrefix(String urlPrefix) {
            this.urlPrefix = urlPrefix;
        }

        public String getCredentialId() {
            return credentialId;
        }

        @DataBoundSetter
        public void setCredentialId(String credentialId) {
            this.credentialId = credentialId;
        }

        public AuthMethod getAuthMethod() {
            return authMethod;
        }

        @DataBoundSetter
        public void setAuthMethod(AuthMethod authMethod) {
            this.authMethod = authMethod != null ? authMethod : AuthMethod.NONE;
        }

        public String getHeaderName() {
            return headerName;
        }

        @DataBoundSetter
        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Descriptor<RemoteConfig> getDescriptor() {
            return Jenkins.get().getDescriptorOrDie(getClass());
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<RemoteConfig> {

            @Override
            @NonNull
            public String getDisplayName() {
                return "Remote HTTP Configuration";
            }

            @SuppressWarnings("unused")
            public ListBoxModel doFillAuthMethodItems() {
                ListBoxModel items = new ListBoxModel();
                for (AuthMethod method : AuthMethod.values()) {
                    items.add(method.getDescription(), method.name());
                }
                return items;
            }

            @SuppressWarnings("unused")
            public FormValidation doCheckUrlPrefix(@QueryParameter String value) {
                if (StringUtils.isBlank(value)) {
                    return FormValidation.error("URL Prefix cannot be empty.");
                }
                try {
                    URI uri = new URI(value);
                    if (uri.getScheme() == null
                            || (!uri.getScheme().equalsIgnoreCase("http")
                                    && !uri.getScheme().equalsIgnoreCase("https"))) {
                        return FormValidation.error("URL Prefix must start with http:// or https://");
                    }

                    if (uri.getHost() == null) {
                        return FormValidation.error(
                                "URL Prefix must include a valid host (e.g., https://example.com).");
                    }

                    if (uri.getQuery() != null) {
                        return FormValidation.error(
                                "URL Prefix should not contain query parameters (e.g., ?key=value). It must be a base path only.");
                    }

                    if (uri.getFragment() != null) {
                        return FormValidation.error(
                                "URL Prefix should not contain URL fragments (e.g., #section). It must be a base path only.");
                    }

                } catch (Exception e) {
                    return FormValidation.error("Invalid URL format: " + e.getMessage());
                }
                return FormValidation.ok();
            }

            @SuppressWarnings("unused")
            public FormValidation doCheckCredentialId(@QueryParameter String value, @QueryParameter String authMethod) {
                if (!AuthMethod.NONE.name().equals(authMethod) && StringUtils.isBlank(value)) {
                    return FormValidation.error("Credential ID is required when authentication is enabled.");
                }
                return FormValidation.ok();
            }
        }
    }
}
