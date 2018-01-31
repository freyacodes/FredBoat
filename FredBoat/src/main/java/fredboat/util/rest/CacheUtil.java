/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
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

package fredboat.util.rest;

import com.google.common.base.Throwables;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CacheUtil {

    private static HashMap<String, File> cachedURLFiles = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(CacheUtil.class);

    private CacheUtil() {
    }

    public static File getImageFromURL(final String url) {
        if (cachedURLFiles.containsKey(url) && cachedURLFiles.get(url).exists()) {
            //Already cached
            return cachedURLFiles.get(url);
        } else {
            final InputStream is;
            final File tmpFile;
            try {
                final Matcher matcher = Pattern.compile("(\\.\\w+$)").matcher(url);
                final String type = matcher.find() ? matcher.group(1) : "";
                tmpFile = File.createTempFile(UUID.randomUUID().toString(), type);
            } catch (final IOException e) {
                throw new RuntimeException("Could not create a temporary file");
            }
            try (final FileOutputStream fos = new FileOutputStream(tmpFile)) {
                is = new URL(url).openStream();
                final byte[] buffer = new byte[1024 * 10];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                is.close();
                fos.close();

                cachedURLFiles.put(url, tmpFile);
                return tmpFile;
            } catch (final IOException e) {
                if (!tmpFile.delete()) {
                    log.error("Could not delete temporary file {}", tmpFile.getAbsolutePath());
                }
                throw new RuntimeException(e);
            }
        }
    }

    public static <K, V> V getUncheckedUnwrapped(LoadingCache<K, V> cache, K key) {
        try {
            return cache.getUnchecked(key);
        } catch (UncheckedExecutionException e) {
            Throwables.throwIfUnchecked(e.getCause());

            // Will never run.
            throw new IllegalStateException(e);
        }
    }
}
