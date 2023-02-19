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
