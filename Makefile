# Check if we need to prepend docker commands with sudo
SUDO := $(shell docker version >/dev/null 2>&1 || echo "sudo")

# If LABEL is not provided set default value
LABEL ?= $(shell git rev-parse --short HEAD)$(and $(shell git status -s),-dirty-$(shell id -u -n))
# If TAG is not provided set default value
TAG ?= stellar/anchor-platform:$(LABEL)
TEST_TAG ?= stellar/anchor-platform-test:$(LABEL)
# https://github.com/opencontainers/image-spec/blob/master/annotations.md
BUILD_DATE := $(shell date -u +%FT%TZ)

docker-build:
	$(SUDO) docker build -f Dockerfile --pull --label org.opencontainers.image.created="$(BUILD_DATE)" \
	-t $(TAG) .

docker-push:
	$(SUDO) docker push $(TAG)

build-docker-compose-tests:
	docker-compose -f integration-tests/docker-compose-configs/docker-compose.base.yaml build --no-cache

run-integration-test-all:
	make run-integration-test-default-config
	make run-integration-test-allowlist
	make run-integration-test-unique-address

define run_integration_tests
	$(SUDO) docker-compose -f integration-tests/docker-compose-configs/docker-compose.base.yaml \
	-f integration-tests/docker-compose-configs/$(1)/docker-compose-config.override.yaml rm -f

	$(SUDO) docker-compose --env-file integration-tests/docker-compose-configs/.env \
	-f integration-tests/docker-compose-configs/docker-compose.base.yaml \
	-f integration-tests/docker-compose-configs/$(1)/docker-compose-config.override.yaml \
	up --exit-code-from end-to-end-tests || (echo "Integration Test Failed: $(1)" && exit 1)
endef

run-integration-test-default-config:
	$(call run_integration_tests,anchor-platform-default-configs)

run-integration-test-allowlist:
	$(call run_integration_tests,anchor-platform-allowlist)

run-integration-test-unique-address:
	$(call run_integration_tests,anchor-platform-unique-address)

run-e2e-tests:
	$(SUDO) docker-compose -f docker-compose/e2e-tests/docker-compose.yaml rm -f

	$(SUDO) docker-compose -f docker-compose/e2e-tests/docker-compose.yaml \
	up --exit-code-from app || (echo "E2E Test Failed: $(1)" && exit 1)