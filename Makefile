# Check if we need to prepend docker commands with sudo
SUDO := $(shell docker version >/dev/null 2>&1 || echo "sudo")

# If LABEL is not provided set default value
LABEL ?= $(shell git rev-parse --short HEAD)$(and $(shell git status -s),-dirty-$(shell id -u -n))
# If TAG is not provided set default value
TAG ?= stellar/anchor-platform:$(LABEL)
E2E_TAG ?= stellar/anchor-platform-e2e-test:$(LABEL)
# https://github.com/opencontainers/image-spec/blob/master/annotations.md
BUILD_DATE := $(shell date -u +%FT%TZ)

docker-build:
	$(SUDO) docker build -f Dockerfile --pull --label org.opencontainers.image.created="$(BUILD_DATE)" \
	-t $(TAG) .

docker-push:
	$(SUDO) docker push $(TAG)

docker-build-e2e-test:
	$(SUDO) docker build -f end-to-end-tests/Dockerfile --pull --label org.opencontainers.image.created="$(BUILD_DATE)" \
	-t $(E2E_TAG) .

docker-push-e2e-test:
	$(SUDO) docker push $(E2E_TAG)

build-docker-compose-tests:
	docker-compose -f integration-tests/docker-compose-configs/docker-compose.base.yaml build --no-cache

run-e2e-test-all:
	make run-e2e-test-default-config
	make run-e2e-test-allowlist
	make run-e2e-test-unique-address

define run_tests
	$(SUDO) docker-compose -f integration-tests/docker-compose-configs/docker-compose.base.yaml \
	-f integration-tests/docker-compose-configs/$(1)/docker-compose-config.override.yaml rm -f

	$(SUDO) docker-compose --env-file integration-tests/docker-compose-configs/.env \
	-f integration-tests/docker-compose-configs/docker-compose.base.yaml \
	-f integration-tests/docker-compose-configs/$(1)/docker-compose-config.override.yaml \
	up --exit-code-from end-to-end-tests || (echo "E2E Test Failed: $(1)" && exit 1)
endef

run-e2e-test-default-config:
	$(call run_tests,anchor-platform-default-configs)

run-e2e-test-allowlist:
	$(call run_tests,anchor-platform-allowlist)

run-e2e-test-unique-address:
	$(call run_tests,anchor-platform-unique-address)
