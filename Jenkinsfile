def recentLTS = "2.174-rc28244.3d5a5a7c3c04"
def configurations = [
    [ platform: "linux", jdk: "8", jenkins: null ],
    [ platform: "linux", jdk: "8", jenkins: recentLTS, javaLevel: "8" ],
    [ platform: "linux", jdk: "11", jenkins: recentLTS, javaLevel: "8" ],
]
buildPlugin(configurations: configurations, timeout: 180)
