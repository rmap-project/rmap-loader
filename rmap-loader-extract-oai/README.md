# OAI harvest service

A simple OAI harvester that places raw harvested OAI records into a queue.  Physically, the it is an executable jar file that can be configured via system properties or environment variables.

The OAI harvest service is just an executable jar, the jar artifact for this module is executable. Configuration is provided by using environment variables, or system properties (it doesn't matter which).
For example,

    export oai.baseUrl=http://example.org/cgi-bin/oai.pl
    java -jar target/rmap-loader-extract-oai-0.0.1-SNAPSHOT.jar -Doai.setSpec=stuff -Doai.from=2015-12-09

## Configuration and Deployment

### `jms.brokerUrl` 

ActiveMQ broker URL.  Default is `tcp://localhost:61616`

### `jms.username`

JMS username.  Leave undefined if authentication is not used.

### `jms.password`

JMS password.  Leave undefined if authentication is not used.

### `jms.queue.dest`

Destination JMS queue to send OAI harvested documents to.  Default is `rmap.harvest.oai_dc`

### `jms.maxConnections`

Maximum number of JMS connections.  Default is 10.

### `oai.baseURL`

OAI baseURL

### `oai.from`

OAI from date

### `oai.metadataPrefix`

OAI metadata prefix

### `oai.until`

OAI until date

### `oai.setSpec

OAI setSpec

### `LOG.*`

Any environment variable or system propertu that begins with `LOG.` can be used to specify the logging level of 
the logger whose name appears after the `LOG.` characters.  For example, setting the environment variable:

    LOG.info.rmapproject=DEBUG
    
 This will set the logger called `info.rmapproject` to the `DEBUG` level. 
