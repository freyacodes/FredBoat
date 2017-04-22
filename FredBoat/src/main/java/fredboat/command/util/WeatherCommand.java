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

package fredboat.command.util;

import fredboat.Config;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.event.EventListenerBoat;
import fredboat.feature.I18n;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

public class WeatherCommand extends Command implements IUtilCommand {
   
    public static String LoadWeather(String g_city){
      String data = "";
    	try {
      	URL url = new URL("http://api.openweathermap.org/data/2.5/find?q="+g_city+"&units=metric&appid="+ Config.CONFIG.getOpenWeatherKey());
      	InputStream is = url.openStream();
      	BufferedReader br = new BufferedReader(new InputStreamReader(is));
      	String line;
    	while ( (line = br.readLine()) != null){
    		data = data+line;
    	}
    	br.close();
    	is.close();
      }catch (Exception e) {
        e.printStackTrace();
      }
    	String output = "[Weather]\n";
    	 JSONParser parser = new JSONParser();
    	try {
			Object obj = parser.parse(data);
			JSONObject jsonObject = (JSONObject)obj;
			JSONArray arr = (JSONArray)jsonObject.get("list");
			jsonObject = (JSONObject)arr.get(0);
			output+=(String)jsonObject.get("name") +"\nTemp:"; //city
			jsonObject = (JSONObject)jsonObject.get("main");
			output+=(String)jsonObject.get("temp").toString() + "°C | Min:"+ (String)jsonObject.get("temp_min").toString()+ "°C | Max:"+(String)jsonObject.get("temp_max").toString() + "°C|\nPressure: "+(String)jsonObject.get("pressure").toString() + " hPA\n";
    	 }catch (Exception e) {
    	        e.printStackTrace();
    	      }
      return output;
    }
	

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
	if (args.length < 2) {
	    String command = args[0].substring(Config.CONFIG.getPrefix().length());
	    HelpCommand.sendFormattedCommandHelp(guild, channel, invoker, command);
	    return;
        }
        String res = message.getRawContent().substring(args[0].length() + 1);
        Message myMsg;
        try {

            myMsg = channel.sendMessage('\u200b'+ LoadWeather(res)).complete(true);

            //EventListenerBoat.messagesToDeleteIfIdDeleted.put(message.getId(), myMsg);
        } catch (RateLimitedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1} <city>\n#Show the weather.";
    }
}
