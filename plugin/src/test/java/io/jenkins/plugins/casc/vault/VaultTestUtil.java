package io.jenkins.plugins.casc.vault;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestEnvironment;
import org.testcontainers.vault.VaultContainer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VaultTestUtil {
    private final static Logger LOGGER = Logger.getLogger(VaultTestUtil.class.getName());

    public static final String VAULT_DOCKER_IMAGE = "vault:1.0.3";
    public static final String VAULT_ROOT_TOKEN = "root-token";
    public static final String VAULT_USER = "admin";
    public static final String VAULT_URL = "http://localhost:8200";
    public static final int VAULT_PORT = 8200;
    public static final String VAULT_PW = "admin";
    public static final String VAULT_POLCY= "io/jenkins/plugins/casc/vault/vaultTest_adminPolicy.hcl";
    public static final String VAULT_PATH_V1 = "kv-v1/admin";
    public static final String VAULT_PATH_V2 = "kv-v2/admin";
    public static String VAULT_APPROLE_ID = "";
    public static String VAULT_APPROLE_SECRET = "";

    private static Container.ExecResult runCommand(VaultContainer container, final String... command) throws IOException, InterruptedException {
        LOGGER.log(Level.FINE, String.join(" ", command));
        final Container.ExecResult result = container.execInContainer(command);
        final String out = result.getStdout();
        final String err = result.getStderr();
        if (out != null && !out.isEmpty()) {
            LOGGER.log(Level.FINE, result.getStdout());
        }
        if (err != null && !err.isEmpty()) {
            LOGGER.log(Level.WARNING, result.getStderr());
        }
        return result;
    }

    public static boolean hasDockerDaemon() {
        try {
            return TestEnvironment.dockerApiAtLeast("1.10");
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static VaultContainer createVaultContainer() {
        if (!hasDockerDaemon()) return null;
        return new VaultContainer<>(VaultTestUtil.VAULT_DOCKER_IMAGE)
                .withVaultToken(VaultTestUtil.VAULT_ROOT_TOKEN)
                .withClasspathResourceMapping(VAULT_POLCY, "/admin.hcl", BindMode.READ_ONLY)
                .withVaultPort(VAULT_PORT)
                .waitingFor(Wait.forHttp("/v1/sys/seal-status").forStatusCode(200));
    }

    public static void configureVaultContainer(VaultContainer container) {
        try {
            // Create Secret Backends
            runCommand(container, "vault", "secrets", "enable", "-path=kv-v2", "-version=2", "kv");
            runCommand(container, "vault", "secrets", "enable", "-path=kv-v1", "-version=1", "kv");

            // Create user/password credential
            runCommand(container, "vault", "auth", "enable", "userpass");
            runCommand(container, "vault", "write", "auth/userpass/users/" + VAULT_USER, "password=" + VAULT_PW, "policies=admin");

            // Create policies
            runCommand(container, "vault", "policy", "write", "admin", "/admin.hcl");

            // Create AppRole
            runCommand(container, "vault", "auth", "enable", "approle");
            runCommand(container, "vault", "write", "auth/approle/role/admin", "secret_id_ttl=10m",
                    "token_num_uses=0", "token_ttl=20m", "token_max_ttl=20m", "secret_id_num_uses=1000", "policies=admin");
            VAULT_APPROLE_ID = runCommand(container, "vault", "read", "-field=role_id", "auth/approle/role/admin/role-id")
                    .getStdout();
            VAULT_APPROLE_SECRET = runCommand(container, "vault", "write", "-force", "-field=secret_id", "auth/approle/role/admin/secret-id")
                    .getStdout();

            // add secrets for v1 and v2
            runCommand(container, "vault", "kv", "put", VAULT_PATH_V1, "key1=123", "key2=456");
            runCommand(container, "vault", "kv", "put", VAULT_PATH_V2, "key1=123", "key2=456");

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage());
        }
    }
}
