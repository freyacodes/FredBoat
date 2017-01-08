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

import fredboat.common.db.entities.UserSession;
import java.util.HashMap;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Configuration
@EnableAutoConfiguration(exclude = {HibernateJpaAutoConfiguration.class, DataSourceAutoConfiguration.class})
@Controller
@ComponentScan
public class RestController {

    public static RestController ins = null;
    public String token = null;
    private final HashMap<String, String> HEADERS = null;
    private String[] args = null;
    public final String baseUrl;

    @SuppressWarnings("LeakingThisInConstructor")
    public RestController(String token, String[] args, String baseUrl) {
        if (ins != null) {
            throw new IllegalStateException("Only one instance may exist.");
        }

        this.token = token;
        this.args = args;
        this.baseUrl = baseUrl;
        HEADERS.put("authorization", token);

        ins = this;
    }

    public void start() {
        SpringApplication.run(RestController.class, args);
    }

    public boolean isAuthenticated(HttpServletRequest request) {
        return request.getHeader("authorization").equals(token);
    }

    @RequestMapping("/callback")
    @ResponseBody
    private String callback(HttpServletRequest request, HttpServletResponse response) {
        AccountManager.CallbackResult result = AccountManager.handleCallback(request.getParameter("code"));

        response.addCookie(new Cookie("authentication", result.getUnhashedToken()));

        String script =  "localStorage.setItem(\"authentication\", \""+result.getUnhashedToken()+"\");"
                + "window.location = \"" + baseUrl + "\";";

        return "<head><script>\n"+script+"\n</script></head>";
    }

    @RequestMapping("/myGuilds")
    @ResponseBody
    private String getMyGuilds(HttpServletRequest request, HttpServletResponse response) {
        JSONObject body = new JSONObject();

        UserSession uconfig = SessionManager.findSession(request.getHeader("Authorization"));

        return body.toString();
    }

}
