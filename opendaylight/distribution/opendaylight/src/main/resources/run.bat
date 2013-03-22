@ECHO OFF
SETLOCAL
SETLOCAL ENABLEDELAYEDEXPANSION
IF EXIST "%JAVA_HOME%" (
   	 	set basedir=%~dp0
REM       Now set the classpath:
                set cp="!basedir!lib\org.eclipse.osgi-3.8.1.v20120830-144521.jar;!basedir!lib\org.eclipse.virgo.kernel.equinox.extensions-3.6.0.RELEASE.jar;!basedir!lib\org.eclipse.equinox.launcher-1.3.0.v20120522-1813.jar"

REM       Now set framework classpath
               set fwcp="file:\!basedir!lib\org.eclipse.osgi-3.8.1.v20120830-144521.jar,file:\!basedir!lib\org.eclipse.virgo.kernel.equinox.extensions-3.6.0.RELEASE.jar,file:\!basedir!lib\org.eclipse.equinox.launcher-1.3.0.v20120522-1813.jar"

REM echo cp: %cp%
REM echo fwcp: %fwcp%

REM echo %JAVA_HOME%\bin\java.exe %* -Djava.io.tmpdir=!basedir!work\tmp -Dosgi.install.area=!basedir! -Dosgi.configuration.area=!basedir!configuration -Dosgi.frameworkClassPath=%fwcp% -Dosgi.framework=file:\!basedir!lib\org.eclipse.osgi-3.8.1.v20120830-144521.jar -classpath %cp% org.eclipse.equinox.launcher.Main -console -consoleLog
"%JAVA_HOME%\bin\java.exe" %* -Djava.io.tmpdir="!basedir!work\tmp" -Dosgi.install.area=!basedir! -Dosgi.configuration.area="!basedir!configuration" -Dosgi.frameworkClassPath=!fwcp! -Dosgi.framework="file:\!basedir!lib\org.eclipse.osgi-3.8.1.v20120830-144521.jar" -classpath !cp! org.eclipse.equinox.launcher.Main -console -consoleLog

) ELSE (
    ECHO JAVA_HOME environment variable is not set
    PAUSE
)
ENDLOCAL