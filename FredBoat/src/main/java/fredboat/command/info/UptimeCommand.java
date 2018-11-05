package fredboat.command.info;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IInfoCommand;
import fredboat.feature.I18n;
import fredboat.main.Launcher;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import net.dv8tion.jda.core.entities.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class UptimeCommand extends Command implements IInfoCommand {

    public UptimeCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        context.reply(getUptime(context));
    }

    public static Message getUptime(@Nullable Context context) {
        long totalSecs = (System.currentTimeMillis() - Launcher.START_TIME) / 1000;
        int days = (int) (totalSecs / (60 * 60 * 24));
        int hours = (int) ((totalSecs / (60 * 60)) % 24);
        int mins = (int) ((totalSecs / 60) % 60);
        int secs = (int) (totalSecs % 60);

        final ResourceBundle i18n;
        if (context != null) {
            i18n = context.getI18n();
        } else {
            i18n = I18n.DEFAULT.getProps();
        }

        String str = MessageFormat.format(i18n.getString("uptimeParagraph"),
                days, hours, mins, secs)
                + "\n";

        return CentralMessaging.getClearThreadLocalMessageBuilder().append(str).build();
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#Shows uptime of this bot.";
    }
}
