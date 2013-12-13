rem Inject the sanitytest jar as a controller plugin
copy .\target\dependency\sanitytest*.jar .\target\distribution.opendaylight-osgipackage\opendaylight\plugins

rem Store the current working directory in a variable so that we can get back to it later
set cwd=%cd%

rem Switch to the distribution folder
cd .\target\distribution.opendaylight-osgipackage\opendaylight

rem Run the controller
cmd.exe /c run.bat

rem Store the exit value of the controller in a variable
set success=%ERRORLEVEL%

rem Switch back to the directory from which this script was invoked
cd %cwd%

rem Remove the sanitytest jar from the plugins directory
del .\target\distribution.opendaylight-osgipackage\opendaylight\plugins\sanitytest*.jar

rem Exit using the exit code that we had captured earlier after running the controller
exit /b %SUCCESS%