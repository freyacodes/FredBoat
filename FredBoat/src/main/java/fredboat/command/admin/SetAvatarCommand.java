package fredboat.command.admin;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.FredBoat;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.Message.Attachment;
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

    @Override
    public void onInvoke(CommandContext context) {
        String imageUrl = null;

        if (!context.msg.getAttachments().isEmpty()) {
            Attachment attachment = context.msg.getAttachments().get(0);
            imageUrl = attachment.getUrl();
        } else if (context.args.length > 1) {
            imageUrl = context.args[1];
        }

        if (imageUrl != null) {
            boolean success = setBotAvatar(context, imageUrl);
            if (!success) {
                HelpCommand.sendFormattedCommandHelp(context);
            }
        } else {
            HelpCommand.sendFormattedCommandHelp(context);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <imageUrl> OR <attachment>\n#Sets the bot avatar to the image provided by the url or attachment.";
    }

    private String getImageMimeType(String imageUrl) {
        String mimeType = "";
        try {
            mimeType = Unirest.get(imageUrl).asBinary().getHeaders().getFirst("Content-Type");
        } catch (UnirestException e) {
            log.error("Error retrieving mime type of image!", e);
            throw new RuntimeException(e);
        }
        return mimeType;
    }

    private boolean setBotAvatar(CommandContext context, String imageUrl) {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            String type = getImageMimeType(imageUrl);

            if (type.equals("image/jpeg") || type.equals("image/png") || type.equals("image/gif") || type.equals("image/webp")) {
                InputStream avatarData;
                try {
                    avatarData = Unirest.get(imageUrl).asBinary().getBody();
                    FredBoat.getFirstJDA().getSelfUser().getManager().setAvatar(Icon.from(avatarData))
                            .queue(
                                    success -> context.reply("Avatar has been set successfully!"),
                                    failure -> context.reply("Error setting avatar. Please try again later.")
                            );
                } catch (UnirestException | IOException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
