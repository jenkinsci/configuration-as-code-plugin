#!/usr/bin/env bats

@test "The demo example ${DEMO_EXAMPLE_TO_TEST} folder (${DEMO_EXAMPLE_DIR}) is reachable" {
    [ -n "${DEMO_EXAMPLE_DIR}" ]
    [ -d "${DEMO_EXAMPLE_DIR}" ]
}

@test "A README.md file is present" {
    [ -f "${DEMO_EXAMPLE_DIR}/README.md" ]
}

@test "A config.yaml file" {
    [ -f "${DEMO_EXAMPLE_DIR}/config.yaml" ]
}

# @test "we can build the Jenkins Docker Image for this demo example" {

# }

# @test "we can start the Jenkins for this demo example" {

# }
