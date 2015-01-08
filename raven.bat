@echo off
SETLOCAL

# ------------------------------------------------------------------------------
# -- What is Raven?
# A DOS Batch wrapper around mvn to save output to file and open it in an editor
# By Robert Mark Bram
# https://github.com/robertmarkbram/MavenUtilities
# http://robertmarkbramprogrammer.blogspot.com.au/2015/01/do-you-use-mvn-on-dos-prompt.html
# ------------------------------------------------------------------------------
:: Set-up Dependencies first.
:: - This script
::    - Save this script as raven.bat in some folder you like to use for utilities etc.
::    - Add the path (folder/directory name) where this script lives to your PATH (*see note 1*).
::    - Otherwise you will have to use the absolute path to this script: e.g. "C:\myApps\Batch\raven" instead of just "raven".
:: - Maven
::    - Download it from: http://maven.apache.org/
::    - Set M2_HOME variable (*see note 1*) or LOCAL_M2_HOME in this script.
:: - Java
::    - Download and install it from: http://www.oracle.com/technetwork/java/javase/downloads/index.html
::    - Set JAVA_HOME variable (*see note 1*) or LOCAL_JAVA_HOME in this script.
:: - mtee.exe (to send output to file and console)
::    - Download mtee.exe from http://www.commandline.co.uk/mtee/
::    - Add the path (folder/directory name) where mtee.exe lives to your PATH (*see note 1*) or set MTEE variable in this script.
:: - Your favourite text editor.
::    - This script defaults to notepad. If you like it, do nothing.
::    - If you want to use something else, set EDITOR in this script
:: - Temp dir
::    - This script defaults to %TEMP%\maven.
::    - If you want to use something else, set TMPDIR in this script

:: How to use this script.
:: - Instead of using "mvn", just use "raven", e.g.
::       raven clean deploy
::       C:\myApps\Batch\raven clean deploy

:: Note 1 - changing PATH or other environment variables on Win7 and above: two options.
:: - OPTION 1: Start button > Search for "Environment Variables for your account" > modify PATH (or other variable) in top section, "user variables for USERNAME"
::    - No re-boot required, just restart the DOS prompt.
::    - PATH is set only for your user. Other logged users will not see it.
:: - OPTION 2: Start button > Search for "Edit the System Environment Variables" > Environment Variables > modify PATH (or other variable) in bottom section, "System Variables"
::    - Re-boot required.
::    - PATH is set for all logged in users.

:: History
:: Wednesday 07 January 2015, 07:07:33 PM
:: - Updated to check java, maven, mtee temp dir and editor variables.
:: Thursday 08 January 2015, 04:57:36 PM
:: - Modified the way this script looks for maven and java such that it always uses local versions first.


:: ############################################################################
:: Edit these variables - dependencies of this script.
:: ############################################################################
:: set EDITOR=C:\Program Files (x86)\IDM Computer Solutions\UltraEdit\Uedit32.exe
set EDITOR=notepad
set LOCAL_JAVA_HOME=C:\Program Files\Java\jdk1.7.0_67
set LOCAL_M2_HOME=C:\apps\apache-maven-3.2.3
set MTEE=C:\apps\mtee.exe
set TMPDIR=%TEMP%\maven
set MAVEN_OPTS=-Xms512m -Xmx1024m -XX:MaxPermSize=256m
:: ############################################################################
:: DO NOT EDIT BELOW HERE
:: ############################################################################

:: Create timestamp.
SET hh=%time:~0,2%
if "%time:~0,1%"==" " SET hh=0%hh:~1,1%
SET YYYYMMDD_HHMMSS=%date:~10,4%%date:~7,2%%date:~4,2%_%hh%%time:~3,2%%time:~6,2%
set LOG_FILE=%TMPDIR%\maven_%YYYYMMDD_HHMMSS%.txt

:: Check temp dir.
if not exist %TMPDIR% mkdir %TMPDIR%

:: Check mtee.exe
:: Check in path first.
set MTEE_FOUND=
for %%e in (%PATHEXT%) do (
  for %%X in (mtee%%e) do (
    if not defined MTEE_FOUND (
      set MTEE_FOUND=%%~$PATH:X
    )
  )
)
if "%MTEE_FOUND%" == "" (
   :: Not in path. Check script variable.
   if not exist %MTEE% (
      echo.
      echo **************
      echo Please download mtee.exe from http://www.commandline.co.uk/mtee/
      echo and update MTEE variable in this script.
      echo **************
      echo.
      goto :exit
   )
) else (
   set MTEE=%MTEE_FOUND%
)

:: Check editor.
if "%EDITOR%" == "notepad.exe" goto :editor_check_end
if "%EDITOR%" == "notepad" goto :editor_check_end
if not exist "%EDITOR%" (
   echo. 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo ************** 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo Please update EDITOR ["%EDITOR%"] 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo variable to point to an editor you wish to use. Using notepad. 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo ************** 2<&1 | "%MTEE%" /+ %LOG_FILE%
   set EDITOR=notepad
)
:editor_check_end

:: Check Maven.
:: First check if local version is defined and exists.
:: Allows user to specify a different maven than what exists in M2_HOME or PATH.
if exist "%LOCAL_M2_HOME%\bin\mvn.bat" (
   set M2_HOME=%LOCAL_M2_HOME%
   set "PATH=%M2_HOME%\bin;%PATH%"

) else if exist "%M2_HOME%\bin\mvn" (
   :: Next check if M2_HOME set. It is.
   set "PATH=%M2_HOME%\bin;%PATH%"

) else (
   :: LOCAL_M2_HOME and M2_HOME don't work.
   :: OK, no mvn then.
   echo. 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo ************** 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo -- Please download Maven from http://maven.apache.org/ and make 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo --   the mvn command available via one of the following methods 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo --   ^(this script detects maven in this order^): 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo -- 1. Set LOCAL_M2_HOME in this script. 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo -- 2. Set M2_HOME environment variable ^(system or user level^). 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo -- -- 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo -- We must be able to set M2_HOME from one of these. 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo ************** 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo. 2<&1 | "%MTEE%" /+ %LOG_FILE%
   goto :report_results
)

:: Check Java.
:: First check if local version is defined and exists.
:: Allows user to specify a different java than what exists in JAVA_HOME.
if exist "%LOCAL_JAVA_HOME%\bin\java.exe" (
   set JAVA_HOME=%LOCAL_JAVA_HOME%
   set "PATH=%JAVA_HOME%\bin;%PATH%"
) else if exist "%JAVA_HOME%\bin\java.exe" (
   :: Next check if JAVA_HOME set. It is.
   set "PATH=%JAVA_HOME%\bin;%PATH%"
) else (
   :: LOCAL_JAVA_HOME and JAVA_HOME don't work.
   :: One of these MUST be set because maven requires JAVA_HOME to be set.
   :: OK, no java then.
   echo. 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo ************** 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo -- Please download and install Java from 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo --   http://www.oracle.com/technetwork/java/javase/downloads/index.html 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo --   and make the java command available via one of the following methods 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo --   ^(this script detects java in this order^): 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo -- 1. Set LOCAL_JAVA_HOME in this script. 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo -- 2. Set JAVA_HOME environment variable ^(system or user level^). 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo -- -- 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo -- We must be able to set JAVA_HOME from one of these or maven will fail. 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo ************** 2<&1 | "%MTEE%" /+ %LOG_FILE%
   echo. 2<&1 | "%MTEE%" /+ %LOG_FILE%
   goto :report_results
)

echo. 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo ----------- 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo Current Directory [%cd%] 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo This script [%0] 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo M2_HOME [%M2_HOME%] 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo JAVA_HOME [%JAVA_HOME%] 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo EDITOR [%EDITOR%] 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo MTEE [%MTEE%] 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo TMPDIR [%TMPDIR%] 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo PATH [%PATH%] 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo ----------- 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo. 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo Caw caw said the Raven! 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo. 2<&1 | "%MTEE%" /+ %LOG_FILE%


echo =========================================================================== 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo Command: 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo mvn %* 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo =========================================================================== 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo. 2<&1 | "%MTEE%" /+ %LOG_FILE%

mvn %* 2<&1 | "%MTEE%" /+ %LOG_FILE%

:report_results
echo. 2<&1 | "%MTEE%" /+ %LOG_FILE%
echo Output sent to %LOG_FILE% 2<&1 | "%MTEE%" /+ %LOG_FILE%
start "" "%EDITOR%" %LOG_FILE%

:exit
echo.
echo Done.
