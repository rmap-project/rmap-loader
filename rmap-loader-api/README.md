# RMap harvest/loader API

The primary class of interest in this module is [HarvestRecord](rmap-loader-api/src/main/java/info/rmapproject/loader/HarvestRecord.java), which represents an abstraction of a document (as a blob, just bytes), and minimal metadata used for accounting or provenance purposes.  This is the primary user-facing abstraction used by developers who read or write to message queues using the [jms client](rmap-loader-jms/README.md).


## HarvestRecord
The [HarvestRecord](rmap-loader-api/src/main/java/info/rmapproject/loader/HarvestRecord.java) encapsulates binary content (via `getBody()` and `setBody()`), and RecordInfo metadata via `getRecordInfo()` and `setRecordInfo()`

## RecordInfo

[RecordInfo](rmap-loader-api/src/main/java/info/rmapproject/loader/model/RecordInfo.java) contains information about specific records/documents at various points in the harvest.  Properties include:

### id

URI identifying a record.  Used for correlation purposes when depositing DiSCOs that are derived from records.  For example, an id of `urn:oai:theOaiSource:oaiRecordID` could be used to represent a specific OAI record that may be updated at some future point.

### date

Represents the logical date of a record.  Used for determining if RMap has the latest version of a given record.

### content type

Media type of the given record.

### src

Optional.  URI representing the source of a given record.  Used for provenance purposes

### harvestInfo

Optional.  Metadata describing the circumstances of the harvest that created the record.

## HarvestInfo

[HarvestInfo](rmap-loader-api/src/main/java/info/rmapproject/loader/model/HarvestInfo.java) contains basic metadata describing a particular harvest, strictly for provenance purposes.  

### id

Unique ID of a given harvest

### src

Source of the harvest

### date

Date of the harvest.
