# About this project #
Do not use this software. It is not finished, subject to change and bug-ridden. However, if you really need this stuff and are the brave soul who likes to ride the depths of Hades' SOAP-service underworld on his/her flaming JVM chariot wielding the almighty sword named 'Kotlin', I very much welcome you.
I might even fix some bugs if you ask nicely (be sure to supply logs, steps to reproduce, and beer). 

# Features #

## SOAP 1.1 & 1.2 ##
This implementation is not intended to be feature-complete but one which is just enough to test a spec-compliant SOAP-service, but nothing more.
This means it might fail to detect an in theory malformed/invalid SOAP-response which parses OK.

Validators:
- HTTP Status
- HTTP Headers
- HTTP responses
- SOAP faults
- XPath, XSD
- XQUERY

## REST ##
REST services can be tested.

Validators:
- HTTP Status
- HTTP Headers
- HTTP responses
- JSON Path

## JDBC ##
JDBC services can be tested.

Validators:
- Colomn checks
- Boolean checks
- Row Count


# TODO #
The following stuff (and much more) needs to be done:
- General cleanup
- Improve logging (human-friendly v.s. technical log?)
- More test coverage. This stuff slowly becomes something a poor soul actually would be using, so some additional unit tests would be nice.
- Everything where your find // TODO in the code
- Push to Maven Central (when all of the above has been fixed)
- Parsing WSDLs
- Charsets other than UTF-8 (we default to UTF-8, which might not always work)