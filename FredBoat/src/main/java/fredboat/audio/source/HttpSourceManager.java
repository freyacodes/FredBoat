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

package fredboat.audio.source;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.*;
import static com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools.getHeaderValue;

/**
 * Audio source manager which implements finding audio files from HTTP addresses (either direct links or embedded in html).
 * This completely replaces HttpAudioSourceManager as it already has its function
 */
public class HttpSourceManager extends HttpAudioSourceManager {

    private static final Pattern PLAYLIST_PATTERN = Pattern.compile("<a[^>]*href=\"([^\"]*\\.(?:m3u|pls))\"");
    private static final Pattern CHARSET_PATTERN = Pattern.compile("\\bcharset=([^\\s;]+)\\b");

    private MediaContainerDetectionResult checkHtmlResponse(AudioReference reference, PersistentHttpStream stream, MediaContainerHints hints) {
        StringWriter writer = new StringWriter();
        Matcher mimeMatcher = CHARSET_PATTERN.matcher(hints.mimeType);
        String charset = mimeMatcher.find() ? mimeMatcher.group(1) : null;
        try {
            //reset position to start of stream to get full html content
            stream.seek(0L);
            if(charset != null)
                IOUtils.copy(stream, writer, charset);
            else
                IOUtils.copy(stream, writer, StandardCharsets.UTF_8);
        } catch(IOException ex) {
            throw new FriendlyException("Could not read HTML body", SUSPICIOUS, ex);
        }
        String htmlBody = writer.toString();
        Matcher matcher = PLAYLIST_PATTERN.matcher(htmlBody);
        if(matcher.find()) {
            return detectContainer(resolve(reference, matcher.group(1)), true);
        }
        return null;
    }

    private AudioReference resolve(AudioReference original, String resolve) {
        if(resolve.startsWith("http")) {
            return new AudioReference(resolve, original.title);
        }
        try {
            URL resolved = new URL(new URL(original.identifier), resolve);
            return new AudioReference(resolved.toString(), original.title);
        } catch(MalformedURLException e) {
            throw new FriendlyException("Error resolving relative url of playlist link", SUSPICIOUS, e);
        }
    }

    /*
        Following code is from Sedmelluq's LavaPlayer project (https://github.com/sedmelluq/lavaplayer)
        and therefore under the Apache 2.0 License.
        A copy of the License file is provided in "ThirdPartyLicenses/APACHE2"

        Changes:
         - Added ignoreHtml boolean parameter to detectContainer and detectContainerWithClient
        Other changes are surrounded with comments for clarification.
     */
    @Override
    public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
        AudioReference httpReference = getAsHttpReference(reference);
        if (httpReference == null) {
            return null;
        }

        return handleLoadResult(detectContainer(httpReference, false));
    }

    private AudioReference getAsHttpReference(AudioReference reference) {
        if (reference.identifier.startsWith("https://") || reference.identifier.startsWith("http://")) {
            return reference;
        } else if (reference.identifier.startsWith("icy://")) {
            return new AudioReference("http://" + reference.identifier.substring(6), reference.title);
        }
        return null;
    }

    private MediaContainerDetectionResult detectContainer(AudioReference reference, boolean ignoreHtml) {
        MediaContainerDetectionResult result;

        try (HttpInterface httpInterface = getHttpInterface()) {
            result = detectContainerWithClient(httpInterface, reference, ignoreHtml);
        } catch (IOException e) {
            throw new FriendlyException("Connecting to the URL failed.", SUSPICIOUS, e);
        }

        return result;
    }

    private MediaContainerDetectionResult detectContainerWithClient(HttpInterface httpInterface, AudioReference reference, boolean ignoreHtml) throws IOException {
        try (PersistentHttpStream inputStream = new PersistentHttpStream(httpInterface, new URI(reference.identifier), Long.MAX_VALUE)) {
            int statusCode = inputStream.checkStatusCode();
            String redirectUrl = HttpClientTools.getRedirectLocation(reference.identifier, inputStream.getCurrentResponse());

            if (redirectUrl != null) {
                return new MediaContainerDetectionResult(null, new AudioReference(redirectUrl, null));
            } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                return null;
            } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw new FriendlyException("That URL is not playable.", COMMON, new IllegalStateException("Status code " + statusCode));
            }

            MediaContainerHints hints = MediaContainerHints.from(getHeaderValue(inputStream.getCurrentResponse(), "Content-Type"), null);

            /* START CUSTOM CHANGES */
            MediaContainerDetectionResult detection = MediaContainerDetection.detectContainer(reference, inputStream, hints);
            if(!ignoreHtml && !detection.isReference() && !detection.isContainerDetected() && hints.mimeType.startsWith("text/html")) {
                return checkHtmlResponse(reference, inputStream, hints);
            }
            return detection;
            /* END CUSTOM CHANGES */

        } catch (URISyntaxException e) {
            throw new FriendlyException("Not a valid URL.", COMMON, e);
        }
    }
}