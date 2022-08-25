# Check if we need to prepend docker commands with sudo
SUDO := $(shell docker version >/dev/null 2>&1 || echo "sudo")

# If LABEL is not provided set default value
LABEL ?= $(shell git rev-parse --short HEAD)$(and $(shell git status -s),-dirty-$(shell id -u -n))
# If TAG is not provided set default value
TAG ?= stellar/anchor-platform:$(LABEL)
E2E_TAG ?= stellar/anchor-platform-e2e-test:$(LABEL)
# https://github.com/opencontainers/image-spec/blob/master/annotations.md
BUILD_DATE := $(shell date -u +%FT%TZ)
DOCKER_COMPOSE_TEST_CONFIG_DIR = integration-tests/docker-compose-configs
DOCKER_COMPOSE_TEST_BASE_FILE := $(DOCKER_COMPOSE_TEST_CONFIG_DIR)/docker-compose.base.yaml
DOCKER_COMPOSE_TEST_OVERRIDE_FILE = docker-compose-config.override.yaml

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

run-e2e-test-default-config:
	$(SUDO) docker-compose -f $(DOCKER_COMPOSE_TEST_BASE_FILE) \
	-f $(DOCKER_COMPOSE_TEST_CONFIG_DIR)/anchor-platform-default-configs/$(DOCKER_COMPOSE_TEST_OVERRIDE_FILE) rm -f

	$(SUDO) docker-compose --env-file $(DOCKER_COMPOSE_TEST_CONFIG_DIR)/.env \
	-f $(DOCKER_COMPOSE_TEST_BASE_FILE) \
	-f $(DOCKER_COMPOSE_TEST_CONFIG_DIR)/anchor-platform-default-configs/$(DOCKER_COMPOSE_TEST_OVERRIDE_FILE) \
	up --exit-code-from end-to-end-tests

run-e2e-test-allowlist:
	$(SUDO) docker-compose -f $(DOCKER_COMPOSE_TEST_BASE_FILE) \
	-f $(DOCKER_COMPOSE_TEST_CONFIG_DIR)/anchor-platform-allowlist/$(DOCKER_COMPOSE_TEST_OVERRIDE_FILE) rm -f

	$(SUDO) docker-compose --env-file $(DOCKER_COMPOSE_TEST_CONFIG_DIR)/.env \
	-f $(DOCKER_COMPOSE_TEST_BASE_FILE) \
	-f $(DOCKER_COMPOSE_TEST_CONFIG_DIR)/anchor-platform-allowlist/$(DOCKER_COMPOSE_TEST_OVERRIDE_FILE) \
	up --exit-code-from end-to-end-tests

run-e2e-test-unique-address:
	$(SUDO) docker-compose -f $(DOCKER_COMPOSE_TEST_BASE_FILE) \
	-f $(DOCKER_COMPOSE_TEST_CONFIG_DIR)/anchor-platform-unique-address/$(DOCKER_COMPOSE_TEST_OVERRIDE_FILE) rm -f

	$(SUDO) docker-compose --env-file $(DOCKER_COMPOSE_TEST_CONFIG_DIR)/.env \
	-f $(DOCKER_COMPOSE_TEST_BASE_FILE) \
	-f $(DOCKER_COMPOSE_TEST_CONFIG_DIR)/anchor-platform-unique-address/$(DOCKER_COMPOSE_TEST_OVERRIDE_FILE) \
	up --exit-code-from end-to-end-tests

