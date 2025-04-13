# event-logger-backend

run the server:
```
$ clj -M:run
```

register a new event-logger with login and password:
```
$ curl -s -v -d login=u -d password=p  http://localhost:8080/api/register/z
```

download the event-logger data:
```
$ curl -s -v -u u:p http://localhost:8080/api/logger/z
```

post new document data:
```
$ curl -s -v -u u:p \
  -H 'Content-Type: text/plain' \
  -d '{"categories": [{"thing": true,"whatever": 2}]}' \
  http://localhost:8080/api/logger/z
```

delete the event-logger:
```
$ curl -s -v -u u:p -X delete http://localhost:8080/api/logger/z
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
