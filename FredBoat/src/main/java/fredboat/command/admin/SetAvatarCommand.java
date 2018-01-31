package fredboat.command.admin;

import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.feature.metrics.Metrics;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.util.rest.Http;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.Message.Attachment;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

/**
 * This command allows a bot admin to change the avatar of FredBoat
 */
public class SetAvatarCommand extends Command implements ICommandRestricted {

    private static final Logger log = LoggerFactory.getLogger(SetAvatarCommand.class);

    public SetAvatarCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        String imageUrl = null;

        if (!context.msg.getAttachments().isEmpty()) {
            Attachment attachment = context.msg.getAttachments().get(0);
            imageUrl = attachment.getUrl();
        } else if (context.hasArguments()) {
            imageUrl = context.args[0];
        }

        if (imageUrl != null && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
            setBotAvatar(context, imageUrl);
        } else {
            HelpCommand.sendFormattedCommandHelp(context);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <imageUrl> OR <attachment>\n#Set the bot avatar to the image provided by the url or attachment.";
    }

    //return false if the provided url was not an image
    private void setBotAvatar(CommandContext context, String imageUrl) {
        try (Response response = Http.get(imageUrl).execute()) {

            if (Http.isImage(response)) {
                //noinspection ConstantConditions
                InputStream avatarData = response.body().byteStream();
                context.guild.getJDA().getSelfUser().getManager().setAvatar(Icon.from(avatarData))
                        .queue(__ -> {
                                    Metrics.successfulRestActions.labels("setAvatar").inc();
                                    context.reply("Avatar has been set successfully!");
                                },
                                t -> {
                                    CentralMessaging.getJdaRestActionFailureHandler("Failed to set avatar " + imageUrl).accept(t);
                                    context.reply("Error setting avatar. Please try again later.");
                                }
                        );
            } else {
                context.reply("Provided link/attachment is not an image.");
            }
        } catch (IOException e) {
            String message = "Failed to fetch the image.";
            log.error(message, e);
            context.replyWithName(message);
        }
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
