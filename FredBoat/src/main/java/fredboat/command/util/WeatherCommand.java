package fredboat.command.util;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.util.rest.OpenWeatherAPI;
import fredboat.util.rest.Weather;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

public class WeatherCommand extends Command implements IUtilCommand {

    private Weather weather;

    public WeatherCommand(Weather weatherImplementation) {
        weather = weatherImplementation;
    }

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {

        weather.getCurrentWeatherByCity();
    }

    @Override
    public String help(Guild guild) {
        return null;
    }
}
