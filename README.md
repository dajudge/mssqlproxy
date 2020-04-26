mssqlproxy
-
A proxy sidecar for moving security with MSSQL out of your services.

You connect to the proxy with any username / password combination and the proxy injects the correct login credentials.

# Usage
```shell script
$ docker run -d -p 1433:1433 -e MSSQLPROXY_PROXIES="0.0.0.0:1433=username:password@mssql-server:1433" dajudge/mssqlproxy:0.0.1
```

`MSSQLPROXY_PROXIES` is a comma-separated list of proxy definitions where each proxy definition looks like this:
```
<bindAddress>:<bindPort>=<username>:<password>@<serverHostname>:<serverPort>
```
Fields:
* `bindAddress`: the local address to listen on
* `bindPort`: the local port to listen on
* `username`: the username to use for logging in to the MSSQL server
* `password`: the password to use for logging into the MSQQL server
* `serverHostname`: the hostname of the MSSQL server
* `serverPort`: the port of the MSSQL server

# TODO
* Improved handling of various protocol versions.
* Strong server certificate verification (waiting for a [feature in r2dbc-mssql](https://github.com/r2dbc/r2dbc-mssql/issues/148)).