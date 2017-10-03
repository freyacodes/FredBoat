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

package fredboat.command.admin;

import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerDebugCommand extends Command implements ICommandRestricted {

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        JSONArray a = new JSONArray();
        
        for(GuildPlayer gp : PlayerRegistry.getRegistry().values()){
            JSONObject data = new JSONObject();
            data.put("name", gp.getGuild().getName());
            data.put("id", gp.getGuild().getId());
            data.put("users", gp.getCurrentVoiceChannel().getMembers().toString());
            data.put("isPlaying", gp.isPlaying());
            data.put("isPaused", gp.isPaused());
            data.put("songCount", gp.getTrackCount());
            
            a.put(data);
        }
        
        try {
            context.reply(TextUtils.postToPasteService(a.toString()));
        } catch (UnirestException ex) {
            Logger.getLogger(PlayerDebugCommand.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#Show debug information about the music player of this guild.";
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_OWNER;
    }
}
