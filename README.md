# event-logger-backend

### Development

Run the tests:
```bash
make test
# or
clj -T:build run-tests [-n event-logger-backend.core-test]
```

Run the server:
```bash
clj -M:run
```

Build the standalone uberjar:
```bash
make uber
# or
clj -T:build uber
```

Build the container image:
```bash
make container
# or
make
```

### Usage

download the event-logger data:
```
$ curl -s -v -u u:p http://localhost:8080/storage/api/logger/z
```

post new document data:
```
$ curl -s -v -u u:p \
  -H 'Content-Type: text/plain' \
  -d '{"categories": [{"thing": true,"whatever": 2}]}' \
  http://localhost:8080/storage/api/logger/z
```

delete the event-logger:
```
$ curl -s -v -u u:p -X delete http://localhost:8080/storage/api/logger/z
```

static content, like the stylesheet, is available as well:
```
$ curl -s -v http://localhost:8080/css/style.css
```

build the container image:
```
$ make
```

start the containers: app + xtdb 2:
```
$ podman kube play event-logger-backend.yaml
```

stop the containers: app + xtdb 2:
```
$ podman kube down event-logger-backend.yaml
```

create a system service around the pods:
```
$ sudo cp event-logger-backend.service /usr/lib/systemd/system/event-logger-backend.service
$ sudo systemctl daemon-reload
$ sudo systemctl enable event-logger-backend
$ sudo systemctl start event-logger-backend
$ sudo systemctl stop event-logger-backend
```

Create a user Quadlet to run pods:
```
$ loginctl enable-linger # so our services will start at boot and stay around
$ cp event-logger-backend.kube event-logger-backend.yaml $HOME/.config/containers/systemd/
$ systemctl --user daemon-reload
$ systemctl --user start event-logger-backend
```

Access the running XTDB server from the api server:
```
$ podman exec -it event-logger-backend-api psql -U xtdb -h xtdb
```
