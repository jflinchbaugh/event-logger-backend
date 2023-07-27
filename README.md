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

post new categories data:
```
$ curl -s -v -u u:p \
  -H 'Content-Type: application/json' \
  -d '{"categories": [{"thing": true,"whatever": 2}]}' \
  http://localhost:8080/api/logger/z
```

delete the event-logger:
```
$ curl -s -v -u u:p -X delete http://localhost:8080/api/logger/z
```
