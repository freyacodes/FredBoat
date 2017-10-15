package fredboat.command.util;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.rest.APILimitException;
import fredboat.util.rest.Weather;
import fredboat.util.rest.models.weather.RetrievedWeather;
import net.dv8tion.jda.core.EmbedBuilder;

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
                String alphabeticalQuery = query.replaceAll("[^A-Za-z]", "");

                if (alphabeticalQuery == null || alphabeticalQuery.length() == 0) {
                    sendHelpString(context);
                    return;
                }

                RetrievedWeather currentWeather = weather.getCurrentWeatherByCity(alphabeticalQuery);

                if (!currentWeather.isError()) {

                    String title = MessageFormat.format(LOCATION_WEATHER_STRING_FORMAT,
                            currentWeather.getLocation(), currentWeather.getTemperature());

                    EmbedBuilder embedBuilder = CentralMessaging.getColoredEmbedBuilder()
                            .setTitle(title)
                            .setDescription(currentWeather.getWeatherDescription())
                            .setFooter(currentWeather.getDataProviderString(), currentWeather.getDataProviderIcon());

                    if (currentWeather.getThumbnailUrl().length() > 0) {
                        embedBuilder.setThumbnail(currentWeather.getThumbnailUrl());
                    }

                    context.reply(embedBuilder.build());

                } else {
                    switch (currentWeather.errorType()) {
                        case LOCATION_NOT_FOUND:
                            context.reply(context.i18nFormat("weatherLocationNotFound",
                                    "`" + query + "`"));
                            break;

                        default:
                            context.reply((context.i18nFormat("weatherError",
                                    "`" + query.toUpperCase()) + "`"
                            ));
                            break;
                    }
                }
                return;

            } catch (APILimitException e) {
                context.reply(context.i18n("tryLater"));
                return;
            }
        }

        sendHelpString(context);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return HELP_STRING_FORMAT + context.i18n("helpWeatherCommand");
    }

    /**
     * Send help message.
     *
     * @param context Command context of the command.
     */
    private void sendHelpString(@Nonnull CommandContext context) {
        HelpCommand.sendFormattedCommandHelp(context);
    }
}
