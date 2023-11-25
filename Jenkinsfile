buildPlugin(useContainerAgent: true, forkCount: '1C', timeout: 180, configurations: [
    [platform: 'linux', jdk: 21],
    // Windows fails on >11 https://github.com/jenkinsci/configuration-as-code-plugin/pull/2392#issuecomment-1826296308
    [platform: 'windows', jdk: 11],
])
