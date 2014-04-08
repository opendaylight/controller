@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION

IF NOT EXIST "%JAVA_HOME%" (
    ECHO JAVA_HOME environment variable is not set
    EXIT /B 2
)

SET basedir=%~dp0
SET debugport=8000
SET consoleport=2400
SET jmxport=1088
SET jvmMaxMemory=
SET extraJVMOpts=
SET consoleOpts=-console -consoleLog
SET PID=

:LOOP
IF "%~1" NEQ "" (
    SET CARG=%~1
    IF "!CARG!"=="-debug" (
       SET debugEnabled=true
       SHIFT
       GOTO :LOOP
    )
    IF "!CARG!"=="-debugsuspend" (
       SET debugEnabled=true
       SET debugSuspended=true
       SHIFT
       GOTO :LOOP
    )
    IF "!CARG!"=="-debugport" (
       SET debugEnabled=true
       SET debugport=%~2
       SHIFT & SHIFT
       GOTO :LOOP
    )
    IF "!CARG!"=="-jmx" (
       SET jmxEnabled=true
       SHIFT
       GOTO :LOOP
    )
    IF "!CARG!"=="-jmxport" (
       SET jmxEnabled=true
       SET jmxport=%~2
       SHIFT & SHIFT
       GOTO :LOOP
    )
    IF "!CARG!"=="-start" (
       SET startEnabled=true
       SHIFT
       GOTO :LOOP
    )
    IF "!CARG!"=="-consoleport" (
       SET consoleport=%~2
       SHIFT & SHIFT
       GOTO :LOOP
    )
    IF "!CARG!"=="-console" (
       SHIFT
       GOTO :LOOP
    )
    IF "!CARG!"=="-status" (
       for /F "TOKENS=1" %%G in ('%JAVA_HOME%\bin\jps.exe -lvV ^| find /I "opendaylight"') do (
           set PID=%%G
       )
       if "!PID!" NEQ "" (
           ECHO Controller is running with PID !PID!
       ) else (
           ECHO Controller is not running.
       )
       GOTO :EOF
    )
    IF "!CARG!"=="-stop" (
       for /F "TOKENS=1" %%G in ('%JAVA_HOME%\bin\jps.exe -lvV ^| find /I "opendaylight"') do (
           set PID=%%G
       )
       if "!PID!" NEQ "" (
           ECHO Stopping controller PID !PID!
           TASKKILL /F /PID !PID!
       ) else (
           ECHO Controller is not running.
       )
       GOTO :EOF
    )
    IF "!CARG:~0,4!"=="-Xmx" (
       SET jvmMaxMemory=!CARG!
       SHIFT
       GOTO :LOOP
    )
    IF "!CARG:~0,2!"=="-D" (
       SET extraJVMOpts=%extraJVMOpts% !CARG!
       SHIFT
       GOTO :LOOP
    )
    IF "!CARG:~0,2!"=="-X" (
       SET extraJVMOpts=%extraJVMOpts% !CARG!
       SHIFT
       GOTO :LOOP
    )
    IF "!CARG!"=="-help" (
        ECHO "Usage: %0 [-jmx] [-jmxport <num>] [-debug] [-debugsuspend] [-debugport <num>] [-start] [-consoleport <num>]] [-stop] [-status] [-console] [-help] [<other args will automatically be used for the JVM>]"
        ECHO Note: Enclose any JVM or System properties within double quotes.
        GOTO :EOF
    )

    ECHO "Unknown option: !CARG!"
    EXIT /B 1
)

IF "%debugEnabled%" NEQ "" (
    REM ECHO "DEBUG enabled"
    SET extraJVMOpts=%extraJVMOpts% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=%debugport%
)
IF "%debugSuspended%" NEQ "" (
    REM ECHO "DEBUG enabled suspended"
    SET extraJVMOpts=%extraJVMOpts% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%debugport%
)

IF "%jvmMaxMemory%"=="" (
    SET jvmMaxMemory=-Xmx1G
    ECHO *****************************************************************
    ECHO JVM maximum memory was not defined. Setting maximum memory to 1G.
    ECHO To define the maximum memory, specify the -Xmx setting on the
    ECHO command line.
    ECHO                    e.g. run.bat -Xmx1G
    ECHO *****************************************************************"
)

SET extraJVMOpts=%extraJVMOpts%  %jvmMaxMemory%

IF "%jmxEnabled%" NEQ "" (
    REM ECHO "JMX enabled "
    SET extraJVMOpts=%extraJVMOpts% -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=%jmxport% -Dcom.sun.management.jmxremote
)
IF "%startEnabled%" NEQ "" (
    REM ECHO "START enabled "
    SET consoleOpts=-console %consoleport% -consoleLog
)

REM       Check if controller is already running
for /F "TOKENS=1" %%G in ('%JAVA_HOME%\bin\jps.exe -lvV ^| find /I "opendaylight"') do (
    SET PID=%%G
)
if "!PID!" NEQ "" (
   ECHO Controller is already running with PID !PID!
   EXIT /B 1
)


REM       Now set the classpath:
SET cp="%basedir%lib\org.eclipse.osgi-3.8.1.v20120830-144521.jar;%basedir%lib\org.eclipse.virgo.kernel.equinox.extensions-3.6.0.RELEASE.jar;%basedir%lib\org.eclipse.equinox.launcher-1.3.0.v20120522-1813.jar"

REM       Now set framework classpath
SET fwcp="file:\%basedir%lib\org.eclipse.osgi-3.8.1.v20120830-144521.jar,file:\%basedir%lib\org.eclipse.virgo.kernel.equinox.extensions-3.6.0.RELEASE.jar,file:\%basedir%lib\org.eclipse.equinox.launcher-1.3.0.v20120522-1813.jar"

SET RUN_CMD="%JAVA_HOME%\bin\java.exe" -Dopendaylight.controller %extraJVMOpts% -Djava.io.tmpdir="%basedir%work\tmp" -Djava.awt.headless=true -Dosgi.install.area=%basedir% -Dosgi.configuration.area="%basedir%configuration" -Dosgi.frameworkClassPath=%fwcp% -Dosgi.framework="file:\%basedir%lib\org.eclipse.osgi-3.8.1.v20120830-144521.jar" -classpath %cp% org.eclipse.equinox.launcher.Main %consoleOpts%

ECHO %RUN_CMD%

if "%startEnabled%" NEQ "" (
    START /B cmd /C CALL %RUN_CMD% > %basedir%\logs\controller.out 2>&1
    ECHO Running controller in the background.
) else (
    %RUN_CMD%
    EXIT /B %ERRORLEVEL%
)


