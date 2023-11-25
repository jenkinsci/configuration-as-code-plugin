buildPlugin(useContainerAgent: true, forkCount: '1C', timeout: 180, configurations: [
    [platform: 'linux', jdk: 21],
    // Integration tests fail in unexpected ways with Java 17 on Windows
    // https://github.com/jenkinsci/configuration-as-code-plugin/pull/2392
    // Switch to Java 17 on Windows when unexpected failures are resolved
    [platform: 'windows', jdk: 11],
])
