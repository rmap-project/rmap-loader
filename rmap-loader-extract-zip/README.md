# Archive extractor service

This iterates through all entries in a zip, tar, or other sort of archive file, and places them into a queue for further processing.  Physically, the it is an executable jar file that can be configured via system properties or environment variables.

Once a zip/tar file has been consumed, it is renamed with a `.done` appended to the file name.

## Configuration and Deployment

The archive extractor service is just an executable jar, the jar artifact for this module is executable.  A list of files to consume (or a directory containing such files) is passed as an argument to the executable

Additional configuration is provided by using environment variables, or system properties (it doesn't matter which).  

For example,

    java -Dcontent.type='application/vnd.rmap-project.disco+rdf+xml' \
         -Djms.queue.dest=rmap.harvest.disco.ieee.2018-01-11 \
         -jar /rmap-loader-extract-zip-0.0.1-SNAPSHOT.jar /path/to/directory/*.zip

or

    java -Dcontent.type='application/vnd.rmap-project.disco+rdf+xml' \
         -Djms.queue.dest=rmap.harvest.disco.ieee.2018-01-11 
         -Ddir=/path/to/zip 
         -Dfilter='*.zip' 

### `dir`

Specify a directory in which to look for zip files.  All files in this directory will be consumed, unless otherwise filtered by `filter`.  This is used as an alternative to specifying the files
on the command line.

### `filter`
Filter the input zip/tar files based on a [glob](https://javapapers.com/java/glob-with-java-nio/) pattern, for example
`*.zip`, or `**/*.zip` (for recursive behaviour in a complex ).  Only 
necessary if _not_ specifying files on the command line (e.g. by using the `dir` property)

### `jms.brokerUrl` 

ActiveMQ broker URL.  Default is `tcp://localhost:61616`

### `jms.username`

JMS username.  Leave undefined if authentication is not used.

### `jms.password`

JMS password.  Leave undefined if authentication is not used.

### `jms.queue.dest`

Destination JMS queue to send unzipped documents to.  Default is `rmap.harvest.xml.zip`

### `jms.maxConnections`

Maximum number of JMS connections.  Default is 10.

### `LOG.*`

Any environment variable or system propertu that begins with `LOG.` can be used to specify the logging level of 
the logger whose name appears after the `LOG.` characters.  For example, setting the environment variable:

    LOG.info.rmapproject=DEBUG
    
 This will set the logger called `info.rmapproject` to the `DEBUG` level. 
