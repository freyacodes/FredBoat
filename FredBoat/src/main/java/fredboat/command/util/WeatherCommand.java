package fredboat.command.util;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.feature.I18n;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.rest.APILimitException;
import fredboat.util.rest.Weather;
import fredboat.util.rest.models.weather.RetrievedWeather;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;

import javax.annotation.Nonnull;
import java.text.MessageFormat;

public class WeatherCommand extends Command implements IUtilCommand {

    private Weather weather;
    private static final String LOCATION_WEATHER_STRING_FORMAT = "{0} - {1}";
    private static final String HELP_STRING_FORMAT = "{0}{1} <location>\n#";

    public WeatherCommand(Weather weatherImplementation) {
        weather = weatherImplementation;
    }

    @Override
    public void onInvoke(CommandContext context) {

        context.sendTyping();
        if (context.args.length > 1) {

            try {
                StringBuilder argStringBuilder = new StringBuilder();
                for (int i = 1; i < context.args.length; i++) {
                    argStringBuilder.append(context.args[i]);
                    argStringBuilder.append(" ");
                }

                String query = argStringBuilder.toString().trim();
                String alphanumericalQuery = query.replaceAll("[^A-Za-z0-9 ]", "");

                RetrievedWeather currentWeather = weather.getCurrentWeatherByCity(alphanumericalQuery);

                if (!currentWeather.isError()) {

                    String title = MessageFormat.format(LOCATION_WEATHER_STRING_FORMAT,
                            currentWeather.getLocation(), currentWeather.getTemperature());

                    EmbedBuilder embedBuilder = CentralMessaging.getClearThreadLocalEmbedBuilder()
                            .setTitle(title)
                            .setDescription(currentWeather.getWeatherDescription());

                    if (currentWeather.getThumbnailUrl().length() > 0) {
                        embedBuilder.setThumbnail(currentWeather.getThumbnailUrl());
                    }

                    context.reply(embedBuilder.build());
                } else {
                    context.reply(
                            MessageFormat.format(I18n.get(context.guild).getString("weatherError"),
                                    query.toUpperCase()));
                }
                return;

            } catch (APILimitException e) {
                context.reply(I18n.get(context.guild).getString("tryLater"));
                return;
            }
        }

        HelpCommand.sendFormattedCommandHelp(context);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return HELP_STRING_FORMAT + I18n.get(context.getGuild()).getString("helpWeatherCommand");
    }
}
