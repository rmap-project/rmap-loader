# Zip extractor service

This iterates through all entries in a zip file, and places them into a queue for further processing.  Physically, the it is an executable jar file that can be configured via system properties or environment variables.

Once a zip file has been consumed, it is placed into a `.done` folder in the current directory

## Configuration and Deployment

The zip extractor service is just an executable jar, the jar artifact for this module is executable. Configuration is provided by using environment variables, or system properties (it doesn't matter which).
For example,

    export import.directory=/path/to/dir
    java -jar target/rmap-loader-extract-zip-0.0.1-SNAPSHOT.jar -Dimport.file=stuff.zip

### `import.directory`

Specify a directory in which to look for zip files.  All files in this directory will be consumed, unless otherwise restricted by `input.file`.  If not suecified, then the current workinf directory will be used.

### `import.file` 

Specify a particular file to process.  Note, this is a _name_ and not a path, e.g. "whatever.zip" vs "/path/to/file.zip".

### `jms.brokerUrl` 

ActiveMQ broker URL.  Default is `tcp://localhost:61616`

### `jms.username`

JMS username.  Leave undefined if authentication is not used.

### `jms.password`

JMS password.  Leave undefined if authentication is not used.

### `jms.maxConnections`

Maximum number of JMS connections.  Default is 10.

### `LOG.*`

Any environment variable or system propertu that begins with `LOG.` can be used to specify the logging level of 
the logger whose name appears after the `LOG.` characters.  For example, setting the environment variable:

    LOG.info.rmapproject=DEBUG
    
 This will set the logger called `info.rmapproject` to the `DEBUG` level. 
