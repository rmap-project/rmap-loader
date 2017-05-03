# DiSCO deposit service

The DiSCO deposit service consumes DiSCOs from message queues, and attempts to deposit them in RMap.  Records that end in a failure (for any reason, including network hiccups) will be sent to an error queue.  

The deposit service maintains a database of records it deposited.  If messages in the queue contain [RecordInfo](rmap-loader-api/src/main/java/info/rmapproject/loader/model/RecordInfo.java) headers, this information will be used to determine if a DiSCO is new, or represents an update to an existing DiSCO.  Absent this information, all DiSCOs are assumed to be new.

Physically, the deposit service is an executable jar file that can be configured via system properties or environment variables.  It can be run continuously in the background to continuously monitor queues and deposit DiSCOs immediatly when available, or be run on-demand, or on a schedule (e.g. by using `cron`, etc).

## Messages and queues.

The DiSCO deposit service blindly deposits the body of all messages consumed from queues.  By default, it uses the wildcard queue:

    rmap.harvest.disco.>
    
If the message has a `Content-Type` header, it will use that media type when uploading to RMap.  Otherwise, it uses `application/vnd.rmap-project.disco+rdf+xml`. Errors go into an error queue based upon the name of the queue a partular message came from.  So if a message was consumed from

    rmap.harvest.disco.a.b.c.d
    
.. an error will go to

    rmap.harvest.error.a.b.c.d
    
Message headers relevant for DiSCO accounting are listed in [JmsHeaders](rmap-loader-jms/src/main/java/info/rmapproject/loader/jms/JmsHeaders.java).  If using the [JMSClient](rmap-loader-jms/src/main/java/info/rmapproject/loader/jms/JmsClient.java) with the [HarvestRecord](rmap-loader-api/src/main/java/info/rmapproject/loader/HarvestRecord.java) abstraction, mapping to or from JMS headers occurs automatically.

## Accounting

The DiSCO loader keeps the id, date, and disco identifier in a relational database.  

A record's ID is found in jms header `rmap.harvest.record.id`, or accessible by `harvestRecord.getRecordInfo().getId()`.  It is a URI, and is presumed to be globally unique.  The date is `rmap.harvest.record.date`, or `harvestRecord.getRecordInfo().getDate()`, and represents the logical date of a record.  

When the RMap deposit service recieves a record, it checks its database for the presence of a matching ID.  If one matches, it compares dates.  If the provided date is newer than the one in the database, it'll update the corresponding DiSCO (the Disco ID is in the database).  If the provided date is older or equal (i.e. already in RMap), it skips the record and discards it.  If not present, it will deposit a new record to RMap, and create an entry.

## Configuration and deployment

The disco deposit service is just an executable jar, the jar artifact for this module is executable.
Configuration is provided by using environment variables, or system properties (it doesn't matter which).   
For example, 

    export jdbc.password=myPassword
    java -jar target/rmap-loader-deposit-disco-0.0.1-SNAPSHOT.jar -Djdbc.username=user

### `jdbc.url`

JDBC url, e.g `jdbc:postgresql://localhost/test`.  By default, it uses a non-durable in-memory sqlite database. 

Supported JSBC drivers include:
* sqlite.  This is especially useful for simply persisting to a file, without installing a RDBMS, e.g. `jdbc:sqlite:/path/to/rmap.db`
* postgresql
* mysql

### `jdbc.username`

RDBMS username

### `jdbc.password`

### `jms.brokerUrl` 

ActiveMQ broker URL.  Default is `tcp://localhost:61616`

### `jms.username`

JMS username.  Leave undefined if authentication is not used.

### `jms.password`

JMS password.  Leave undefined if authentication is not used.

### `jms.maxConnections`

Maximum number of JMS connections.  Default is 10.

RDBMS password.

### `jms.queue.src`

Queue to consume DiSCOs from.  By default, it's the wildcard `rmap.harvest.disco.>`

### `rmap.api.auth.token`

RMap authentication token.

### `rmap.api.baseuri`

RMap API base URI.  The default is `https://test.rmap-project.org/apitest/`

### `LOG.*`

Any environment variable or system propertu that begins with `LOG.` can be used to specify the logging level of 
the logger whose name appears after the `LOG.` characters.  For example, setting the environment variable:

    LOG.info.rmapproject=DEBUG
    
 This will set the logger called `info.rmapproject` to the `DEBUG` level. 
