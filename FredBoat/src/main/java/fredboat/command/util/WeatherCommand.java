package fredboat.command.util;

import fredboat.Config;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.feature.I18n;
import fredboat.messaging.CentralMessaging;
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
    public void onInvoke(CommandContext context) {

        context.reply(I18n.get(context.guild).getString("weatherLoading"), outMsg -> {
            if (context.args.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < context.args.length; i++) {
                    sb.append(context.args[i]);
                    sb.append(" ");
                }

                String query = sb.toString().trim();
                String alphanumericalQuery = query.replaceAll("[^A-Za-z0-9 ]", "");

                RetrievedWeather currentWeather = weather.getCurrentWeatherByCity(alphanumericalQuery);

                if (currentWeather.isError()) {
                    CentralMessaging.editMessage(outMsg,
                            MessageFormat.format(I18n.get(context.guild).getString("weatherError"),
                                    query.toUpperCase()));

                } else {
                    MessageBuilder messageBuilder = CentralMessaging.getClearThreadLocalMessageBuilder();
                    messageBuilder.append(
                            MessageFormat.format(I18n.get(context.guild).getString("weatherLocation"),
                                    currentWeather.getLocation()));
                    messageBuilder.append("\n");

                    messageBuilder.append(
                            MessageFormat.format(I18n.get(context.guild).getString("weatherDescription"),
                                    currentWeather.getWeatherDescription()));
                    messageBuilder.append("\n");

                    messageBuilder.append(
                            MessageFormat.format(I18n.get(context.guild).getString("weatherTemperature"),
                                    currentWeather.getTemperature()));

                    CentralMessaging.editMessage(outMsg, messageBuilder.build());
                }
                return;
            }

            CentralMessaging.editMessage(outMsg,
                    MessageFormat.format(I18n.get(context.guild).getString("weatherUsageError"),
                            Config.CONFIG.getPrefix()));
        });
    }

    @Override
    public String help(Guild guild) {
        return I18n.get(guild).getString("helpWeatherCommand");
    }
}
