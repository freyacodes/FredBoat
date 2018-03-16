/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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

package fredboat.jda;

import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.stream.Stream;

/**
 * Created by napster on 25.02.18.
 * <p>
 * The Lazy init of the shard manager allows us to resolve circular dependency of our components on the shard manager
 */
@Component
public class JdaEntityProvider implements EmoteProvider, GuildProvider, MemberProvider, RoleProvider, ShardProvider,
        TextChannelProvider, UserProvider, VoiceChannelProvider {

    private final ShardManager shardManager;

    public JdaEntityProvider(@Lazy ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Nullable
    @Override
    public Emote getEmoteById(long emoteId) {
        return shardManager.getEmoteById(emoteId);
    }

    @Override
    public Stream<Emote> streamEmotes() {
        return shardManager.getEmoteCache().stream();
    }

    @Override
    @Nullable
    public Guild getGuildById(long guildId) {
        return shardManager.getGuildById(guildId);
    }

    @Override
    public Stream<Guild> streamGuilds() {
        return shardManager.getGuildCache().stream();
    }

    @Override
    @Nullable
    public Member getMemberById(long guildId, long userId) {
        Guild guild = getGuildById(guildId);
        return guild == null ? null : guild.getMemberById(userId);
    }

    @Override
    @Nullable
    public Role getRoleById(long roleId) {
        return shardManager.getRoleById(roleId);
    }

    @Override
    public Stream<Role> streamRoles() {
        return shardManager.getRoleCache().stream();
    }

    @Override
    public JDA getShardById(int shardId) {
        return shardManager.getShardById(shardId);
    }

    @Override
    public Stream<JDA> streamShards() {
        return shardManager.getShardCache().stream();
    }

    @Override
    @Nullable
    public TextChannel getTextChannelById(long textChannelId) {
        return shardManager.getTextChannelById(textChannelId);
    }

    @Override
    public Stream<TextChannel> streamTextChannels() {
        return shardManager.getTextChannelCache().stream();
    }

    @Override
    @Nullable
    public User getUserById(long userId) {
        return shardManager.getUserById(userId);
    }

    @Override
    public Stream<User> streamUsers() {
        return shardManager.getUserCache().stream();
    }

    @Override
    @Nullable
    public VoiceChannel getVoiceChannelById(long voiceChannelId) {
        return shardManager.getVoiceChannelById(voiceChannelId);
    }

    @Override
    public Stream<VoiceChannel> streamVoiceChannels() {
        return shardManager.getVoiceChannelCache().stream();
    }
}
