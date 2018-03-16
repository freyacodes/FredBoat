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

package fredboat.audio.queue;

import fredboat.jda.JdaEntityProvider;
import fredboat.messaging.internal.LeakSafeContext;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import javax.annotation.Nullable;

public class IdentifierContext extends LeakSafeContext {

    public final String identifier;
    private boolean quiet = false;
    private boolean split = false;
    private long position = 0L;

    public IdentifierContext(JdaEntityProvider jdaEntityProvider, String identifier, TextChannel textChannel, Member member) {
        super(jdaEntityProvider, textChannel, member);
        this.identifier = identifier;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public long getPosition() {
        return position;
    }

    public boolean isSplit() {
        return split;
    }

    public void setSplit(boolean split) {
        this.split = split;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    @Override
    @Nullable
    public TextChannel getTextChannel() {
        return jdaEntityProvider.getTextChannelById(channelId);
    }

    @Override
    @Nullable
    public Guild getGuild() {
        return jdaEntityProvider.getGuildById(guildId);
    }

    @Override
    @Nullable
    public Member getMember() {
        Guild guild = getGuild();
        return guild == null ? null : guild.getMemberById(userId);
    }

    @Override
    @Nullable
    public User getUser() {
        return jdaEntityProvider.getUserById(userId);
    }
}
