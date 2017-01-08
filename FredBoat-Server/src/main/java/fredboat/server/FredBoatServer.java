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

package fredboat.server;

import fredboat.common.db.DatabaseManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Scanner;
import org.json.JSONObject;

public class FredBoatServer {
    
    private static JSONObject credsjson;
    private static final boolean IS_WINDOWS_MACHINE = System.getProperty("os.name").toLowerCase().contains("windows");
    
    @SuppressWarnings("ConvertToTryWithResources")
    public static void main(String[] args) throws FileNotFoundException {
        InputStream is = new FileInputStream(new File("./credentials.json"));
        Scanner scanner = new Scanner(is);
        credsjson = new JSONObject(scanner.useDelimiter("\\A").next());
        scanner.close();
        
        String baseUrl = IS_WINDOWS_MACHINE ? "https://localhost/" : "https://fredboat.frederikam.com/";
        new RestController(getFredBoatToken(), new String[0], baseUrl).start();
        DatabaseManager.startup(credsjson);
        AccountManager.init(getClientId(), getClientSecret());
    }
    
    public static String getFredBoatToken(){
        return credsjson.getString("fredboatToken");
    }
    
    public static String getClientId(){
        return credsjson.getString("clientId");
    }
    
    public static String getClientSecret(){
        return credsjson.getString("secret");
    }
    
}
