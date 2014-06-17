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
SET JAVA_H=%JAVA_HOME%\bin\jps.exe

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
       for /F "TOKENS=1" %%G in ('""!JAVA_H!" -lvV ^| find /I "opendaylight""') do (
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
       for /F "TOKENS=1" %%G in ('""!JAVA_H!" -lvV ^| find /I "opendaylight""') do (
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
       SET extraJVMOpts=!extraJVMOpts! !CARG!
       SHIFT
       GOTO :LOOP
    )
    IF "!CARG:~0,2!"=="-X" (
       SET extraJVMOpts=!extraJVMOpts! !CARG!
       SHIFT
       GOTO :LOOP
    )
    IF "!CARG!"=="-help" (
        SHIFT
        SET CARG=%2
        IF "!CARG!" NEQ "" (
             CALL:!CARG!
        ) ELSE (
              CALL:helper
         )
        GOTO :EOF
    )

    ECHO "Unknown option: !CARG!"
    EXIT /B 1
)

IF "%debugEnabled%" NEQ "" (
    REM ECHO "DEBUG enabled"
    SET extraJVMOpts=!extraJVMOpts! -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=%debugport%
)

IF "%debugSuspended%" NEQ "" (
    REM ECHO "DEBUG enabled suspended"
    SET extraJVMOpts=!extraJVMOpts! -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%debugport%
)

IF "%jvmMaxMemory%"=="" (
    SET jvmMaxMemory=-Xmx1G
    ECHO Setting maximum memory to 1G.
)

SET extraJVMOpts=!extraJVMOpts!  %jvmMaxMemory%

IF "%jmxEnabled%" NEQ "" (
    REM ECHO "JMX enabled "
    SET extraJVMOpts=!extraJVMOpts! -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=%jmxport% -Dcom.sun.management.jmxremote
)
IF "%startEnabled%" NEQ "" (
    REM ECHO "START enabled "
    SET consoleOpts=-console %consoleport% -consoleLog
)

REM       Check if controller is already running
for /F "TOKENS=1" %%G in ('""!JAVA_H!" -lvV ^| find /I "opendaylight""') do (
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

SET RUN_CMD="%JAVA_HOME%\bin\java.exe" -Dopendaylight.controller !extraJVMOpts! -Djava.io.tmpdir="%basedir%work\tmp" -Djava.awt.headless=true -Dosgi.install.area=%basedir% -Dosgi.configuration.area="%basedir%configuration" -Dosgi.frameworkClassPath=%fwcp% -Dosgi.framework="file:\%basedir%lib\org.eclipse.osgi-3.8.1.v20120830-144521.jar" -classpath %cp% org.eclipse.equinox.launcher.Main %consoleOpts%

ECHO !RUN_CMD!

if "%startEnabled%" NEQ "" (
    START /B cmd /C CALL !RUN_CMD! > %basedir%\logs\controller.out 2>&1
    ECHO Running controller in the background.
    EXIT /B 1
) else (
    !RUN_CMD!
    EXIT /B %ERRORLEVEL%
)

:helper
echo. For more information on a specific command, type -help command-name.
echo.
echo   jmx              ^[-jmx^]
echo   jmxport          ^[-jmxport ^<num^>^] - DEFAULT is 1088
echo   debug            ^[-debug^]
echo   debugsuspend     ^[-debugsuspend^]
echo   debugport        ^[-debugport ^<num^>^] - DEFAULT is 8000
echo   start            ^[-start ^[^<console port^>^]^] - DEFAULT port is 2400
echo   stop             ^[-stop^]
echo   status           ^[-status^]
echo   console          ^[-console^]
echo   agentpath        ^[-agentpath:^<path to lib^>^]
exit/B 1

:debugsuspend
ECHO.
ECHO. debugsuspend     ^[-debugsuspend^]
ECHO.
ECHO. This command sets suspend on true in runjdwp in extra JVM options. If its true, VMStartEvent has a suspendPolicy of SUSPEND_ALL. If its false, VMStartEvent has a suspendPolicy of SUSPEND_NONE.
ECHO.
EXIT /B 1

:debugport
ECHO.
ECHO. debugport        ^[-debugport ^<num^>^] - DEFAULT is 8000
ECHO.
ECHO. Set address for settings in runjdwp in extra JVM options.
ECHO. The address is transport address for the connection.
ECHO. The address has to be in the range ^[1024,65535^]. If the option was not call, port will be set to default value.
ECHO.
EXIT /B 1

:jmxport
ECHO.
ECHO. jmxport          ^[-jmxport ^<num^>^] - DEFAULT is 1088
ECHO.
ECHO.    Set jmx port for com.sun.management.jmxremote.port in JMX support. Port has to be in the range ^[1024,65535^]. If this option was not call, port will be set to default value.
ECHO.
EXIT /B 1

:debug
ECHO.
ECHO. debug            [-debug]
ECHO.
ECHO. Run ODL controller with -Xdebug and -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=^$^{debugport^}
ECHO.
ECHO.    -Xdebug enables debugging capabilities in the JVM which are used by the Java Virtual Machine Tools Interface (JVMTI). JVMTI is a low-level debugging interface used by debuggers and profiling tools.
ECHO.
ECHO.    -Xrunjdwp option loads the JPDA reference implementation of JDWP. This library resides in the target VM and uses JVMDI and JNI to interact with it. It uses a transport and the JDWP protocol to communicate with a separate debugger application.
ECHO.
ECHO. settings for -Xrunjdwp:
ECHO.            transport -  name of the transport to use in connecting to debugger application
ECHO.            server    -  if 'y', listen for a debugger application to attach; otherwise, attach to the debugger application at the specified address
ECHO.                      -  if 'y' and no address is specified, choose a transport address at which to listen for a debugger application, and print the address to the standard output stream
ECHO.            suspend   -  if 'y', VMStartEvent has a suspend Policy of SUSPEND_ALL
ECHO.                      -  if 'n', VMStartEvent has a suspend policy of SUSPEND_NONE
ECHO.            address   -  transport address for the connection
ECHO.                      -  if server=n, attempt to attach to debugger application at this address
ECHO.          -  if server=y, listen for a connection at this address
ECHO.
EXIT /B 1

:jmx
ECHO.
ECHO. jmx              [-jmx]
ECHO.
ECHO. Add JMX support. With settings for extra JVM options: -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=^$^{jmxport^} -Dcom.sun.management.jmxremote
ECHO. jmxport can by set with option -jmxport ^<num^>. Default num for the option is 1088.
ECHO.
EXIT /B 1

:stop
ECHO.
ECHO. stop             ^[-stop^]
ECHO.
ECHO. If a controller is running, the command stop controller. Pid will be clean.
ECHO.
EXIT /B 1

:status
ECHO.
ECHO. status           ^[-status^]
ECHO.
ECHO. Find out whether a controller is running and print it.
ECHO.
EXIT /B 1

:start
ECHO.
ECHO. start            ^[-start ^[^<console port^>^]^]
ECHO.
ECHO.    If controller is not running, the command with argument^(for set port, where controller has start^) will start new controller on a port. The port has to be in the range ^[1024,65535^]. If this option was not call, port will be set to default value. Pid will be create.
EXIT /B 1

:console
ECHO.
ECHO. console          [-console]
ECHO.     Default option.
EXIT /B 1

:agentpath
ECHO.
ECHO. agentpath        ^[-agentpath:^<path to lib^>^]
ECHO.
ECHO.    Agentpath option passes path to agent to jvm in order to load native agent library, e.g. yourkit profiler agent.
EXIT /B 1


