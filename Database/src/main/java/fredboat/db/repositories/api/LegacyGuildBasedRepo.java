/*
 *
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

package fredboat.db.repositories.api;

import net.dv8tion.jda.core.entities.Guild;

/**
 * Created by napster on 05.02.18.
 * <p>
 * As soon as the db backend is released, entities using this should be migrated to use long ids instead of strings
 */
@Deprecated
public interface LegacyGuildBasedRepo<E> extends Repo<String, E> {

    /**
     * Type safety on top of {@link Repo#delete(Object)} for entities based on guilds.
     */
    default void delete(Guild guild) {
        delete(guild.getId());
    }

    /**
     * Type safety on top of {@link Repo#fetch(Object)} for entities based on guilds.
     */
    default E fetch(Guild guild) {
        return fetch(guild.getId());
    }
}
