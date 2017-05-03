# XSLT service

The XSLT service consumes messages from the given source queue (or wildcard), processes their content with a profidex XSLT file, and sends the results to the given destination queue.  Physically, the it is an executable jar file that can be configured via system properties or environment variables.

## Configuration and Deployment

The XSLT service is just an executable jar, the jar artifact for this module is executable. Configuration is provided by using environment variables, or system properties (it doesn't matter which).
For example,

    export xslt.file=/path/to/transform.xsl
    java -jar target/rmap-loader-transform-xsl-0.0.1-SNAPSHOT.jar -Djms.queue.dest="rmap.harvest.disco.transformed.ACM.2015-04012"


### `jms.brokerUrl` 

ActiveMQ broker URL.  Default is `tcp://localhost:61616`

### `jms.username`

JMS username.  Leave undefined if authentication is not used.

### `jms.password`

JMS password.  Leave undefined if authentication is not used.

### `jms.queue.dest`

Destination JMS queue to send transformed records to.  Default is `rmap.harvest.disco.transformed`.

### `jms.queue.src`

Source JMS queue (or wild card) to look for records to transform.  Default is `rmap.harvest.xml.>`.

### `jms.maxConnections`

Maximum number of JMS connections.  Default is 10.

### `xslt.file`

File path to the xslt file to use for the transform.

### `LOG.*`

Any environment variable or system propertu that begins with `LOG.` can be used to specify the logging level of 
the logger whose name appears after the `LOG.` characters.  For example, setting the environment variable:

    LOG.info.rmapproject=DEBUG
    
 This will set the logger called `info.rmapproject` to the `DEBUG` level. 
