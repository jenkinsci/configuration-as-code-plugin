/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/
buildPlugin(
  useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
  timeout: 180,
  configurations: [
    [platform: 'linux', jdk: 17],
    [platform: 'linux', jdk: 11 ],
])
