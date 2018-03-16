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

package fredboat.test;

import fredboat.messaging.internal.Context;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import javax.annotation.Nullable;

/**
 * Created by napster on 02.10.17.
 */
public class FakeContext extends Context {

    @Nullable
    private final TextChannel channel;
    @Nullable
    private final Member member;
    @Nullable
    private final Guild guild;

    public FakeContext(@Nullable TextChannel channel, @Nullable Member member, @Nullable Guild guild) {
        this.channel = channel;
        this.member = member;
        this.guild = guild;
    }

    @Nullable
    @Override
    public TextChannel getTextChannel() {
        return channel;
    }

    @Nullable
    @Override
    public Guild getGuild() {
        return guild;
    }

    @Nullable
    @Override
    public Member getMember() {
        return member;
    }

    @Nullable
    @Override
    public User getUser() {
        return getMember() != null ? getMember().getUser() : null;
    }
    
}
