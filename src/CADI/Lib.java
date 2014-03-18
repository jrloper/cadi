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

import java.io.File;
import CADI.DataType.GUID;
import CADI.Console.Log;

/**
 * @version 2.1
 * @author Jeremy R. Loper <jrloper@gmail.com>
 */
public class Lib {

    public Lib() {
        slash = System.getProperty("file.seperator");
        log = new Log();
        TempFolder = new Target.FileSystem().getTempFolder();
    }

    public static String slash;
    public static Log log;
    public static GUID ANDROID_ADB_ID;
    public static File TempFolder;
}
