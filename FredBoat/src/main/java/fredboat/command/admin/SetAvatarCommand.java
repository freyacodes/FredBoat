package fredboat.command.admin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.MessagingException;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.definitions.PermissionLevel;
import fredboat.feature.metrics.Metrics;
import fredboat.main.BotController;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.rest.Http;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.Message.Attachment;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * This command allows a bot admin to change the avatar of FredBoat
 */
public class SetAvatarCommand extends Command implements ICommandRestricted {

    private static final Logger log = LoggerFactory.getLogger(SetAvatarCommand.class);

    /**
     * The {@code Branding/} directory that the SetAvatarCommand uses.
     * <p>
     * By default uses the github repo, because that's a good common denominator and allows
     * a push to git to update and use the logos without having to redeploy the bot.
     * </p>
     * <p>
     * It's also possible to use a local URI, in the form of file:///loc/ation/of/the/logos/
     * </p>
     */
    private static final URI BRANDING_DIR = URI.create("https://raw.githubusercontent.com/Frederikam/FredBoat/dev/Branding/");

    private static final Map<String, URI> AVATARS = ImmutableMap.<String, URI>builder()
            .put(entry("defaultMusic", URI.create("Music.png")))
            .put(entry("defaultPatron", URI.create("Patreon.png")))
            .put(entry("defaultCE", URI.create("CuttingEdge.png")))

            .put(entry("festiveMusic", URI.create("Event/Music-fireworks-santa.png")))
            .put(entry("festivePatron", URI.create("Event/Patreon-fireworks-santa.png")))
            .build();

    private static Map.Entry<String, URI> entry(String name, /*relative*/ URI file) {
        if (!checkRelativeToBrandingDir(file)) {
            log.error("An avatar is not relative",
                    new IllegalArgumentException(String.format("%s (%s) is not relative", name, file)));
        }

        URI resolve = BRANDING_DIR.resolve(file);

        return Maps.immutableEntry(name, resolve);
    }

    @CheckReturnValue
    private static boolean checkRelativeToBrandingDir(URI uri) {
        URI resolve = BRANDING_DIR.resolve(uri);

        //if the relative form of the resolved URI is equal to the original, the original was relative as well
        return BRANDING_DIR.relativize(resolve).equals(uri);
    }

    public SetAvatarCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        URI imageUrl = null;

        try {
            if (!context.msg.getAttachments().isEmpty()) {
                Attachment attachment = context.msg.getAttachments().get(0);
                imageUrl = new URI(attachment.getUrl());
            } else if (context.hasArguments()) {
                imageUrl = AVATARS.containsKey(context.args[0]) ? AVATARS.get(context.args[0]) : new URI(context.args[0]);
            }
        } catch (URISyntaxException e) {
            context.reply("Not a valid link.");
            return;
        }
        if (imageUrl != null && imageUrl.getScheme() != null) {
            if (imageUrl.getScheme().equals("branding")) {
                // for branding:resource.png URLs

                // clear off the 'scheme'
                imageUrl = URI.create(imageUrl.getSchemeSpecificPart());
                if (!checkRelativeToBrandingDir(imageUrl)) {
                    throw new MessagingException("url is not relative");
                }
                imageUrl = BRANDING_DIR.resolve(imageUrl);
            }
            Icon avatar;
            switch (imageUrl.getScheme()) {
                case "http":
                case "https":
                    avatar = fetchRemote(imageUrl);
                    break;
                case "file":
                    avatar = fetchFile(imageUrl);
                    break;
                default:
                    throw new MessagingException("Not a readable image");
            }

            setBotAvatar(context, avatar);
            return;
        }

        // if not handled it's not a proper invocation
        HelpCommand.sendFormattedCommandHelp(context);
    }

    private static Icon fetchRemote(URI uri) {
        try (Response response = BotController.HTTP.get(uri.toString()).execute()) {
            if (Http.isImage(response)) {
                //noinspection ConstantConditions
                InputStream avatarData = response.body().byteStream();
                return Icon.from(avatarData);
            } else {
                throw new IOException("Provided link/attachment is not an image.");
            }
        } catch (IOException e) {
            throw new MessagingException("Failed to fetch the image.", e);
        }
    }

    private static Icon fetchFile(URI resource) {
        try {
            return Icon.from(new File(resource));
        } catch (IOException e) {
            throw new MessagingException("Not a valid image.");
        }
    }

    private void setBotAvatar(CommandContext context, Icon icon) {
        context.guild.getJDA().getSelfUser().getManager().setAvatar(icon)
                .queue(__ -> {
                            Metrics.successfulRestActions.labels("setAvatar").inc();
                            context.reply("Avatar has been set successfully!");
                        },
                        t -> {
                            CentralMessaging.getJdaRestActionFailureHandler("Failed to set avatar").accept(t);
                            context.reply("Error setting avatar. Please try again later.");
                        }
                );
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <imageUrl>, <attachment> OR <keyword>\n" +
                "#Set the bot's profile picture to a different image by providing a url, attachment, or select a default image from our keyword list below:\n" +
                "#  " + String.join(", ", AVATARS.keySet()) + "\n" +
                "#The url can be a file:///, http(s)://, or branding: url\n";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
