# About this project #
Do not use this software. It is not finished, subject to change and bug-ridden. However, if you really need this stuff and are the brave soul who likes to ride the depths of Hades' SOAP-service underworld on his/her flaming JVM chariot wielding the almighty sword named 'Kotlin', I very much welcome you.
I might even fix some bugs if you ask nicely (be sure to supply logs, steps to reproduce, and beer). 

# Features #

## SOAP ##
This implementation is not intended to be a feature-complete but one which is just enough to test your typical spec-compliant SOAP-service, but nothing more.
This means it fail detect an in theory malformed SOAP-response (parsable, but strictly not correct).

## REST ##

## JDBC ##

# TODO #
The following stuff (and much more) needs to be done:
- Getting the default namespace in XPath validators to work
- Parsing WSDLs
- Sending MTOM (receiving works)
- SwA support (only MTOM/XOP is supported right now)
- General cleanup
- Improve logging (human-friendly v.s. technical log?)