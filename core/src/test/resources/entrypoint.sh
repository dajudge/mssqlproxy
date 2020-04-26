#! /bin/sh

# Copyright 2020 The mssqlproxy developers (see CONTRIBUTORS)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# network.forceencryption                    Force encryption of incoming client connections
# network.ipaddress                          IP address for incoming connections
# network.kerberoskeytabfile                 Kerberos keytab file location
# network.tcpport                            TCP port for incoming connections
# network.tlscert                            Path to certificate file for encrypting incoming client connections
# network.tlsciphers                         TLS ciphers allowed for encrypted incoming client connections
# network.tlskey                             Path to private key file for encrypting incoming client connections
# network.tlsprotocols                       TLS protocol versions allowed for encrypted incoming client connections

#/opt/mssql/bin/mssql-conf set network.tlscert /server-cert.pem
#/opt/mssql/bin/mssql-conf set network.tlskey /server-key.pem
#/opt/mssql/bin/mssql-conf set network.tlsprotocols 1.2
#/opt/mssql/bin/mssql-conf set network.forceencryption 1

exec "$@"