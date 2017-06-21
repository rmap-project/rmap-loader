# RMap loader run registry

The run registry is an optional tool that can be used to record and retrieve the most recent harvest date for a harvest run.  If used along side the [DiSCO deposit service](../rmap-loader-deposit-disco), the two will share a database. 

## Configuration and deployment

As with the [DiSCO deposit service](../rmap-loader-deposit-disco), configuration is provided by using environment variables, or system properties (it doesn't matter which).   

### `jdbc.url`

JDBC url, e.g `jdbc:postgresql://localhost/test`.  By default, it uses a non-durable in-memory sqlite database. 

Supported JDBC drivers include:
* sqlite.  This is especially useful for simply persisting to a file, without installing a RDBMS, e.g. `jdbc:sqlite:/path/to/rmap.db`
* postgresql

To use other JDBC drivers, add their jar to the classpath when running the deposit service jar, and specify an appropriate jdbc URI.

### `jdbc.username`

RDBMS username

### `jdbc.password`

RDBMS password.

### `LOG.*`

Any environment variable or system property that begins with `LOG.` can be used to specify the logging level of 
the logger whose name appears after the `LOG.` characters.  For example, setting the environment variable:

    LOG.info.rmapproject=DEBUG
    
 This will set the logger called `info.rmapproject` to the `DEBUG` level. 
