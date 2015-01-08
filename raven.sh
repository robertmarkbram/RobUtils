#!/bin/bash

# ------------------------------------------------------------------------------
# -- What is Raven?
# A Bash wrapper around mvn to save output to file and open it in an editor
# By Robert Mark Bram
# https://github.com/robertmarkbram/MavenUtilities
# http://robertmarkbramprogrammer.blogspot.com.au/2015/01/do-you-use-mvn-on-bash-prompt.html
# ------------------------------------------------------------------------------
# Set-up Dependencies first.
# - This script
#    - Save this script as raven.sh in some folder you like to use for utilities etc.
#    - Add the path (folder/directory name) where this script lives to your PATH (*see note 1*).
#    - Otherwise you will have to use the absolute path to this script: e.g. "/C/myApps/Batch/raven" instead of just "raven".
# - Maven
#    - Download it from: http://maven.apache.org/
#    - Set M2_HOME variable (*see note 1*) or LOCAL_M2_HOME in this script.
# - Java
#    - Download and install it from: http://www.oracle.com/technetwork/java/javase/downloads/index.html
#    - Set JAVA_HOME variable (*see note 1*) or LOCAL_JAVA_HOME in this script.
# - tee (to send output to file and console)
#    - Should already be part of Cygwin/nix install.
# - Your favourite text editor.
#    - This script defaults to notepad. If you like it, do nothing.
#    - If you want to use something else, set EDITOR in this script
#    - Because we are *nix, you can set it to vim or less as well.
# - Temp dir
#    - This script defaults to ${TEMP}/maven.
#    - If you want to use something else, set TMPDIR in this script
#
# How to use this script.
# - Instead of using "mvn", just use "raven", e.g.
#       raven clean deploy
#       /C/myApps/Batch/raven clean deploy
#
# Note 1 - changing PATH or other environment variables on Win7 and above: two options.
# - OPTION 1: Start button > Search for "Environment Variables for your account" > modify PATH (or other variable) in top section, "user variables for USERNAME"
#    - No re-boot required, just restart the DOS prompt.
#    - PATH is set only for your user. Other logged users will not see it.
# - OPTION 2: Start button > Search for "Edit the System Environment Variables" > Environment Variables > modify PATH (or other variable) in bottom section, "System Variables"
#    - Re-boot required.
#    - PATH is set for all logged in users.


# ------------------------------------------------------------------------------
# -- Journal of Changes
# ------------------------------------------------------------------------------
# Thursday 08 January 2015, 04:57:36 PM
# - Adapted so that shell version works the same way as batch version.
# - Modified the way this script looks for maven and java such that it always uses local versions first.
# Thursday 08 January 2015, 06:55:09 PM
# - Fix to reporting of commands. Use $* instead of $@ for reporting.

# ------------------------------------------------------------------------------
# -- Variables for this script.
#     Edit these variables - dependencies of this script.
# ------------------------------------------------------------------------------
# Could be vim or less too.
EDITOR=/C/Program\ Files\ \(x86\)/IDM\ Computer\ Solutions/UltraEdit/Uedit32.exe
# EDITOR=notepad
LOCAL_JAVA_HOME=/C/Program\ Files/Java/jdk1.7.0_67
LOCAL_M2_HOME=/C/apps/apache-maven-3.2.3
# TMPDIR=${TEMP}/maven
TMPDIR=${DirMavenLogs}
MAVEN_OPTS="-Xms512m -Xmx1024m -XX:MaxPermSize=256m"
# ############################################################################
# DO NOT EDIT BELOW HERE
# ############################################################################

# ------------------------------------------------------------------------------
# -- Common functions for this script.
# ------------------------------------------------------------------------------

# ===  FUNCTION  ===============================================================
#   DESCRIPTION:  Output message if verbose is on
#    PARAMETERS:  1 - message to be printed
#                 2 - options
#                     - "off" - don't use -e in echo
#       RETURNS:  -
# ==============================================================================
function message() {
   if [ "$#" -eq 2 -a "$2" == "off" ] ; then
      echo "$1" 2>&1 | tee -a "${outputFile}"
   else
      echo -e "$1" 2>&1 | tee -a "${outputFile}"
   fi
}

# ===  FUNCTION  ===============================================================
#   DESCRIPTION:  Check that all dependencies exist.
#    PARAMETERS:  -
#       RETURNS:  -
# ==============================================================================
function checkDependencies() {
   # Temp dir for logs.
   if [ ! -e "$TMPDIR" ] ; then
      mkdir "$TMPDIR"
   fi

   # Check for tee.
   type tee >/dev/null 2>&1 || {
      echo -e "\n**************"
      echo Please update your *nix to install the \"tee\" command.
      echo -e "**************\n"
   }

   # Check editor.
   if [ "${EDITOR}" != "notepad" -a "${EDITOR}" != "notepad.exe" -a "${EDITOR}" != "less" -a "${EDITOR}" != "vim" ] ; then
      if [ ! -e "${EDITOR}" ] ; then
         message "\n**************"
         message "Please update EDITOR [${EDITOR}]"
         message "variable to point to an editor you wish to use. Using notepad."
         message "**************\n"
         set EDITOR=notepad
      fi
   fi

   # Check Maven.
   # First check if local version is defined and exists.
   # Allows user to specify a different maven than what exists in M2_HOME or PATH.
   if [ -e "${LOCAL_M2_HOME}/bin/mvn" ] ; then
      # All good, use local.
      M2_HOME="${LOCAL_M2_HOME}"
      PATH="${M2_HOME}/bin;${PATH}"

   # Next check if M2_HOME set.
   elif [ -e "${M2_HOME}/bin/mvn" ] ; then
      PATH="${M2_HOME}/bin;${PATH}"

   # LOCAL_M2_HOME and M2_HOME don't work.
   # OK, no mvn then.
   else
      # OK, no mvn then.
      message "\n**************"
      message "-- Please download Maven from http://maven.apache.org/ and make"
      message "--   the mvn command available via one of the following methods"
      message "--   (this script detects maven in this order):"
      message "-- 1. Set LOCAL_M2_HOME in this script."
      message "-- 2. Set M2_HOME environment variable (system or user level)."
      message "-- --"
      message "-- We must be able to set M2_HOME from one of these."
      message "**************\n"
      reportResults
      exit 3
   fi

   # Check Java.
   # First check if local version is defined and exists.
   # Allows user to specify a different maven than what exists in JAVA_HOME or PATH.
   if [ -e "${LOCAL_JAVA_HOME}/bin/java" ] ; then
      # All good, use local.
      JAVA_HOME="${LOCAL_JAVA_HOME}"
      PATH="${JAVA_HOME}/bin;${PATH}"

   # Next check if JAVA_HOME set.
   elif [ -e "${JAVA_HOME}/bin/java" ] ; then
      PATH="${JAVA_HOME}/bin;${PATH}"

   # LOCAL_JAVA_HOME and JAVA_HOME don't work.
   # One of these MUST be set because maven requires JAVA_HOME to be set.
   else
      message "\n**************"
      message "-- Please download and install Java from"
      message "--   http://www.oracle.com/technetwork/java/javase/downloads/index.html"
      message "--   and make the java command available via one of the following methods"
      message "--   (this script detects java in this order):"
      message "-- 1. Set LOCAL_JAVA_HOME in this script."
      message "-- 2. Set JAVA_HOME environment variable (system or user level)."
      message "-- --"
      message "-- We must be able to set JAVA_HOME from one of these or maven will fail."
      message "**************\n"
      reportResults
      exit 3
   fi

}


# ===  FUNCTION  ===============================================================
#   DESCRIPTION:  Open log in editor.
#    PARAMETERS:  -
#       RETURNS:  -
# ==============================================================================
function reportResults() {
   case "${EDITOR}" in
      less )
         message "Output sent to $outputFile"
         less -I "${outputFile}" ;;
      vim )
         message "\nOutput sent to $outputFile"
         vim "${outputFile}" ;;
      * )
         # Some windows app.
         outputFileWin=`cygpath -w -a "${outputFile}"`
         message "\n"
         message "Output sent to ${outputFileWin}" off
         unix2dos "${outputFile}"
         "${EDITOR}" "${outputFileWin}" &;;
   esac

}

# ===  FUNCTION  ===============================================================
#   DESCRIPTION:  Output environment details to aid debugging.
#    PARAMETERS:  -
#       RETURNS:  -
# ==============================================================================
function showEnvironmentDetails() {
   message "\n-----------"
   message "Current Directory [`pwd`]"
   message "This script [`pwd -P`/$0]"
   message "M2_HOME [$M2_HOME]"
   message "JAVA_HOME [$JAVA_HOME]"
   message "EDITOR [$EDITOR]"
   message "MTEE [`type tee`]"
   message "TMPDIR [$TMPDIR]"
   message "PATH [$PATH]"
   message "-----------\n"
}






# ------------------------------------------------------------------------------
# -- Script Logic
# ------------------------------------------------------------------------------
# No args means just open default notes file.


# Create timestamp.
timestamp=$(date +"%Y%m%d_%H%M%S")
outputFile=$TMPDIR/maven_$timestamp.txt


checkDependencies

showEnvironmentDetails

message "Caw caw said the Raven!\n"


message "==========================================================================="
message "Command:"
message "mvn $*"
message "===========================================================================\n"

mvn "$@" 2>&1 | tee -a "${outputFile}"

reportResults


