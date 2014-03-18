/**
 * *****************************************************************************
 * This file is part of CADI a library of CASUAL.
 * 
 * Copyright (C) 2014 Jeremy R. Loper <jrloper@gmail.com>
 *
 * CADI is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * CADI is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 * 
******************************************************************************
 */
package CADI;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

/**
 * *****************************************************************************
 * Interface a.k.a. CADI(v2) or (CASUALS Automated Driver Installer) is a
 * CASUALcore dependant class which attempts to automate CASUAL process on
 * Windows (XP - Win8) A generic driver is required for USB IO via CASUAL. This
 * driver must temporarily take the place of the default OEM driver of targeted
 * device (which must be currently connected). While many OEMs use WinUSB (or
 * compatible alternative) as a device interface, CASUAL is not able communicate
 * with the target because of proprietary (undocumented) driver service API.
 * However once the generic driver is installed CASUAL using reverse engineered
 * open-source tools such as Heimdall - http://goo.gl/bqeulW is able to interact
 * with the target device directly.
 *
 * This class is heavily dependant upon REGEX and a modified version of Devcon
 * (MS-LPL). CADI uses libusbK, which is a generic WinUSB compatible driver for
 * libusbx communication via Heimdall. Two sets of drivers are used (each
 * containing an x86/x64 variant), one built with WDK 7.1 (allowing for XP
 * support) the other built with WDK 8.0 (for Windows 8 support). All driver
 * components are built & digitally signed by Jeremy Loper.
 *
 * WARNING: Modifications to this class can result in system-wide crash of
 * Windows. (I know, I've seen it :-D ) So plan out all modifications prior, and
 * always ensure a null value is never passed to Devcon.
 *
 * @author Jeremy Loper jrloper@gmail.com
 * @author Adam Outler adamoutler@gmail.com
 * ************************************************************************
 */
public class Interface {

    /**
     * windowsDriverBlanket is a static Array of targeted USB VID (VendorID
     * numbers) in hexadecimal form. IDs are stored as strings because Java
     * doesn't have a native storage class for hexadecimal (base 16) without
     * conversion to decimal (base 10) This Member is populated on Class Object
     * creation.
     */
    public static String[] windowsDriverBlanket;

    /**
     * removeDriverOnCompletion is a primarily user set variable, relating to
     * driver package un-installation. Should driver be removed on script
     * completion? 0 - Unset (will prompt user) 1 - Do not remove driver on
     * completion 2 - Remove driver on script completion This Member is
     * populated on Class Object creation.
     */
    public static int removeDriverOnCompletion;

    public class Install {

        /**
         * WindowsDrivers instantiates the windows driver class.
         *
         * @param promptInit initializes removeDriverOnCompletion member and
         * subsequent prompting action. 0 - Unset (will prompt user) (default) 1
         * - Do not remove driver on completion 2 - Remove driver on script
         * completion
         */
        public Install(int promptInit) {
            Lib.log.appendLog("WindowsDrivers() Initializing");
            Interface.windowsDriverBlanket = new String[]{"04E8", "0B05", "0BB4", "22B8", "054C", "2080", "18D1"};
            if (promptInit == 0) {
                removeDriverOnCompletion = removeDriverOptionPrompt("", "") ? 2 : 1; //set value as 2 if true and 1 if false
            }
        }

        public Install(int promptInit, String message, String title) {
            Lib.log.appendLog("WindowsDrivers() Initializing");
            Interface.windowsDriverBlanket = new String[]{"04E8", "0B05", "0BB4", "22B8", "054C", "2080", "18D1"};
            if (promptInit == 0) {
                removeDriverOnCompletion = removeDriverOptionPrompt("", "") ? 2 : 1; //set value as 2 if true and 1 if false
            }
        }

        private boolean removeDriverOptionPrompt(String message, String title) {
            if (message.equals("") || title.equals("")) {
                if (JOptionPane.showConfirmDialog(null, "A generic USB driver must now be installed.\n\n"
                        + "This is required allow direct communications with your device.\n\n"
                        + "Would you like this driver removed after operations have concluded?", "CADI", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    return true;
                }
            } else {
                if (JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    return true;
                }
            }
            return false;
        }

        public boolean installKnownDevices() {
            String[] deviceList = new Console().getDeviceList(true, true);
            int retVal = 0;
            ArrayList<String> qualifiedDevices = new ArrayList<>();//get list of devices to be instaled
            for (String device : deviceList) {
                addDeviceToInstallationQueueIfInList(qualifiedDevices, device);
            }
            ArrayList<String[]> uidVid = new ArrayList<>();//get vidUID list
            parseUidVidFromQualifiedDevices(qualifiedDevices, uidVid);
            Console driver = new Console();
            for (String[] uv : uidVid) {
                String usbVidString = "USB\\VID_" + uv[0] + "&PID_" + uv[1];
                if (driver.update(usbVidString).contains(" successfully")) {//install each driver
                    retVal++;
                }
            }
            return retVal > 0;
        }

        public ArrayList<String[]> parseUidVidFromQualifiedDevices(ArrayList<String> qualifiedDevices, ArrayList<String[]> uidVid) {
            for (String device : qualifiedDevices) {

                if (!device.startsWith("USB\\VID_")) {
                    continue;
                }
                device = device.replace("USB\\VID_", "");
                String vid = device.substring(0, 4);

                if (!device.startsWith(vid + "&PID_")) {
                    continue;
                }
                device = device.replace(vid + "&PID_", "");
                String uid = device.substring(0, 4);
                uidVid.add(new String[]{vid, uid});
            }
            return uidVid;
        }

        public ArrayList<String> addDeviceToInstallationQueueIfInList(ArrayList<String> installqueue, String device) {
            for (String vid : windowsDriverBlanket) {
                if (device.startsWith("USB\\VID_" + vid)) {
                    installqueue.add(device);
                }
            }
            return installqueue;
        }
    }

    class Remove {

        /**
         * windowsDriverBlanket is a static Array of targeted USB VID (VendorID
         * numbers) in hexadecimal form. IDs are stored as strings because Java
         * doesn't have a native storage class for hexadecimal (base 16) without
         * conversion to decimal (base 10) This Member is populated on Class
         * Object creation.
         */
        private final String[] windowsDriverBlanket;

        public Remove() {
            this.windowsDriverBlanket = new String[]{"04E8", "0B05", "0BB4", "22B8", "054C", "2080", "18D1"};
            Lib.log.appendLog("uninstallCADI() Initializing");
            Lib.log.appendLog("uninstallCADI() Scanning for CADI driver package(s)");

        }

        public boolean removeDriver() {
            deleteOemInf();
            Lib.log.appendLog("uninstallCADI() Scanning for orphaned devices");
            boolean driverRemoved = true;
            for (String vid : windowsDriverBlanket) {
                driverRemoved = removeOrphanedDevices(vid);
            }

            Lib.log.appendLog("removeDriver() Windows will now scan for hardware changes");
            if (!new Console().rescan()) {
                Lib.log.appendLog("removeDriver() rescan() failed!");
            }
            return driverRemoved;
        }

        /**
         * deleteOemInf parses output from devconCommand via regex to extract
         * the name of the *.inf file from Windows driver store. Extraction of
         * the file name is determined by setup classes & provider names.
         *
         * @return a String Array of *.inf files matching the search criteria.
         */
        public boolean deleteOemInf() {
            Console driver = new Console();
            Lib.log.appendLog("deleteOemInf() Enumerating installed driver packages");
            int resultSum = 0;
            Pattern pattern = driver.regexPattern(Console.PatternChoice.INF);
            String outputBuffer = driver.enumerate();
            if (outputBuffer == null) {
                Lib.log.appendLog("deleteOemInf() installer returned null!");
                return false;
            }
            Matcher matcher = pattern.matcher(outputBuffer);
            while (matcher.find()) {
                Lib.log.appendLog("removeDriver() Forcing removal of driver package" + matcher.group(0));
                String result = driver.delete(matcher.group(0));
                if (result == null || result.contains("Driver package")) {
                    Lib.log.appendLog("removeDriver() installer returned null!");
                }
                resultSum++;
            }
            return resultSum > 0;
        }

        /**
         * removeOrphanedDevices parses installer output of any current or
         * previously installed USB device drivers for the specified VID. Any
         * matching device drivers are uninstalled
         *
         * @param VID a String containing a four character USB vendor ID code in
         * hexadecimal
         * @return a String Array of installer output from attempted uninstalls
         * of drivers
         */
        public boolean removeOrphanedDevices(String VID) {
            int i = 0;
            int resultSum = 0;
            String result;
            Console driver = new Console();
            if (VID.equals("")) {
                Lib.log.appendLog("removeOrphanedDevices() no VID specified");
                return false;
            }
            Pattern pattern;
            pattern = driver.regexPattern(Console.PatternChoice.MATCHINGDEVICES);
            if (pattern == null) {
                Lib.log.appendLog("removeOrphanedDevices() getRegExPattern() returned null!");
                return false;
            }
            String outputBuffer = driver.findall("*USB\\VID_" + VID + "*");
            if (outputBuffer == null) {
                Lib.log.appendLog("removeOrphanedDevices() installer returned null!");
                return false;
            }
            pattern = driver.regexPattern(Console.PatternChoice.ORPHANS);
            if (pattern == null) {
                Lib.log.appendLog("removeOrphanedDevices() getRegExPattern() returned null!");
                return false;
            }
            Matcher matcher = pattern.matcher(outputBuffer);
            while (matcher.find()) {
                Lib.log.appendLog("removeOrphanedDevices() Removing orphaned device " + "\"@" + DataType.Strings.removeLeadingAndTrailingSpaces(matcher.group(0).replace("\"", "")) + "\"");
                result = driver.remove("\"@" + DataType.Strings.removeLeadingAndTrailingSpaces(matcher.group(0).replace("\"", "")) + "\"");
                if (result.equals("")) {
                } else if (result.contains("device(s) are ready to be removed. To remove the devices, reboot the system.")) {
                    resultSum++;
                } else {
                    Lib.log.appendLog("removeOrphanedDevices() installer returned null!");
                }
                i++;
            }
            return resultSum > 0;
        }
    }
}
