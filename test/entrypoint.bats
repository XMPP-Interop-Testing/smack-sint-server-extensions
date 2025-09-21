#!/usr/bin/env bats

bats_load_library 'bats-support'
bats_load_library 'bats-assert'

setup() {
  SCRIPT="$(pwd)/entrypoint.sh"
  chmod +x "$SCRIPT"
}

@test "prints usage with --help" {
  run "$SCRIPT" --help
  assert_success
  assert_output --partial "Usage:"
}

@test "fails with invalid argument" {
  run "$SCRIPT" --notarealflag
  assert_failure
  assert_output --partial "Error: Invalid argument"
}

@test "fails if only one account is specified" {
  run "$SCRIPT" --accountOneUsername=foo --accountOnePassword=bar
  assert_failure
  assert_output --partial "You must specify three test accounts, or none at all"
}

@test "fails if three accounts and admin specified" {
  run "$SCRIPT" --accountOneUsername=foo --accountOnePassword=bar --accountTwoUsername=foo2 --accountTwoPassword=bar2 --accountThreeUsername=foo3 --accountThreePassword=bar3 --adminAccountUsername=admin --adminAccountPassword=adminpw
  assert_failure
  assert_output --partial "You can specify either 3 individual test accounts or an admin account, or neither, but not both."
}

@test "succeeds with three accounts only" {
  run "$SCRIPT" --accountOneUsername=foo --accountOnePassword=bar --accountTwoUsername=foo2 --accountTwoPassword=bar2 --accountThreeUsername=foo3 --accountThreePassword=bar3
  assert_success
  assert_output --partial "Running: java -Dsinttest"
  assert_output --partial "-Dsinttest.accountOneUsername=foo -Dsinttest.accountOnePassword=bar -Dsinttest.accountTwoUsername=foo2 -Dsinttest.accountTwoPassword=bar2 -Dsinttest.accountThreeUsername=foo3 -Dsinttest.accountThreePassword=bar3"
}

@test "succeeds with admin only" {
  run "$SCRIPT" --adminAccountUsername=admin --adminAccountPassword=adminpw
  assert_success
  assert_output --partial "Running: java -Dsinttest"
  assert_output --partial "-Dsinttest.adminAccountUsername=admin -Dsinttest.adminAccountPassword=adminpw"
}

@test "succeeds with neither an admin account or manually specified accounts (IBR mode)" {
  run "$SCRIPT"
  assert_success
  assert_output --partial "Running: java -Dsinttest"
}