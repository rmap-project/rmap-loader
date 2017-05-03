# RMap harvest/loader framework

The RMap harvest/loader framework is a loosely coupled set of components for extracting source data, transforming it into RMap DiSCOs, and adding or updating these DiSCOs in RMap.  Components may be read and write to durable message queues.  As such, they are loosely coupled and may be implement any logic in any language to perform individual tasks.  A simple and convenient java-based [JMS client](rmap-loader-jms/README.md) is provided, but clients may use any convenient messsaging library to interact with the queues (such as Apache Camel).

## Message queues

The harvest framework relies on message queues to store raw, intermediate, and final documents as they are harvested, transformed, and ultimately loaded.  Naming these queues follows a convention: `rmap.harvest.${format}.*`.  In other words, all queue names begin with `rmap.harvest`, followed by a string representing the "format" of document contained in the message, followed optionally by additional string(s) that provide meaning to humans in telling queues apart based upon source, procecessing method, etc.

If records cannot be successfully processed, the convention is to put them in a queue named `rmap.harvest.error.${format}.*`

The RMap harvest/loader framework has been tested with the Apache ActiveMQ message broker.  ActiveMQ supports [wildcards](http://activemq.apache.org/wildcards.html) in queue names in order to match multiple queues for consuming messages.  Using wildcards important for ease of configuration.  For example, relying on our queue naming convention; the following queue specification will consume from all queues containing DiSCOs:

    rmap.harvest.disco.>
    
### Example

Suppose we havest

Let's say that records are harvested from OAI, transformed into DiSCOs, and then loaded into RMap.

It would be reasonable for an OAI harvester service to place raw, individual OAI records into a queue:

    rmap.harvest.oai_dc.the_source.2017-05-01.try01
    
Next, let's suppose we have a service that transforms `oai_dc` records into DiSCOs.  It listens on a wildcard queue:

    rmap.harvest.oai_dc.>
    
.. so it picks up messages our OAI harvester places on the queue.  When it transforms, it places the results on the queue:

    rmap.harvest.disco.transformed.oai_dc.xsl.the_source.2017-05-01.try01
    
Any failed transforms are thrown onto the queue:

    rmap.harvest.error.disco.transformed.oai_dc.xsl.the_source.2017-05-01.try01
    
Now, our DiSCO loader service listens on:

    rmap.harvest.disco.>
    
It uploads the discos from our queue (and any others).

We can later inspect the content of the error queue, and decide the fate of the failed records from the transform.  If they are fixable, we fix them, and throw them back onto the queue they came from, or just create a new queue

    rmap.harvest.disco.transformed.oai_dc.xsl.the_source.2017-05-01.try01.FIXED
    
### Reporting and monitoring

There are several tools available for monitoring activeMQ queues.  The built-in [activemq web console](http://activemq.apache.org/web-console.html) is good start.  Something like [hawtio](http://bennet-schulz.com/2016/07/apache-activemq-and-hawtio.html) has more features.  This is where a good naming convention for queues pays off.  To be able to tell at a glance the significance of a given queue is quite helpful.

## Services

Services are the things that read or write to queues.  In this repository, they are distributed simply as executable jars that can be run whenever desired.  

### Extractors

* [OAI](rmap-loader-extract-oai/README.md) - simple OAI harvester that slurps in records from an OAI endpoint, and places the records into the desired queue.  

* [zip](rmap-loader-extract-zip/README.md) - places all files contained within a zip file onto a queue

### Transformers

* [xsl](rmap-loader-transform-xsl/README.md) - Transforms files from a specified queue with the specified xslt file.  Uses Saxon, and supports transforms that output multiple records for a given input document

### Loaders

* [disco](rmap-loader-deposit-disco/README.md) - Loads records from disco queues into RMap.  Maintains a registry of deposited records so that it can decide whether an update is appropriate, or an add.  

## Deployment

* Install an instance of ActiveMQ.  The easiest way to do this would be to [download](http://activemq.apache.org/download.html) the binary distribution.  
* Run any of the above services when desired.
