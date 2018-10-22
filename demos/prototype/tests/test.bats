#!/usr/bin/env bats

load "${BATS_HELPERS_DIR}/bats-support/load.bash"
load "${BATS_HELPERS_DIR}/bats-assert/load.bash"

@test "FOO env variable has been created" {
    run grep -rI ">FOO<" /var/jenkins_home/
    assert_success
}
