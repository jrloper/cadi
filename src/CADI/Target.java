/*  Target provides a way to determine which platform is being used and various other tools. 
 *  Copyright (C) 2013  Adam Outler
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package CADI;

import CADI.Lib;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;

/**
 * Provides a set of tools designed to identify the operating system and
 * architecture.
 *
 * @author Adam Outler adamoutler@gmail.com
 */
public class Target {

    public static class HostSystem {

        /**
         * Checks if system is Windows.
         *
         * @return true if Linux
         * @author Adam Outler adamoutler@gmail.com
         */
        public static boolean isCompatible() {
            String os = System.getProperty("os.name").toLowerCase();
            return os.indexOf("win") >= 0 && !(os.contains("xp") && os.contains("2000"));
        }

        /**
         * Determines System system architecture based on .
         *
         * @return true if 64 bit.
         * @author Adam Outler adamoutler@gmail.com
         */
        public static boolean is64bitSystem() {
            return System.getenv("ProgramFiles(x86)") != null;
        }

        public static class Shell {

            public class TimeoutLogger {

                TimeoutLogger(boolean realTime, Process p, final String[] restartTimerKeywords, int timeOut) {
                    this.realTime = realTime;
                    processRunning = p;
                    this.timeOut = timeOut;
                    this.restartTimerKeywords = restartTimerKeywords;
                }
                String[] restartTimerKeywords;
                boolean realTime;
                int timeOut;
                private final StringBuilder log = new StringBuilder();
                AtomicBoolean timedOut = new AtomicBoolean(false);
                AtomicBoolean isRunning = new AtomicBoolean(true);
                final Object isRunningLock = new Object();
                AtomicBoolean isLogging = new AtomicBoolean(true);
                final Object isLoggingLock = new Object();
                final Process processRunning;

                final Timer watchDogTimer = new Timer(timeOut, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        log.append("Watchdog Triggered!  Command timed out.");
                        timedOut.set(true);
                        synchronized (processRunning) {
                            processRunning.notifyAll();
                        }
                    }
                });

                synchronized void log(char c) {
                    log.append(c);
                    if (realTime) {
                        log.append(java.lang.Character.toString(c));
                        String logstring = log.toString();
                        for (String check : restartTimerKeywords) {
                            if (logstring.endsWith(check) && isRunning.get()) {
                                log.append("Timer Reset on keyword ").append(check);
                                watchDogTimer.restart();
                            }
                        }
                    }
                }

                synchronized String get() {
                    return log.toString();
                }
            }

            /**
             * timerimeoutimerShellCommand is a multi-threaded method and
             * reports to the TimeOutimerStimerring class. The value contained
             * within the TimeOutimerStimerring class is reported after the
             * timeout elapses if the task locks up.
             *
             * @param cmd to be executed
             * @param timeout in milliseconds
             * @return any text from the command
             */
            public String timeoutShellCommand(final String[] cmd, int timeout) {
                //final object for runnable to write out to.
                class TimeoutString {

                    public String AllText = "";
                }
                final TimeoutString tos = new TimeoutString();

                //Runnable executes in the background
                Runnable runCommand = new Runnable() {
                    @Override
                    public void run() {
                        Lib.log.appendLog("###executing timeout command: " + cmd[0] + "###");
                        try {
                            String line;
                            ProcessBuilder p = new ProcessBuilder(cmd);
                            p.redirectErrorStream(true);
                            Process process = p.start();
                            BufferedReader STDOUT = new BufferedReader(new InputStreamReader(process.getInputStream()));

                            while ((line = STDOUT.readLine()) != null) {
                                tos.AllText = tos.AllText + line + "\n";
                            }
                            //Log.level0(cmd[0]+"\":"+AllText);
                        } catch (IOException ex) {
                            Lib.log.appendLog("@problemWhileExecutingCommand " + DataType.Strings.arrayToString(cmd) + " " + tos.AllText);
                        }
                    }
                };
                //t executes the runnable on a different thread
                Thread t = new Thread(runCommand);
                t.setDaemon(true);
                t.setName("TimeOutShell " + cmd[0] + timeout + "ms abandon time");
                t.start();

                //set up timeout with calendar time in millis
                Calendar endTime = Calendar.getInstance();
                endTime.add(Calendar.MILLISECOND, timeout);
                //loop while not timeout and halt if thread dies. 
                while (Calendar.getInstance().getTimeInMillis() < endTime.getTimeInMillis()) {
                    if (!t.isAlive()) {
                        break;
                    }
                }
                if (Calendar.getInstance().getTimeInMillis() >= endTime.getTimeInMillis()) {
                    Lib.log.appendLog("TimeOut on " + cmd[0] + " after " + timeout + "ms. Returning what was received.");
                    return "Timeout!!! " + tos.AllText;
                }
                //return values logged from TimeoutKeywordReader class above
                return tos.AllText;

            }

            /*
             * Complex, but bulletproof method of running a shell command.  launches a
             * process, and waits for it to complete.   Launches a watchdog timer which
             * will cause the process to stop waiting after a defined period of time.
             * Monitors for keywords which trigger the timer to be reset.  This allows
             * running of commands which have a high probability of timing out, or may
             * take a while.
             * @param cmd array of commands. eg. "new string[]{command, param, param}"
             * @param timeout process timeout in ms. The process will be abandoned after this time.
             * @param restartTimerKeywords keywords which reset the timer.
             * @param logLevel2 Set to true if user viewable logging is preferable.
             * @return Text received from command.
             * @author Adam Outler adamoutler@gmail.com
             */
            /**
             *
             * @param cmd
             * @param restartTimerKeywords
             * @param timeout
             * @param logLevel2
             * @param shell
             * @return
             */
            public String timeoutShellCommandWithWatchdog(final String[] cmd, final String[] restartTimerKeywords, final int timeout, final boolean logLevel2, Target shell) {
                StringBuilder sb = new StringBuilder();
                try {
                    ProcessBuilder p = new ProcessBuilder(cmd);
                    p = p.redirectErrorStream(true);
                    final Process process = p.start();
                    final TimeoutLogger tl = new TimeoutLogger(logLevel2, process, restartTimerKeywords, timeout);
                    Thread processMonitor = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                tl.processRunning.waitFor();
                                tl.watchDogTimer.stop();
                                tl.isRunning.set(false);
                                Lib.log.appendLog("Process Monitor done.");
                                synchronized (tl.processRunning) {
                                    tl.processRunning.notifyAll();
                                }
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Target.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                    processMonitor.setName("Monitoring Process Exit Status from " + cmd[0]);
                    Thread reader = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BufferedInputStream STDOUT = new BufferedInputStream(tl.processRunning.getInputStream());
                            Lib.log.appendLog("Instantiating reader process");
                            try {
                                while (tl.isRunning.get() && !tl.timedOut.get()) {
                                    if (STDOUT.available() > 0) {
                                        char read = (char) STDOUT.read();
                                        tl.log(read);
                                    }
                                }
                                tl.watchDogTimer.stop();
                                Thread.sleep(100);
                                while (STDOUT.available() > 0) {
                                    char read = (char) STDOUT.read();
                                    tl.log(read);
                                }
                                tl.isLogging.set(false);
                                synchronized (tl.processRunning) {
                                    tl.processRunning.notifyAll();
                                }
                            } catch (IOException | InterruptedException ex) {
                                Logger.getLogger(Target.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                    reader.setName("Reading and monitoring output from " + cmd[0]);
                    reader.start();
                    tl.watchDogTimer.start();
                    processMonitor.start();
                    synchronized (tl.processRunning) {
                        tl.processRunning.wait();
                    }
                    if (tl.isLogging.get()) {
                        synchronized (tl.processRunning) {
                            tl.processRunning.wait();
                        }
                    }
                    String retvalue = tl.get();
                    if (tl.timedOut.get()) {
                        retvalue = "Timeout!!! " + retvalue;
                        process.destroy();
                    }
                    return retvalue;
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(Target.class.getName()).log(Level.SEVERE, null, ex);
                }
                return "";
            }

            /**
             * Sends a shell command in a basic way, logs results
             *
             * @param cmd command and params to execute
             * @return result from shell
             * @author Adam Outler adamoutler@gmail.com
             */
            public String sendShellCommand(String[] cmd) {
                Lib.log.appendLog("###executing: " + cmd[0] + "###");
                String AllText = "";
                try {
                    String line;
                    Process process = new ProcessBuilder(cmd).start();
                    BufferedReader STDOUT = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader STDERR = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    try {
                        process.waitFor();
                    } catch (InterruptedException ex) {
                        Lib.log.appendLog(ex.getLocalizedMessage());
                    }
                    int y = 0;
                    while ((line = STDOUT.readLine()) != null) {
                        if (y == 0) {
                            AllText = AllText + "\n" + line + "\n";
                        } else {
                            AllText = AllText + line + "\n";
                        }
                        y++;
                    }
                    y = 0;
                    while ((line = STDERR.readLine()) != null && !line.equals("")) {
                        if (y == 0) {
                            AllText = AllText + "\n" + line + "\n";
                        } else {
                            AllText = AllText + line + "\n";
                        }
                        y++;
                    }
                    return AllText + "\n";
                } catch (IOException ex) {
                    Lib.log.appendLog("@problemWhileExecutingCommand " + DataType.Strings.arrayToString(cmd) + "\nreturnval:" + AllText);
                    return "CritERROR!!!";
                }
            }

            /**
             * Live shell command executes a command and outputs information in
             * real-time to console
             *
             * @param params command and arguments to execute
             * @param display true if output should be logged to log device
             * @return output from command
             * @author Adam Outler adamoutler@gmail.com
             */
            public String liveShellCommand(String[] params, boolean display) {
                String LogRead = "";
                try {
                    ProcessBuilder p = new ProcessBuilder(params);
                    p.redirectErrorStream(true);
                    Process process = p.start();
                    Lib.log.appendLog("###executing real-time command: " + params[0] + "###");
                    BufferedReader STDOUT = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String LineRead = "";
                    String CharRead;
                    int c;
                    while ((c = STDOUT.read()) > -1) {
                        CharRead = Character.toString((char) c);
                        LineRead = LineRead + CharRead;
                        LogRead = LogRead + CharRead;
                        if (display) {
                            Lib.log.appendLog(CharRead);
                        }
                    }
                } catch (RuntimeException ex) {
                    Lib.log.appendLog(ex.getLocalizedMessage());
                    return LogRead;
                } catch (IOException ex) {
                    Lib.log.appendLog(ex.getLocalizedMessage());
                }
                return LogRead;
            }

            /**
             * Sends a shell command but does not log output to logging device
             *
             * @param cmd command and parameters to be executed.
             * @return output from shell command.
             * @author Adam Outler adamoutler@gmail.com
             */
            public String silentShellCommand(String[] cmd) {
                String AllText = "";
                try {
                    String line;
                    Process process = new ProcessBuilder(cmd).start();
                    BufferedReader STDOUT = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    try {
                        process.waitFor();
                    } catch (InterruptedException ex) {
                        Lib.log.appendLog(ex.getLocalizedMessage());
                    }
                    while ((line = STDOUT.readLine()) != null) {
                        AllText = AllText + "\n" + line;
                    }
                    return AllText;
                } catch (IOException ex) {
                    return "CritERROR!!!";
                }
            }
        }
    }

    public static class FileSystem {

        private File getNewTempFolder() {

            if (Lib.TempFolder == null) {
                File Temp = null;
                String user = System.getProperty("user.name");  //username
                String tf = System.getProperty("java.io.tmpdir"); //tempfolder
                tf = tf.endsWith(Lib.slash) ? tf : tf + Lib.slash;  //make sure temp folder has a slash
                SimpleDateFormat sdf = new SimpleDateFormat("-yyyy-MM-dd-HH.mm.ss");
                Temp = new File(tf + "CADI" + user + sdf.format(new Date()).toString() + Lib.slash); //set /temp/usernameRandom/
                if (!Temp.exists() && Temp.isDirectory()) {
                    Temp.mkdirs();
                }
                return Temp;
            }
            return null;
        }

        public File getTempFolder() {
            if (Lib.TempFolder == null) {
                return getNewTempFolder();
            } else {
                return Lib.TempFolder;
            }
        }

        /**
         * recursively deletes a String path
         *
         * @param path
         * @author Adam Outler adamoutler@gmail.com
         */
        public void recursiveDelete(String path) {
            recursiveDelete(new File(path));
        }

        /**
         * recursively deletes a file path
         *
         * @param path
         * @author Adam Outler adamoutler@gmail.com
         */
        public void recursiveDelete(File path) {
            File[] c = path.listFiles();
            if (path.exists()) {
                Lib.log.appendLog("Removing folder and contents:" + path.toString());
                if (c != null && c.length > 0) {
                    for (File file : c) {
                        if (file.isDirectory()) {
                            recursiveDelete(file);
                            file.delete();
                        } else {
                            file.delete();
                        }
                    }
                }
                path.delete();
            }
        }

        /**
         * verify ability to write to every file in a path
         *
         * @param path
         * @return true if permission to write
         * @author Adam Outler adamoutler@gmail.com
         */
        public boolean verifyWritePermissionsRecursive(String path) {
            File Check = new File(path);
            File[] c = Check.listFiles();
            if (Check.exists()) {
                Lib.log.appendLog("Verifying permissions in folder:" + path.toString());
                for (File file : c) {
                    if (!file.canWrite()) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * takes a path and a name returns qualified path to file
         *
         * @param PathToSearch
         * @param FileName
         * @return absolute path to folder
         * @author Adam Outler adamoutler@gmail.com
         */
        public String findRecursive(String PathToSearch, String FileName) {
            File Check = new File(PathToSearch);
            File[] c = Check.listFiles();
            String s = "";
            if (Check.exists()) {
                Lib.log.appendLog("Searching for file in folder:" + PathToSearch.toString());
                for (File file : c) {
                    if (file.isDirectory()) {
                        return findRecursive(file.getAbsolutePath(), FileName);
                    } else if (file.getName().equals(FileName)) {
                        try {
                            return file.getCanonicalPath();
                        } catch (IOException ex) {
                            Lib.log.appendLog(ex.getLocalizedMessage());
                        }
                    }
                }
            }
            return s;
        }

        /**
         * verifies file/folder exists returns a boolean value if the file
         * exists
         *
         * @param file
         * @return true if exists
         * @author Adam Outler adamoutler@gmail.com
         */
        public boolean verifyExists(String file) {
            if (file != null && !file.isEmpty()) {
                File f = new File(file);
                if (!f.exists() && !f.isDirectory() && !f.isFile()) {
                    return false;
                }
            } else {
                return false;
            }
            return true;
        }

        /**
         * makes a folder, works recursively
         *
         * @param Folder
         * @return true if folder was created
         * @author Adam Outler adamoutler@gmail.com
         */
        public boolean makeFolder(String Folder) {
            if (Folder == null) {
                return false;
            }
            File folder = new File(Folder);
            if (folder.exists()) {
                return true;
            } else {
                folder.mkdirs();
                if (folder.exists()) {
                    return true;
                } else {
                    Lib.log.appendLog("@couldNotCreateFolder " + Folder);
                    return false;
                }
            }
        }

        /**
         * writes a stream to a destination file
         *
         * @param stream Stream to be written
         * @param destination output file
         * @throws FileNotFoundException
         * @throws IOException
         * @author Adam Outler adamoutler@gmail.com
         */
        public void writeStreamToFile(BufferedInputStream stream, String destination) throws FileNotFoundException, IOException {
            int currentByte;
            int buffer = 4096;
            byte[] data = new byte[buffer];
            File f = new File(destination);
            if (!verifyExists(f.getParent())) {
                makeFolder(f.getParent());
            }
            FileOutputStream fos = new FileOutputStream(f);
            BufferedOutputStream dest;
            dest = new BufferedOutputStream(fos, buffer);
            while ((currentByte = stream.read(data, 0, buffer)) != -1) {
                dest.write(data, 0, currentByte);
            }
            dest.flush();
            dest.close();
        }

        /**
         * takes a string and a filename, writes to the file
         *
         * @param Text
         * @param File
         * @throws IOException
         * @author Adam Outler adamoutler@gmail.com
         */
        public void writeToFile(String Text, String File) throws IOException {
            BufferedWriter bw;
            try (final FileWriter fw = new FileWriter(File, true)) {
                bw = new BufferedWriter(fw);
                bw.write(Text);
                bw.flush();
            }
        }

        /**
         * takes a string and a filename, overwrites to the file
         *
         * @param Text
         * @param File
         * @throws IOException
         * @author Adam Outler adamoutler@gmail.com
         */
        public void overwriteFile(String Text, String File) throws IOException {
            BufferedWriter bw;
            bw = new BufferedWriter(new FileWriter(File, false));
            bw.write(Text);
            bw.close();
            Lib.log.appendLog("File overwrite Finished");
        }

        private boolean writeInputStreamToFile(InputStream is, File file) {
            Lib.log.appendLog("Attempting to write " + file.getPath());
            try {
                BufferedOutputStream out;
                out = new BufferedOutputStream(new FileOutputStream(file));
                int currentByte;
                // establish buffer for writing file
                int BUFFER = 4096;
                byte[] data = new byte[BUFFER];
                if (is.available() > 0) {
                    // while stream does not return -1, fill data buffer and write.
                    while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, currentByte);
                    }
                } else {
                    return false;
                }
                is.close();
                out.flush();
                out.close();
            } catch (IOException e) {
                return false;
            }
            if (file.exists() && file.length() >= 4) {
                Lib.log.appendLog("File verified.");
                return true;
            } else {
                Lib.log.appendLog("@failedToWriteFile");
                return false;
            }
        }

        /**
         * takes a string filename returns a boolean if the file was deleted
         *
         * @param FileName
         * @return true if file was deleted
         * @author Adam Outler adamoutler@gmail.com
         */
        public Boolean deleteFile(String FileName) {
            Boolean Deleted;
            File file = new File(FileName);
            if (file.exists()) {
                if (file.delete()) {
                    Deleted = true;
                    Lib.log.appendLog("Deleted " + FileName);
                } else {
                    Deleted = false;
                    Lib.log.appendLog("@couldNotDeleteFile" + FileName);
                }
            } else {
                Deleted = true;
            }
            return Deleted;
        }

        /**
         * deletes files
         *
         * @param cleanUp files to be deleted
         * @return true if all files were deleted false and halts on error
         * @author Adam Outler adamoutler@gmail.com
         */
        public boolean deleteStringArrayOfFiles(String[] cleanUp) {
            for (String s : cleanUp) {
                if (s != null) {
                    new File(s).delete();
                } else {
                    continue;
                }
                if (this.verifyExists(s)) {
                    return false;
                }
            } //all files were deleted
            return true;
        }

        /**
         * copies a file from a source to a destination
         *
         * @param sourceFile
         * @param destFile
         * @throws IOException
         * @author Adam Outler adamoutler@gmail.com
         */
        public void copyFile(File sourceFile, File destFile) throws IOException {
            Lib.log.appendLog("Copying " + sourceFile.getCanonicalPath() + " to " + destFile.getCanonicalPath());
            if (!destFile.exists()) {
                destFile.createNewFile();
            }
            FileChannel source = null;
            FileChannel destination = null;
            try {
                source = new FileInputStream(sourceFile).getChannel();
                destination = new FileOutputStream(destFile).getChannel();
                destination.transferFrom(source, 0, source.size());
            } finally {
                if (source != null) {
                    source.close();
                }
            }
            if (destination != null) {
                destination.close();
            }
        }

        /**
         * returns the name of the current folder
         *
         * @return current folder
         * @author Adam Outler adamoutler@gmail.com
         */
        public String currentDir() {
            String CurrentDir = new File(".").getAbsolutePath();
            Lib.log.appendLog("Detected current folder: " + CurrentDir);
            if (CurrentDir.endsWith(".")) {
                CurrentDir = CurrentDir.substring(0, CurrentDir.length() - 1);
            }
            return CurrentDir;
        }

        /**
         * copies a file from a string path to a string path returns a boolean
         * if completed
         *
         * @param FromFile
         * @param ToFile
         * @return true if completed
         * @author Adam Outler adamoutler@gmail.com
         */
        public boolean copyFile(String FromFile, String ToFile) {
            File OriginalFile = new File(FromFile);
            File DestinationFile = new File(ToFile);
            try {
                copyFile(OriginalFile, DestinationFile);
                return true;
            } catch (IOException ex) {
                return false;
            }
        }

        /**
         * takes a filename sets executable returns result
         *
         * @param Executable
         * @return true if executable bit was set
         * @author Adam Outler adamoutler@gmail.com
         */
        public boolean setExecutableBit(String Executable) {
            File Exe = new File(Executable);
            boolean Result = Exe.setExecutable(true);
            Lib.log.appendLog("Setting executable " + Exe + ". Result=" + Result);
            return Result;
        }

        /**
         * takes a string resource name returns result if it exists
         *
         * @param res resource to verify
         * @return true if resource exists
         * @author Adam Outler adamoutler@gmail.com
         */
        public boolean verifyResource(String res) {
            return getClass().getResource(res) != null;
        }

        /**
         * takes a resource name returns a string of file contents
         *
         * @param Resource
         * @return string contents of resource
         * @author Adam Outler adamoutler@gmail.com
         */
        public String readTextFromResource(String Resource) {
            InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(Resource);
            StringBuilder text = new StringBuilder();
            try {
                InputStreamReader in;
                in = new InputStreamReader(resourceAsStream, "UTF-8");
                int read;
                while ((read = in.read()) != -1) {
                    char C = Character.valueOf((char) read);
                    text.append(C);
                }
                in.close();
            } catch (NullPointerException | IOException ex) {
                Lib.log.appendLog("@resourceNotFound:" + Resource);
            }
            //Log.level3(text.toString());
            return text.toString();
        }

        /**
         * reads text from stream
         *
         * @param in stream to read
         * @return text output
         * @author Adam Outler adamoutler@gmail.com
         */
        public String readTextFromStream(BufferedInputStream in) {
            StringBuilder text = new StringBuilder();
            try {
                int read;
                while ((read = in.read()) != -1) {
                    char C = Character.valueOf((char) read);
                    text.append(C);
                }
                in.close();
            } catch (IOException ex) {
                Lib.log.appendLog(ex.getLocalizedMessage());
            }
            //Log.level3(text.toString());
            return text.toString();
        }

        /**
         * reads file contents returns string
         *
         * @param FileOnDisk file to read
         * @return string representation of file
         * @author Adam Outler adamoutler@gmail.com
         */
        public String readFile(String FileOnDisk) {
            String EntireFile = "";
            try {
                String Line;
                try (final BufferedReader br = new BufferedReader(new FileReader(FileOnDisk))) {
                    while ((Line = br.readLine()) != null) {
                        //Log.level3(Line);
                        EntireFile = EntireFile + "\n" + Line;
                    }
                }
            } catch (IOException ex) {
                Lib.log.appendLog("@fileNotFound " + FileOnDisk);
            }
            EntireFile = EntireFile.replaceFirst("\n", "");
            return EntireFile;
        }

        /**
         * lists files in a folder
         *
         * @param folder folder to list
         * @return array of filenames
         * @author Adam Outler adamoutler@gmail.com
         */
        public String[] listFolderFiles(String folder) {
            File dir = new File(folder);
            if (!dir.isDirectory()) {
                Lib.log.appendLog("@fileNotAFolder");
                return null;
            }
            ArrayList<String> files = new ArrayList<>();
            File[] list = dir.listFiles();
            for (int x = 0; list.length > x; x++) {
                files.add(list[x].getName());
            }
            return DataType.Strings.convertArrayListToStringArray(files);
        }

        /**
         * lists files with full qualifiers
         *
         * @param folder folder to list
         * @return array of files
         * @author Adam Outler adamoutler@gmail.com
         */
        public String[] listFolderFilesCannonically(String folder) {
            File dir = new File(folder);
            if (!dir.isDirectory()) {
                Lib.log.appendLog("\"@fileNotAFolder");
                return null;
            }
            String[] childOf = new String[1024];
            File[] list = dir.listFiles();
            for (int x = 0; list.length > x; x++) {
                try {
                    childOf[x] = list[x].getCanonicalFile().toString();
                } catch (IOException ex) {
                    Lib.log.appendLog(ex.getLocalizedMessage());
                }
            }
            return childOf;
        }

        /**
         *
         * @param sourceFile from location
         * @param destFile to location
         * @return true if moved
         * @throws IOException
         * @author Adam Outler adamoutler@gmail.com
         */
        public boolean moveFile(File sourceFile, File destFile) throws IOException {
            FileSystem fO = new FileSystem();
            if (!destFile.getParentFile().exists()) {
                File folder = destFile.getParentFile();
                folder.mkdirs();
            }
            if (destFile.exists()) {
                Lib.log.appendLog("Cannot move file.  Destination file is in the way");
                return false;
            }
            Lib.log.appendLog("moving " + sourceFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
            return sourceFile.renameTo(destFile);
        }

        /**
         * moves a file
         *
         * @param sourceFile from location
         * @param destFile to location
         * @return true if moved
         * @throws IOException
         * @author Adam Outler adamoutler@gmail.com
         */
        public boolean moveFile(String sourceFile, String destFile) throws IOException {
            FileSystem fo = new FileSystem();
            if (!fo.verifyExists(sourceFile)) {
                Lib.log.appendLog("[moveFile()] Source doesn't exist");
                return false;
            }
            if (fo.verifyExists(destFile)) {
                fo.deleteFile(destFile);
            }
            if (fo.copyFile(sourceFile, destFile)) {
                if (fo.deleteFile(sourceFile)) {
                    Lib.log.appendLog("[moveFile()]File moved successfully");
                    return true;
                } else {
                    Lib.log.appendLog("[moveFile()]File copied, unable to remove source");
                    return false;
                }
            } else {
                Lib.log.appendLog("[moveFile()]Unable to copy source to destination");
                return false;
            }
        }
    }

}
