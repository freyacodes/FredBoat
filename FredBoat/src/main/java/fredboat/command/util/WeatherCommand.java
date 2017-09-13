package fredboat.command.util;

import fredboat.Config;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.feature.I18n;
import fredboat.util.rest.Weather;
import fredboat.util.rest.models.weather.RetrievedWeather;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.text.MessageFormat;

public class WeatherCommand extends Command implements IUtilCommand {

    private Weather weather;

    public WeatherCommand(Weather weatherImplementation) {
        weather = weatherImplementation;
    }

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {

        channel.sendMessage(I18n.get(guild).getString("weatherLoading")).queue(outMsg -> {
            if (args.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    sb.append(args[i]);
                    sb.append(" ");
                }

                String query = sb.toString().trim();
                String alphanumericalQuery = query.replaceAll("[^A-Za-z0-9 ]", "");

                RetrievedWeather currentWeather = weather.getCurrentWeatherByCity(alphanumericalQuery);

                if (currentWeather.isError()) {
                    outMsg.editMessage(
                            MessageFormat.format(I18n.get(guild).getString("weatherError"),
                                    query.toUpperCase())).queue();

                } else {
                    MessageBuilder messageBuilder = new MessageBuilder();
                    messageBuilder.append(
                            MessageFormat.format(I18n.get(guild).getString("weatherLocation"),
                                    currentWeather.getLocation()));
                    messageBuilder.append("\n");

                    messageBuilder.append(
                            MessageFormat.format(I18n.get(guild).getString("weatherDescription"),
                                    currentWeather.getWeatherDescription()));
                    messageBuilder.append("\n");

                    messageBuilder.append(
                            MessageFormat.format(I18n.get(guild).getString("weatherTemperature"),
                                    currentWeather.getTemperature()));

                    outMsg.editMessage(messageBuilder.build()).queue();
                }
                return;
            }

            outMsg.editMessage(
                    MessageFormat.format(I18n.get(guild).getString("weatherUsageError"), Config.CONFIG.getPrefix()))
                    .queue();
        });
    }

    @Override
    public String help(Guild guild) {
        return I18n.get(guild).getString("helpWeatherCommand");
    }
}
