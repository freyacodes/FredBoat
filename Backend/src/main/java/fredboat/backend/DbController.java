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

package fredboat.backend;

import com.google.gson.Gson;
import fredboat.db.entity.main.GuildConfig;
import fredboat.db.repos.impl.cache.SpringSearchResultRepo;
import fredboat.db.repos.impl.main.SpringGuildConfigRepo;
import org.hibernate.internal.util.ReflectHelper;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.entities.SaucedEntity;
import space.npstr.sqlsauce.fp.types.EntityKey;

import javax.annotation.CheckReturnValue;
import java.io.Serializable;
import java.lang.reflect.Constructor;

/**
 * Created by napster on 16.02.18.
 */
@RestController
public class DbController {

    private final SpringGuildConfigRepo guildConfigRepo;
    private final SpringSearchResultRepo searchResultRepo;
    private final Gson gson = new Gson();

    public DbController(SpringGuildConfigRepo guildConfigRepo,
                        SpringSearchResultRepo searchResultRepo) {

        this.guildConfigRepo = guildConfigRepo;
        this.searchResultRepo = searchResultRepo;
    }

    @RequestMapping("/main/guildconfig/{id}")
    public String guildConfig(@PathVariable("id") String id) {
        GuildConfig gc = guildConfigRepo.findById(id).orElse(newInstance(EntityKey.of(id, GuildConfig.class)));
        return gson.toJson(gc);
    }


    @CheckReturnValue
    private static <E extends SaucedEntity<I, E>, I extends Serializable> E newInstance(final EntityKey<I, E> id) {
        try {
            Constructor<E> constructor = ReflectHelper.getDefaultConstructor(id.clazz);
            final E entity = constructor.newInstance((Object[]) null);
            return entity.setId(id.id);
        } catch (final ReflectiveOperationException e) {
            final String message = String.format("Could not construct an entity of class %s with id %s",
                    id.clazz.getName(), id.toString());
            throw new DatabaseException(message, e);
        }
    }
}
