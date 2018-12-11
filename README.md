# Generic-File-Driver-for-IDM
## Introduction

The Generic File Driver is similar to the Text Driver shipped with IDM, but has more options, and has the capability to read virtually any file type. Out of the box, the following file types are supported: XML, CSV and XLS (the latter using POI from Apache).

Main differentiators to NetIQ’s driver:

* Produces and consumes regular NDS documents (no need for XSLT conversion).
* Accepts add, modify or instance documents on the subscriber channel.
* Auto-query back for missing attributes on modify commands.
* GCV (Ecmascript) for generating associations and source DN’s.
* Publisher channels processes files in a streaming mode. A maximum of 20 records are prefetched/buffered in memory (whenever support by the file type: CSV and XML).
* Publisher channel supports interruption (driver stop command) during file processing.
* Publisher channel can add metadata about the file or record processed (eg: record number, filename, last record indicator, etc).

The original version I wrote in 2007 is located at http://www.novell.com/coolsolutions/tools/18671.html. Latest manual jar/package release was posted on https://www.netiq.com/communities/cool-solutions/cool_tools/generic-file-driver-idm-v-003/.

## Documentation
See http://vancauwenberge.info/#txtdriver

## New features and fixes
See http://vancauwenberge.info/#txtdriver_history
