package org.rmb.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Analyse build times from my logs.
 *
 * @author robbram
 */
public final class MavenBuildTimes {

   /** Output debug messages? */
   private static final boolean DEBUG = true;

   /** String to look for at the start of a line that outputs directory. */
   private static final String FRAGMENT_DIRECTORY1 = "Directory: ";

   /** String to look for at the start of a line that outputs directory. */
   private static final String FRAGMENT_DIRECTORY2 = "Working Directory: ";

   /** String to look for at the start of a line that outputs directory. */
   private static final String FRAGMENT_DIRECTORY3 = "Current Directory [";

   /** String to look for at start of line that has time taken. */
   private static final String FRAGMENT_INFO_TOTAL_TIME = "[INFO] Total time: ";

   /** String fragment that means we are dealing in minutes. */
   private static final String FRAGMENT_TIME_MIN = " min";

   /** String fragment that means we are dealing in seconds. */
   private static final String FRAGMENT_TIME_SECONDS = " s";

   /** String to look for at end of line that has time taken. */
   private static final String FRAGMENT_WALL_CLOCK = " (Wall Clock)";

   /** Where the build logs are. */
   public static final String LOGS_DIR =
         "D:/Dropbox/Toll/Notes/logs";

   /**
    * @param args
    *           not used
    */
   public static void main(final String[] args) {
      MavenBuildTimes buildTimes = new MavenBuildTimes();
      try {
         System.out.println("--- START ----");
         buildTimes.analyseBuildTimes();
         System.out.println("--- END ----");
      } catch (IOException e) {
         System.err.println("Failed to analyse build times.");
         e.printStackTrace();
      }
   }

   /**
    * Analyse the build times.
    *
    * @throws IOException
    *            if there is any problem listing files or reading from files.
    */
   public void analyseBuildTimes() throws IOException {
      File logDir = new File(LOGS_DIR);
      int countFiles = 0;
      int countBadFiles = 0;
      String[] fileList = logDir.list(new FilenameFilter() {
         @Override
         public boolean accept(final File dir, final String name) {
            return name.toLowerCase().endsWith(".txt");
         }
      });
      Map<String, MavenBuildTimes.Statistic> times =
            new HashMap<String, MavenBuildTimes.Statistic>();

      System.out.println("In log dir [" + logDir.getAbsolutePath() + "] we found [" + fileList + "] files.");

      for (int index = 0; index < fileList.length; index++) {
         boolean analysisResult =
               analyseLog(new File(LOGS_DIR + "/" + fileList[index]), times);
         countFiles++;
         if (!analysisResult) {
            countBadFiles++;
            if (DEBUG) {
               System.err.println("   Bad file.");
            }

         }
      }
      outputResults(times);
      System.out.println("Finished analysis with [" + countFiles
            + "] total files and [" + countBadFiles
            + "] files we couldn't read.");
   }

   /**
    * Read through a log file to find the stats we are after.
    *
    * @param log
    *           file writen by a maven run
    * @param times
    *           map of command line to
    * @return true if we found a command and time and updated <code>times</code>
    *         . False if we didn't - probably because we encountered a file with
    *         a time but no command.
    * @throws IOException
    *            if there is any problem reading from a file
    */
   private boolean analyseLog(final File log,
         final Map<String, MavenBuildTimes.Statistic> times) //
         throws IOException {
      if (DEBUG) {
         System.out.println("Reading log [" + log.getAbsolutePath() + "] ");
      }
      BufferedReader reader = new BufferedReader(new FileReader(log));
      String previous = null;
      String directory = null;
      Statistic command = null;
      Statistic tempCmd = null;
      String line = null;
      String tempSt = null;
      boolean readTime = false;
      while ((line = reader.readLine()) != null) {
         tempSt = lookForDirectory(line);
         if (tempSt != null) {
            directory = tempSt;
         }
         tempCmd = lookForCommand(times, line, previous);
         if (tempCmd != null) {
            command = tempCmd;
         }
         /*-
          * Look for time:
          *    [INFO] Total time: 02:30 min (Wall Clock)
          */
         if (line != null && line.startsWith(FRAGMENT_INFO_TOTAL_TIME)) {
            // Bad if we found time without a command.
            if (command == null) {
               if (DEBUG) {
                  System.err.println("   Found time [" + line
                        + "] without command.");
               }
               reader.close();
               return false;
            }
            if (directory == null) {
               if (DEBUG) {
                  System.err.println("   No directory found.");
               }
               reader.close();
               return false;
            }
            double seconds = secondsFromLogLine(line);
            times.put(command.getCommand(), command);
            command.addTime(seconds, directory);
            readTime = true;
         }
         previous = line;
      }
      reader.close();
      return readTime;
   }

   /**
    * Look for directory in current line.
    *
    * @param line
    *           current line in the log
    * @return directory, if found; null otherwise
    */
   private String lookForDirectory(final String line) {
      String directory = null;
      if (line == null || line.length() == 0) {
         return null;
      }
      /*-
       * Look for directory:
       *    Directory: ...
       */
      if (line.startsWith(FRAGMENT_DIRECTORY1)) {
         directory = line.substring(FRAGMENT_DIRECTORY1.length());
      } else if (line.startsWith(FRAGMENT_DIRECTORY2)) {
         directory = line.substring(FRAGMENT_DIRECTORY2.length());
      } else if (line.startsWith(FRAGMENT_DIRECTORY3)) {
         directory =
               line.substring(FRAGMENT_DIRECTORY2.length(), line.length() - 1);
      }

      if (DEBUG && directory != null) {
         System.out.println("   Found directory [" + directory + "] ");
      }
      return directory;
   }

   /**
    * Look to see if current line is a command line and add it to times as
    * required.
    *
    * @param times
    *           map of command line to
    * @param line
    *           current line in log file
    * @param previous
    *           line in log file
    * @return Statistic if the line was a command line
    */
   private Statistic lookForCommand(
         final Map<String, MavenBuildTimes.Statistic> times, final String line,
         final String previous) {
      if (line == null || line.length() == 0) {
         return null;
      }
      Statistic command = null;
      /*-
       * Look for command:
       *    Command:
       *    mvn - ...
       */
      if ("Command:".equals(previous) && line != null
            && line.startsWith("mvn ")) {
         if (DEBUG) {
            System.out.println("   Found command [" + line + "] ");
         }
         command = times.get(line);
         if (command == null) {
            command = new Statistic(line);
         }
      }
      return command;
   }

   /**
    * Output results to a CSV file.
    *
    * @param times
    *           statistics around each command.
    * @throws IOException
    *            if we cannot write out report.
    */
   private void
         outputResults(final Map<String, MavenBuildTimes.Statistic> times)
               throws IOException {
      Collection<Statistic> commands = times.values();
      File output = new File("mavenReport.csv");
      PrintWriter writer = new PrintWriter(output, "UTF-8");
      writer.println("Average Time in Seconds,Number of Runs,Command,Directories");
      for (Statistic command : commands) {
         writer.println(command.getAverageTime() + "," //
               + command.getCount() //
               + ",\"" + command.getCommand() + "\"" //
               + ",\"" + command.getDirectoryList() + "\"" //
         );
         if (DEBUG) {
            System.out.println("Command [" + command.getCommand()
                  + "] took an average of [" + command.getAverageTime()
                  + "] seconds over [" + command.getCount() + "] runs.");
         }
      }
      writer.close();
      System.out.println("Output report [" + output.getAbsolutePath() + "].");

   }

   /**
    * Get number of seconds from the log line that contains time the build took.
    * Throws IllegalArgumentException if the time indicator is not one we know
    * about.
    *
    * @param line
    *           from the log like
    *           <code>[INFO] Total time: 02:30 min (Wall Clock)</code>
    * @return number of seconds
    */
   private double secondsFromLogLine(final String line) {
      double seconds = 0;
      String fragment = null;
      if (line.endsWith(FRAGMENT_WALL_CLOCK)) {
         fragment = line.substring(FRAGMENT_INFO_TOTAL_TIME.length(), //
               line.length() - FRAGMENT_WALL_CLOCK.length());
      } else {
         fragment = line.substring(FRAGMENT_INFO_TOTAL_TIME.length());
      }
      if (DEBUG) {
         System.out.print("   Got time fragment [" + fragment + "] ");
      }
      /*-
       * Will get one of:
       *    42.102 s
       *    12:57 min
       */
      StringTokenizer tokens = new StringTokenizer(fragment, " :");
      if (fragment.endsWith(FRAGMENT_TIME_SECONDS)) {
         final double secondsDbl = Double.parseDouble(tokens.nextToken());
         if (DEBUG) {
            System.out.println("with seconds [" + secondsDbl + "].");
         }
         seconds = secondsDbl;
      } else if (fragment.endsWith(FRAGMENT_TIME_MIN)) {
         final int minutesInt = Integer.parseInt(tokens.nextToken());
         final double secondsDbl = Double.parseDouble(tokens.nextToken());
         if (DEBUG) {
            System.out.println("with seconds [" + secondsDbl
                  + "] and minutes [" + minutesInt + "].");
         }
         seconds = minutesInt * 60 + secondsDbl;
      } else {
         throw new IllegalArgumentException("Unknown time indicator from ["
               + line + "].");
      }
      return seconds;
   }

   /**
    * Calculates average time and number of times.
    *
    * @author robbram
    */
   private static final class Statistic {

      /** Command we are calculating time for. */
      private final String command;

      /** Number of times the command has been run. */
      private int count = 0;

      /** Directories that command was run from. */
      private final List<String> directoryList;

      /** Total time for all builds of the same command. */
      private double totalTime = 0;

      /**
       * @param theCommand
       *           command we are collecting statistics for
       */
      public Statistic(final String theCommand) {
         command = theCommand;
         directoryList = new ArrayList<String>();
      }

      /**
       * @param newTime
       *           new time taken for given command in <strong>seconds</strong>
       * @param directory
       *           that the command was run from
       */
      public void addTime(final double newTime, final String directory) {
         totalTime += newTime;
         count++;
         if (!directoryList.contains(directory)) {
            directoryList.add(directory);
         }
      }

      /**
       * @return calculated average from total and count
       */
      public double getAverageTime() {
         return totalTime / count;
      }

      /** @return Command we are calculating time for. */
      public String getCommand() {
         return command;
      }

      /**
       * @return count of times the command was run
       */
      public int getCount() {
         return count;
      }

      /**
       * @return directory the command was run from
       */
      public String getDirectoryList() {
         StringBuilder builder = new StringBuilder();
         for (String dir : directoryList) {
            builder.append(dir);
            builder.append(", ");
         }
         return builder.toString();
      }

   }
}
