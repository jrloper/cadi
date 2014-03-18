/*DataType provides string tools 
 *Copyright (C) 2013  Adam Outler
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/**
 *
 * @author Adam Outler adamoutler@gmail.com
 */
public class DataType {

    public static class Strings {

        /**
         * replaces the last instance of a string
         *
         * @param string original string
         * @param toReplace value to replace
         * @param replacement replace with this
         * @return string with last value replaced
         * @author Adam Outler adamoutler@gmail.com
         */
        public static String replaceLast(String string, String toReplace, String replacement) {
            int pos = string.lastIndexOf(toReplace);
            if (pos > -1) {
                return string.substring(0, pos)
                        + replacement
                        + string.substring(pos + toReplace.length(), string.length());
            } else {
                return string;
            }
        }

        /**
         * removes leading spaces from line
         *
         * @param line string to remove spaces from
         * @return line without any leading spaces
         * @author Adam Outler adamoutler@gmail.com
         */
        public static String removeLeadingSpaces(String line) {
            while (line.startsWith(" ")) {
                line = line.replaceFirst(" ", "");
            }
            return line;
        }

        /**
         * removes leading and trailing spaces
         *
         * @param line original value
         * @return original value without leading or trailing spaces
         * @author Adam Outler adamoutler@gmail.com
         */
        public static String removeLeadingAndTrailingSpaces(String line) {
            while (line.startsWith(" ")) {
                line = line.replaceFirst(" ", "");
            }
            while (line.endsWith(" ")) {
                StringBuilder b = new StringBuilder(line);
                b.replace(line.lastIndexOf(" "), line.lastIndexOf(" ") + 1, "");
                line = b.toString();
            }
            return line;
        }

        /**
         * remove trailing spaces
         *
         * @param line original value
         * @return original value without trailing spaces
         * @author Adam Outler adamoutler@gmail.com
         */
        public static String removeTrailingSpaces(String line) {
            while (line.endsWith(" ")) {
                StringBuilder b = new StringBuilder(line);
                b.replace(line.lastIndexOf(" "), line.lastIndexOf(" ") + 1, "");
                line = b.toString();
            }
            return line;
        }

        /**
         * reads a stream and returns a string
         *
         * @param is stream to read
         * @return stream converted to string
         * @author Adam Outler adamoutler@gmail.com
         */
        public static String convertStreamToString(InputStream is) {
            Scanner s = new Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }

        /**
         * converts a String to an InputStream
         *
         * @param input string to turn into an InputStream
         * @return InputStream representation of the input string.
         * @author Adam Outler adamoutler@gmail.com
         */
        public static InputStream convertStringToStream(String input) {
            InputStream bas = new ByteArrayInputStream(input.getBytes());
            return bas;

        }

        /**
         * takes a array list and converts to string array
         *
         * @param list input array list
         * @return output string array
         * @author Adam Outler adamoutler@gmail.com
         */
        public static String[] convertArrayListToStringArray(ArrayList<String> list) {
            String[] StringArray = new String[list.size()];
            for (int i = 0; i <= list.size() - 1; i++) {
                StringArray[i] = list.get(i).toString();
            }
            return StringArray;
        }

        /**
         * Returns an array of Strings from a source String.
         *
         * @param inputString contains comma delimited collection of strings
         * each surrounded by quotations.
         * @return string array result of breaking on commas
         * @author Jeremy Loper jrloper@gmail.com
         */
        public static String[] convertStringToArray(String inputString) {
            DataType.Strings.removeLeadingAndTrailingSpaces(inputString);
            String[] outputArray = {};
            int currentQuotePosition = 0;
            int lastQuotePosition = 0;

            for (int i = 0; i <= inputString.length(); i++, currentQuotePosition = inputString.indexOf("\",", currentQuotePosition)) {
                if (inputString.length() != currentQuotePosition) {
                    outputArray[i] = inputString.substring(lastQuotePosition, currentQuotePosition - 1);
                    lastQuotePosition = currentQuotePosition++;
                } else {
                    outputArray[i] = inputString.substring(lastQuotePosition, currentQuotePosition);
                    break;
                }
            }
            return outputArray;
        }

        /**
         * gets a random hexadecimal string
         *
         * @param len length of string to return
         * @return random hex string of specified length
         * @author Adam Outler adamoutler@gmail.com
         */
        public static String generateRandomHexString(int len) {
            final char[] chars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
            String random = "";
            for (int i = 0; i < len; i++) {
                random = random + chars[new Random().nextInt(chars.length)];
            }
            return random;
        }

        /**
         * Converts an array to a string delimited by a space.
         *
         * @param stringarray
         * @return string representation of an array.
         * @author Adam Outler adamoutler@gmail.com
         */
        public static String arrayToString(String[] stringarray) {
            String str = " ";
            for (String stringarray1 : stringarray) {
                str = str + " " + stringarray1;
            }
            Lib.log.appendLog("arrayToString " + stringarray + " expanded to: " + str);
            return str;
        }
    }

    public final class GUID implements Serializable, Comparable<GUID> {

        private final long mostSigBits;
        private final long leastSigBits;

        public GUID(long mostSigBits, long leastSigBits) {
            super();
            this.mostSigBits = mostSigBits;
            this.leastSigBits = leastSigBits;
        }

        public GUID(String name) {
            super();
            mostSigBits = new Long(Long.MAX_VALUE);
            leastSigBits = new Long(Long.MAX_VALUE);
            fromString(name);
        }

        public GUID fromString(String name) {
            String[] components = name.split("-");
            if (components.length != 5) {
                throw new IllegalArgumentException("Invalid GUID string: " + name);
            }
            for (int i = 0; i < 5; i++) {
                components[i] = "0x" + components[i];
            }
            long mSigBits = Long.decode(components[0]).longValue();
            mSigBits <<= 16;
            mSigBits |= Long.decode(components[1]).longValue();
            mSigBits <<= 16;
            mSigBits |= Long.decode(components[2]).longValue();
            long lSigBits = Long.decode(components[3]).longValue();
            lSigBits <<= 48;
            lSigBits |= Long.decode(components[4]).longValue();
            return new GUID(mostSigBits, lSigBits);
        }

        public int variant() {
            return (int) ((leastSigBits >>> (64 - (leastSigBits >>> 62))) & (leastSigBits >> 63));
        }

        @Override
        public String toString() {
            return digits(mostSigBits >> 32, 8) + "-" + digits(mostSigBits >> 16, 4) + "-" + digits(mostSigBits, 4) + "-" + digits(leastSigBits >> 48, 4) + "-" + digits(leastSigBits, 12);
        }

        private String digits(long val, int digits) {
            long hi = 1L << (digits * 4);
            return Long.toHexString(hi | (val & (hi - 1))).substring(1);
        }

        @Override
        public int hashCode() {
            long hilo = mostSigBits ^ leastSigBits;
            return ((int) (hilo >> 32)) ^ (int) hilo;
        }

        @Override
        public boolean equals(Object obj) {
            if ((null == obj) || (obj.getClass() != this.getClass())) {
                return false;
            }
            GUID id = (GUID) obj;
            return mostSigBits == id.mostSigBits && leastSigBits == id.leastSigBits;
        }

        @Override
        public int compareTo(GUID val) {
            return this.mostSigBits < val.mostSigBits ? -1 : (this.mostSigBits > val.mostSigBits ? 1 : (this.leastSigBits < val.leastSigBits ? -1 : (this.leastSigBits > val.leastSigBits ? 1 : 0)));
        }

    }
}
