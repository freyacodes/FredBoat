/*
 *
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
 */

package fredboat.messaging.internal;

import fredboat.FredBoat;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

/**
 * Created by napster on 11.09.17.
 * <p>
 * Unlike the CommandContext, this context will always look up the entities by their ids, therefore it may be saved and
 * assigned, however it may return null for entities not found anymore.
 */
public class LeakSafeContext extends Context {

    protected final long channelId;
    protected final long guildId;
    protected final long userId;

    public LeakSafeContext(TextChannel channel, Guild guild, Member member) {
        this.channelId = channel.getIdLong();
        this.guildId = guild.getIdLong();
        this.userId = member.getUser().getIdLong();
    }

    public LeakSafeContext(Context context) {
        this(context.getTextChannel(), context.getGuild(), context.getMember());
    }

    @Override
    public TextChannel getTextChannel() {
        return FredBoat.getTextChannelById(Long.toString(channelId));
    }

    @Override
    public Guild getGuild() {
        return FredBoat.getGuildById(guildId);
    }

    @Override
    public Member getMember() {
        Guild guild = getGuild();
        return guild != null ? guild.getMemberById(userId) : null;
    }

    @Override
    public User getUser() {
        Member member = getMember();
        return member != null ? member.getUser() : null;
    }
}
