# Generic-File-Driver-for-IDM
Generic File Driver for IDM
Introduction

The Generic File Driver is similar to the Text Driver shipped with IDM, but has more options, and has the capability to read virtually any file type. Out of the box, the following file types are supported: XML, CSV and XLS (the latter using POI from Apache).

Main differentiators to NetIQ’s driver:

    Produces and consumes regular NDS documents (no need for XSLT conversion).
    Accepts add, modify or instance documents on the subscriber channel.
    Auto-query back for missing attributes on modify commands.
    GCV (Ecmascript) for generating associations and source DN’s.
    Publisher channels processes files in a streaming mode. A maximum of 20 records are prefetched/buffered in memory (whenever support by the file type: CSV and XML).
    Publisher channel supports interruption (driver stop command) during file processing.
    Publisher channel can add metadata about the file or record processed (eg: record number, filename, last record indicator, etc).

The original version I wrote in 2007 is located at http://www.novell.com/coolsolutions/tools/18671.html. This update comes with an IDM 4 package making the installation simpler and has some new features and fixes (see below).

See the referenced version for a list of the initial features.

 
New features and fixes
version 0.8A, 3-MARCH-2015 (package 0.7)

    Shim feature – general – reverted back to java 1.6 compatible ‘move file’ operation.

version 0.8, 14-SEPT-2014 (package 0.7)

    Package bug: fixed typo in example xslt template for XML publisher files. See forum thread.
    Technical – general refactoring of the shim.
    Shim feature – publisher – added option to remove old publisher files. Disabled by default for backward compatibility.
    Shim feature – publisher – added option to disable the publisher channel.
    Shim feature – publisher – added additional options in the command to generate (“Dynamic Input based” and “Delete”)
    Shim feature – subscriber – added option to disable the “max number of records in generated files” by setting it to 0 (zero).
    Shim feature – subscriber – when a modify command is ’empty’ (=all fields defined in the schema are missing), but the content indicates that the output file should be closed (eg: the file close field is set to true), no query back is performed in order to add a record to the output file. It is assumed that the only purpose of this modify command is to close the output file.
    Shim feature – general – added option to replace illegal ECMA script characters in field names (eg for calculating the association). Disabled by default for backward compatibility.
    Shim feature – general – added option to include the driver’s ECMA script libraries into the shim (for complex ECMA script operation). Disabled by default for backward compatibility.

version 0.7b, 14-MAY-2014 (package 0.6)

    Shim bug: case issue with the new configuration parameter to configure the command generated on the publisher channel

version 0.7, 07-MAY-2014 (package 0.6)

    Shim bug: specifying no encoding on the XML reader caused an Exception (see also this cool solution post)
    Shim feature: added option to configure the command generated on the publisher channel (add, modify or dynamic).
    Package bug: xml and xls file readers had invalid class names (see the same cool solutions post)
    Package feature: changed xls entry field to multi-line

version 0.6, 28-FEB-2014 (package 0.5)

    Shim/package feature: Added configuration parameter to use Quartz scheduler to close subscriber generated files using a cron-like expression string.
    Shim feature: Added slf4j binding (separate jar) for Idm tracing, allowing Quartz logs to be shown in idm trace files.
    Shim bug: possible NPE when the publisher folder becomes unreachable (eg due to network outage).
    Shim feature: Added option for every strategy to receive driver shutdown notification by implementing IShutdown.
    Shim bug/feature: Return an empty instance document when we receive an associated query on the subscriber channel. Reason: support of the merge attribute filter rules in case of a ‘sync’ event.

version 0.5, 21-JAN-2013 (package 0.4)

    Shim bug: Fixed an issue wrt the driver not adding the metadata on the publisher channel.

version 0.4, 24-APR-2012 (package 0.4)

    Shim bug: Fixed an issue with the driver schema (appeared to be hard coded).
    Shim feature: Added beta support for query back. Query back only supported on publisher channel (when reading in a file). Feedback welcome.
    Package bug: Fixed an issue that caused the driver configuration not to be imported when the package was imported.
    Package bug: Changed creation policy from validating the Given Name to validating the Surname.
    Package feature: Added package ‘Delimited Text Driver Password Synchronization Package’ as an optional package for this driver.
    Package feature: Added remote loader prompts to the package.

version 0.3, 12-OCT-2011

    Shim feature: automatic association and src-dn calculation (using ecmascript in the driver configuration). No longer need for style sheets for just creating association and src-dn values. The driver shim now does this for you.
    Shim bug: fixed missing remove-all-values on the publisher documents.
    Shim feature: added the ability (csv output files) to flush the file buffer after every record written (useful for when you are developing/testing your driver).
    Shim feature: when receiving a modify event on the subscriber channel, the shim will query back for all missing attribute values in order to be able to write a complete record (no need for a stylesheet to query for missing attributes and keeping this stylesheet in sync with the schema).
    Shim bug/feature: the subscriber channel now fully support add, modify and instance events. Instance and add events are assumed to contain all data and do not perform a query back. Only the modify event performs a query back

 
Installation

The attached zip file (GenFileDriver_0_8.zip) contains (beside some documentation) 2 main jar files and a lib folder. One jar is the package, one jar is the shim. The lib folder contains optional jars.

 

Shim: GenFileDriverShim_0.7.jar
Copy the shim to you eDirectory server (DirXML’s classes folder). If you want to use XLS support, also download poi from Apache. Restart eDirectory after this.

Note: if you have an older version of the shim, be sure to delete that jar.
Package: SVCGENFILEB_0.0.6.20140508075632.jar
Import this package into your designer project. Drag and drop the “Delimited Text” driver that is located under “Tools” and select the “Generic File Base” package, answering all the questions as needed.
From this point on, you can use and modify it as any other driver.

lib folder:
This folder contains some optional jars. These are only required if you plan to use quartz scheduler to close the generated files using a cron-like string. Jars required when doing this are: quartz-2.2.1.jar, slf4j-api-1.7.5.jar and slf4j-idmTrace.jar.
Policies

This driver only has 3 policies; all of them on the publisher channel. If required, you could add the password policies as available from the standard text driver (by creating your own copy from Novell’s package and adding this to the driver).
SVCGENFILEB-pub-mp
Match on Internet Email Address if any available.
SVCGENFILEB-pub-cp
Checks for mandatory attributes:
Surname: required due to eDir constraints
Internet Email Address: required due to use in matching policy
SVCGENFILEB-pub-pp
Placement based on a driverset GCV (idv.dit.data.users).
Configuration

All configuration options should have a good description. Most of them are also described on the initial release page.
