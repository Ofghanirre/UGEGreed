# UGE-Greed

This is the repositery for the UGE Greed project for the network programming class from the CompSci master's degree at 
Université Gustave Eiffel for students Clément GAUDET and Antonin JEAN.

The implementation is fully in non blocking TCP.

## Console Usage:

> Help Command : help

##  Console Help:
### Commands (5)

```shell
HELP - DEBUG - START - DISCONNECT - CACHE

COMMAND -*- HELP
    USAGE:
    HELP
    DESCRIPTION :
    Help command to list all possibles commands and their usages
    
    COMMAND -*- DEBUG
    USAGE:
    DEBUG code
    PARAMETERS :
    - code -> code for command : 1 : Potential
    DESCRIPTION :
    Debug commands used to test code

COMMAND -*- START
USAGE:
START url-jar fully-qualified-name start-range end-range filename
PARAMETERS :
- url-jar -> jar url
- fully-qualified-name -> the fully qualified name of the class contained in the jar implementing the interface fr.uge.ugegreed.Checker
- start-range -> the first value to test
- end-range -> the last value to test
- filename  -> the name of the output file to store result in
DESCRIPTION :
Start the checking of a given conjecture

COMMAND -*- DISCONNECT
USAGE:
DISCONNECT
DESCRIPTION :
Initiate the disconnection of the network and the application stop

COMMAND -*- CACHE
USAGE:
CACHE use-cache
PARAMETERS :
- use-cache -> wether or not to use cached file if they exists for the requests
DESCRIPTION :
Enable/Disable the usage of cached file
```