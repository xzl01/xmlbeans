/*   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package tools.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * A utility class for getting data from jar files
 */
public class JarUtil {

    /**
     * returns an File Object within the given jarFile as a String. jarFile must exist in classpath
     * pre: jar containing resource is in the classpath
     */
    public static File getResourceFromJarasFile(String pathToResource)
        throws IOException {

        String[] tokens = pathToResource.split("/");
        String fileName = tokens[tokens.length - 1];
        tokens = fileName.split("\\.");
        String extension = (tokens.length < 2) ? null : "." + tokens[1];
        String prefix = (tokens[0].length() < 3) ? tokens[0] + "abc" : tokens[0];
        File temp = File.createTempFile(prefix, extension);
        temp.deleteOnExit();
        try (PrintWriter pr = new PrintWriter(new FileWriter(temp))) {
            String content = getResourceFromJar(pathToResource);
            pr.write(content);
        }
        return temp;
    }


    /**
     * returns the resource as String
     */
    public static String getResourceFromJar(String pathToResource)
        throws IOException {

        BufferedReader in = null;
        try {
            InputStream is = getResourceFromJarasStream(pathToResource);
            in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            for (int readChr; (readChr = in.read(buf)) > -1; ) {
                sb.append(buf, 0, readChr);
            }
            return sb.toString();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * returns an item within the given jarFile as a Stream
     */

    public static InputStream getResourceFromJarasStream(String pathToResource)
        throws IOException {
        InputStream resource = ClassLoader.getSystemClassLoader().getResourceAsStream(
            pathToResource);
        if (resource == null) {
            throw new IOException(" Resource " + pathToResource + " was not found. " +
                                  "Make sure Jar w/ resource is on classpath");
        }
        return resource;
    }
}

