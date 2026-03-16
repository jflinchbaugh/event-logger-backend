all: container

.PHONY: test

test:
	clj -T:build run-tests

.PHONY: uber

uber: test
	clj -T:build uber

.PHONY: container

container: uber
	podman build -t event-logger-backend:latest .

run-server:
	podman kube play --wait event-logger-backend.yaml

run-xtdb:
	podman kube play --wait xtdb.yaml
