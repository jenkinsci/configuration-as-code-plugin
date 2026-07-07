package io.jenkins.plugins.casc.fetcher;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.security.ACL;
import java.util.Collections;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

@Extension(ordinal = 100)
public class JenkinsCredentialsProvider implements FetchCredentialsProvider {

    @SuppressWarnings({"unchecked", "deprecation"})
    @Override
    public <T extends FetchAuthData> T getCredentials(String credentialId, Class<T> type) {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return null;
        }

        List<DomainRequirement> domainRequirements = Collections.emptyList();

        List<IdCredentials> credentials =
                CredentialsProvider.lookupCredentials(IdCredentials.class, jenkins, ACL.SYSTEM, domainRequirements);

        IdCredentials match = credentials.stream()
                .filter(c -> credentialId.equals(c.getId()))
                .findFirst()
                .orElse(null);

        if (match == null) {
            return null;
        }

        if (type == FetchAuthData.Token.class && match instanceof StringCredentials stringCred) {
            return (T) (FetchAuthData.Token) () -> stringCred.getSecret().getPlainText();
        }

        if (type == FetchAuthData.UsernamePassword.class
                && match instanceof StandardUsernamePasswordCredentials userPass) {
            return (T) new FetchAuthData.UsernamePassword() {
                @Override
                public String getUsername() {
                    return userPass.getUsername();
                }

                @Override
                public String getPassword() {
                    return userPass.getPassword().getPlainText();
                }
            };
        }

        if (type == FetchAuthData.SshKey.class && match instanceof SSHUserPrivateKey sshCred) {

            List<String> keys = sshCred.getPrivateKeys();
            if (keys.isEmpty()) {
                return null;
            }

            return (T) new FetchAuthData.SshKey() {
                @Override
                public String getUsername() {
                    return sshCred.getUsername();
                }

                @Override
                public String getPrivateKey() {
                    return keys.get(0);
                }

                @Override
                public String getPassphrase() {
                    return sshCred.getPassphrase() != null
                            ? sshCred.getPassphrase().getPlainText()
                            : null;
                }
            };
        }

        return null;
    }
}
