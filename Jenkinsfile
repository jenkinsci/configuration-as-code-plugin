// Windows controller tests crash with unexpected errors
buildPlugin(useContainerAgent: true, forkCount: '0.5C', timeout: 360, configurations: [
    [platform: 'linux', jdk: 21],
    // Windows fails on >11 https://github.com/jenkinsci/configuration-as-code-plugin/pull/2392#issuecomment-1826296308
    [platform: 'windows', jdk: 11],
])
