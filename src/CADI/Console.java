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

import CADI.Target.HostSystem;
import CADI.Target.HostSystem.Shell;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Jeremy R. Loper <jrloper@gmail.com>
 */
public class Console {

    public static String[][] Vendor = new String[][]{new String[]{"0x0502", "0x1F3A", "0x1B8E", "0x16D5", "0x0E79", "0x0B05", "0x1D91", "0x1219", "0x413C", "0x03FC", "0x297F", "0x2207", "0x0489", "0x04C5", "0x0F1C", "0x091E", "0x0414", "0x1E85", "0x18D1", "0x201E", "0x19A5", "0x109B", "0x03F0", "0x0BB4", "0x12D1", "0x2314", "0x8087", "0x2420", "0x24E3", "0x2116", "0x2237", "0x0482", "0x1949", "0x17EF", "0x2006", "0x1004", "0x25E3", "0x22B8", "0x0DB0", "0x0E8D", "0x0409", "0x2080", "0x0955", "0x22D9", "0x2257", "0x2836", "0x10A9", "0x1D4D", "0x0471", "0x04DA", "0x1662", "0x1D45", "0x05C6", "0x0408", "0x2207", "0x04E8", "0x04DD", "0x1F53", "0x054C", "0x0FCE", "0x1BBB", "0x1D09", "0x2340", "0x0451", "0x0930", "0xE040", "0x0531", "0x2717", "0x2916", "0x1EBF", "0x19D2"}, new String[]{"Acer", "Allwinner", "Amlogic", "AnyDATA", "Archos", "Asus", "BYD", "Compal", "Dell", "ECS", "Emerging Technologies", "Emerson-Rockchip", "Foxconn", "Fujitsu", "Funai", "Garmin-Asus", "Gigabyte", "Gigaset", "Google", "Haier", "Harris", "Hisense", "Hewlett-Packard", "HTC", "Huawei", "Inq Mobile", "Intel", "iRiver", "K-Touch", "KT-Tech", "Kobo", "Kyocera", "LAB126", "Lenovo", "Lenovo Mobile", "LGE", "Lumigon", "Motorola", "MSI", "MTK", "NEC", "Barnes & Noble", "nVidia", "OPPO", "OTGV", "OUYA", "Pantech", "Pegatron", "Phillips", "PMC", "Positivo", "OISDA", "Qualcomm", "Quanta", "Samsung", "Sharp", "SK Telesys", "Sony", "Sony-Ericsson", "T & A", "Techfaith", "Teleepoch", "Texas Instruments", "Toshiba", "Vizio", "Wacom", "Xiaomi", "Yota", "Yulong", "ZTE"}};

    public String vendorName(String vid) {
        for (int x = 0; x < Vendor[0].length; x++) {
            if (Vendor[x].toString().contains(vid)) {
                return Vendor[0][x].toString();
            }
        }
        return "Unknown";
    }

    public enum PatternChoice {

        ORPHANS, CASUALDRIVER, INF, INSTALL, MATCHINGDEVICES, ALLDEVICES
    }

    /**
     * pathToCADI contains the full path to the root folder of where driver
     * package(s) are (or will be). This Member is populated on Class Object
     * creation.
     */
    private final String pathToCADI;

    /**
     * driverExtracted this static member is toggled true upon a successful
     * driver package decompression.
     *
     */
    private static volatile boolean driverExtracted = false;

    /**
     * CADI Windows Driver for 64bit Windows Vista and higher.
     */
    private final static String usbDriverPackage = "/CADI/driver/WinUSB_01011.zip";

    public Console() {
        this.pathToCADI = Lib.TempFolder.toString() + "CADI" + Lib.slash;
        if (!driverExtracted) {
            try {
                driverExtract(pathToCADI);
            } catch (FileNotFoundException ex) {
                Lib.log.appendLog(ex.getLocalizedMessage());
                return;
            } catch (IOException ex) {
                Lib.log.appendLog(ex.getLocalizedMessage());
                return;
            }
            driverExtracted = true;
        }
    }

    /**
     * driverExtract extracts the contents of CADI.zip from CASUAL's resources
     *
     * @param pathToExtract the desired destination folders full path.
     *
     * @throws FileNotFoundException
     * @throws IOException
     *
     * @return true if successful, false otherwise
     */
    private boolean driverExtract(String pathToExtract) throws FileNotFoundException, IOException {
        if (new CADI.Target.FileSystem().makeFolder(pathToCADI)) {
            Lib.log.appendLog("driverExtract() Unzipping Driver Package for 32bit Windows");
            Unzip.unZipResource(usbDriverPackage, pathToExtract);
            return true;
        }
        return false;
    }

    /**
     * getDeviceList parses installer output for connected USB devices of the
     * specified VID; Any matching devices are stored for return in a String
     * Array.
     *
     * @param VID a String containing a four character USB vendor ID code in
     * hexadecimal
     * @return is a String Array of matching connected devices, null otherwise
     */
    public String[] getDeviceList(String VID) {
        if (VID.equals("")) {
            Lib.log.appendLog("getDeviceList() no VID specified");
            return null;
        }
        String rawDeviceList = find("*USB\\VID_" + VID + "*");

        if (rawDeviceList == null) {
            Lib.log.appendLog("getDeviceList() installer returned null!");
            return null;
        }
        Pattern pattern = regexPattern(PatternChoice.MATCHINGDEVICES);
        if (pattern == null) {
            Lib.log.appendLog("getDeviceList() getRegExPattern() returned null!");
            return null;
        }
        Matcher matcher = pattern.matcher(rawDeviceList);
        pattern = regexPattern(PatternChoice.ALLDEVICES);

        if (pattern == null) {
            Lib.log.appendLog("getDeviceList() getRegExPattern() returned null!");
            return null;
        }
        pattern = regexPattern(PatternChoice.ALLDEVICES);
        matcher = pattern.matcher(rawDeviceList);
        ArrayList<String> al = new ArrayList<>();
        while (matcher.find()) {
            String replacedQuote = DataType.Strings.removeLeadingAndTrailingSpaces(matcher.group(0).replace("\"", ""));
            al.add(replacedQuote);
        }
        String[] retval = al.toArray(new String[al.size()]);
        if (retval.length == 0) {
            retval = null;
        }
        return retval;
    }

    /**
     * getDeviceList parses installer output for devices specified Any matching
     * devices are stored for return in a String Array.
     *
     * @param onlyConnected boolean for presently connected devices only
     * @param onlyUSB boolean for USB devices only
     * @return is a String Array of matching devices, null otherwise
     */
    public String[] getDeviceList(boolean onlyConnected, boolean onlyUSB) {
        String rawDeviceList;
        if (onlyConnected && onlyUSB) {
            rawDeviceList = find("USB*"); //All present USB devices
        } else if (onlyConnected && !onlyUSB) {
            rawDeviceList = find("*"); //All present devices
        } else if (!onlyConnected && onlyUSB) {
            rawDeviceList = findall("USB*"); //All installed USB devices
        } else {
            rawDeviceList = findall("*"); //All installed devices
        }
        if (rawDeviceList == null) {
            Lib.log.appendLog("getDeviceList() installer returned null!");
            return null;
        }
        Pattern pattern = regexPattern(PatternChoice.MATCHINGDEVICES);
        if (pattern == null) {
            Lib.log.appendLog("getDeviceList() getRegExPattern() returned null!");
            return null;
        }
        pattern = regexPattern(PatternChoice.ALLDEVICES);
        Matcher matcher = pattern.matcher(rawDeviceList);
        ArrayList<String> al = new ArrayList<>();
        while (matcher.find()) {
            String replacedQuote = DataType.Strings.removeLeadingAndTrailingSpaces(matcher.group(0).replace("\"", ""));
            al.add(replacedQuote);
        }
        String[] retval = al.toArray(new String[al.size()]);
        if (retval.length == 0) {
            retval = null;
        }
        return retval;
    }

    /**
     * regexPattern returns a Pattern Object of the requested REGEX pattern.
     *
     * @param whatPattern a predefined String name for a REGEX pattern.
     * @return a compiled REGEX Pattern if requested pattern exists, otherwise
     * null.
     */
    public Pattern regexPattern(PatternChoice whatPattern) {
        switch (whatPattern) {
            case ORPHANS:
                return Pattern.compile("USB.?VID_[0-9a-fA-F]{4}&PID_[0-9a-fA-F]{4}.*(?=:\\s[CASUAL's|Samsung]+\\s[Android\\sDevice])");
            case CASUALDRIVER:
                return Pattern.compile("USB.?VID_[0-9a-fA-F]{4}&PID_[0-9a-fA-F]{4}.*(?=:\\s[CASUAL's|Samsung]+\\s[Android\\sDevice])");
            case INF:
                return Pattern.compile("[o|Oe|Em|M]{3}[0-9]{1,4}\\.inf(?=\\s*Provider:\\slibusbK\\s*Class:\\s*libusbK USB Devices)");
            case INSTALL:
                return Pattern.compile("USB.?VID_[0-9a-fA-F]{4}&PID_[0-9a-fA-F]{4}(?=.*:)");
            case MATCHINGDEVICES:
                return Pattern.compile("(?<=\\s)[0-9]{1,3}?(?=[\\smatching\\sdevice\\(s\\)\\sfound])");
            case ALLDEVICES:
                return Pattern.compile("\\S+(?=\\s*:\\s)");
            default:
                Lib.log.appendLog("getRegExPattern() no known pattern requested");
                return null;
        }
    }

    /**
     * getCASUALDriverCount parses installer output for all CASUAL driver
     * installations and returns an integer count
     *
     * @return integer count of CASUAL driver installs
     */
    public int getCASUALDriverCount() {
        int devCount = 0;
        String outputBuffer = findall("USB*");
        if (outputBuffer == null) {
            Lib.log.appendLog("removeOrphanedDevices() installer returned null!");
            return 0;
        }
        Pattern pattern = regexPattern(PatternChoice.CASUALDRIVER);
        if (pattern == null) {
            Lib.log.appendLog("removeOrphanedDevices() getRegExPattern() returned null!");
            return 0;
        }
        Matcher matcher = pattern.matcher(outputBuffer);
        while (matcher.find()) {
            devCount++;
        }
        return devCount;
    }

    public String update(String HWID) {
        if (!HWID.isEmpty()) {
            return sendCommand("update " + pathToCADI + "cadi.inf " + "\"" + HWID);
        } else {
            return null;
        }
    }

    public String remove(String HWID) {
        if (!HWID.isEmpty()) {
            return sendCommand("remove " + HWID);
        } else {
            return null;
        }
    }

    public String delete(String infName) {
        if (!infName.isEmpty()) {
            return sendCommand("-f dp_delete " + infName);
        } else {
            return null;
        }
    }

    public String find(String searchString) {
        if (!searchString.isEmpty()) {
            return sendCommand("find " + searchString);
        } else {
            return null;
        }
    }

    public String findall(String searchString) {
        if (!searchString.isEmpty()) {
            return sendCommand("findall " + searchString);
        } else {
            return null;
        }
    }

    public String enumerate() {
        return sendCommand("dp_enum");
    }

    public boolean rescan() {
        return (sendCommand("rescan").contains("Scanning for new hardware"));
    }

    private String sendCommand(String cmd) {
        String retval, exec = pathToCADI + (HostSystem.is64bitSystem() ? "driver_x64.exe " : "driver_x86.exe ") + cmd;
        retval = new Shell().timeoutShellCommand(new String[]{"cmd.exe", "/C", "\"" + exec + "\""}, 90000); //1000 milliseconds — one second
        if (retval.contains(" failed")) {
            exec = pathToCADI + (HostSystem.is64bitSystem() ? "driver_x64_elevate.exe " : "driver_x86_elevate.exe ") + cmd;
            retval = new Shell().timeoutShellCommand(new String[]{"cmd.exe", "/C", "\"" + exec + "\""}, 90000); //1000 milliseconds — one second
        }
        Lib.log.appendLog(retval);
        return retval;
    }

    public static class Log {

        private static StringBuffer cadiLog;
        private final String CRLF = System.lineSeparator();

        public Log() {
            cadiLog = new StringBuffer();
        }

        public void appendLog(String logData) {
            appendLog(true, logData);
        }

        public void appendLog(boolean NewLine, String logData) {
            cadiLog.append(logData).append(NewLine ? CRLF : "");
        }

        public void clearLog() {
            cadiLog.delete(0, cadiLog.length());
        }

        public String readLog() {
            return cadiLog.toString();
        }
    }
}
