#!/bin/bash

readonly FAIL_ON_WARNINGS="${FAIL_ON_WARNINGS:-false}" # Can be set as Environment variable
if [ "${DEBUG}" == 'true' ]
then
    set -x
fi

set -eu -o pipefail

readonly SCRIPT_BASE_DIR="$(cd "$(dirname "${0}")" && pwd -P)"
readonly DEMO_BASE_DIR="${SCRIPT_BASE_DIR}"

function main() {
    local DEMO_EXAMPLE_DIR

    while IFS= read -r -d '' DEMO_EXAMPLE_DIR
    do
        echo "= Found example in: ${DEMO_EXAMPLE_DIR}"
        verify_demo_example "${DEMO_EXAMPLE_DIR}"
    done <   <(find "${DEMO_BASE_DIR}" -not -path '*/\.*' -type d \( ! -iname ".*" \) -mindepth 1 -maxdepth 1 -print0 | sort)

    return 0
}

function verify_demo_example() {
    local DEMO_EXAMPLE_TO_TEST DEMO_EXAMPLE_DIR TEST_RESULT_DIR PLUGINS_TO_INSTALL TEST_FAILURE TEST_SUITE

    readonly DEMO_EXAMPLE_TO_TEST="$(basename "${1}")"
    readonly DEMO_EXAMPLE_DIR="${DEMO_BASE_DIR}/${DEMO_EXAMPLE_TO_TEST}"
    readonly TEST_RESULT_DIR="${DEMO_BASE_DIR}/target/${DEMO_EXAMPLE_TO_TEST}"
    mkdir -p "${TEST_RESULT_DIR}"

    # Is the demo example folder name related to a plugin?
    echo "== Checking if ${DEMO_EXAMPLE_TO_TEST} is a plugin..."
    if curl -sSLI --fail "http://updates.jenkins.io/latest/${DEMO_EXAMPLE_TO_TEST}.hpi" >/dev/null 2>&1
    then
        PLUGINS_TO_INSTALL="${DEMO_EXAMPLE_TO_TEST}"
        echo "=== ${DEMO_EXAMPLE_TO_TEST} is a plugin."
    else
        PLUGINS_TO_INSTALL=""
        echo "=== ${DEMO_EXAMPLE_TO_TEST} is NOT a plugin."
    fi

    # Do we have more plugins to install?
    if [ -f "${DEMO_EXAMPLE_TO_TEST}/plugins.txt" ]
    then
        PLUGINS_TO_INSTALL+="$(< "${DEMO_EXAMPLE_TO_TEST}/plugins.txt")"
    fi

    export DEMO_EXAMPLE_TO_TEST DEMO_EXAMPLE_DIR # Make it available for the test suite

    TEST_FAILURE=0

    # Run the "commons" tests suite
    # set +e # Do not fail the script if the test suite reports an error
    bats --tap "${DEMO_BASE_DIR}/.commons/commons.bats" > "${TEST_RESULT_DIR}/commons.tap" \
        || TEST_FAILURE=1
    # set -e

    # If specific test suites are found, then run it
    # Ref. https://github.com/koalaman/shellcheck/wiki/SC2144 for the loop usage
    for TEST_SUITE in "${DEMO_BASE_DIR}/${DEMO_EXAMPLE_TO_TEST}/tests/"*.bats
    do
        set +e # Do not fail the script if the test suite reports an error
        if [ -f "${TEST_SUITE}" ]
        then
            bats --tap "${TEST_SUITE}" > "${TEST_RESULT_DIR}/$(basename "${TEST_SUITE}").tap" \
                || TEST_FAILURE=1
        fi
        set -e
    done

    # Report any failure found during test run
    if [ "${TEST_FAILURE}" -eq 1 ]
    then
        echo "== Test suite for ${DEMO_EXAMPLE_TO_TEST} ran with test failures."
    else
        echo "== Test suite for ${DEMO_EXAMPLE_TO_TEST} ran successfully."
    fi

    echo "== Test result can be found in the directory ${TEST_RESULT_DIR}"

    return 0
}

main

exit 0
