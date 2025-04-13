all: container

.PHONY: uber

uber:
	clj -T:build uber

.PHONY: container

container: uber
	podman build -t event-logger-backend:latest .

