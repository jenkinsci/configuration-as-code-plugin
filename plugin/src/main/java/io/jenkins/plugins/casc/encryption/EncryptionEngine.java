package io.jenkins.plugins.casc.encryption;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import java.security.GeneralSecurityException;

public abstract class EncryptionEngine implements ExtensionPoint {

    public abstract String getId();

    public abstract String decrypt(String encryptedData) throws GeneralSecurityException;

    public static ExtensionList<EncryptionEngine> all() {
        return ExtensionList.lookup(EncryptionEngine.class);
    }
}
