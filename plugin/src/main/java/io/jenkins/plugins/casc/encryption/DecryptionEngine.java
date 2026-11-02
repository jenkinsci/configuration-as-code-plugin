package io.jenkins.plugins.casc.encryption;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import java.io.IOException;
import java.security.GeneralSecurityException;

public abstract class DecryptionEngine implements ExtensionPoint {

    public abstract String getId();

    public abstract String decrypt(String encryptedData) throws GeneralSecurityException, IOException;

    public static ExtensionList<DecryptionEngine> all() {
        return ExtensionList.lookup(DecryptionEngine.class);
    }
}
