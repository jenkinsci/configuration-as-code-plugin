@Library('pipeline-library@warnings-ng-test') _

def configurations = [
    [ platform: "linux", jdk: "8", jenkins: null ],
    [ platform: "linux", jdk: "11", jenkins: null, javaLevel: "8" ]
]
buildPlugin2(configurations: configurations, timeout: 180, useAci: true)
