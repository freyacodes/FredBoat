/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.util;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.*;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CacheUtil {

    private static HashMap<String, File> cachedURLFiles = new HashMap<>();

    private CacheUtil() {
    }

    public static File getImageFromURL(String url) {
        if (cachedURLFiles.containsKey(url) && cachedURLFiles.get(url).exists()) {
            //Already cached
            return cachedURLFiles.get(url);
        } else {
            InputStream is;
            FileOutputStream fos;
            File tmpFile = null;
            try {
                Matcher matcher = Pattern.compile("(\\.\\w+$)").matcher(url);
                String type = matcher.find() ? matcher.group(1) : "";
                tmpFile = File.createTempFile(UUID.randomUUID().toString(), type);
                is = Unirest.get(url).asBinary().getRawBody();
                FileWriter writer = new FileWriter(tmpFile);
                fos = new FileOutputStream(tmpFile);

                byte[] buffer = new byte[1024 * 10];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                is.close();
                fos.close();

                cachedURLFiles.put(url, tmpFile);
                return tmpFile;
            } catch (IOException ex) {
                tmpFile.delete();
                throw new RuntimeException(ex);
            } catch (UnirestException ex) {
                tmpFile.delete();
                throw new RuntimeException(ex);
            }
        }
    }

}
