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

import fredboat.db.entity.main.GuildConfig;
import fredboat.db.repositories.impl.SqlSauceGuildConfigRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import space.npstr.sqlsauce.DatabaseWrapper;

import javax.annotation.Nullable;

/**
 * Created by napster on 17.02.18.
 */
@RestController
public class GuildConfigController extends SqlSauceGuildConfigRepo {

    private static final String ROUTE = "/" + Application.API_VERSION + "/guildconfig";

    @Autowired
    public GuildConfigController(@Qualifier("mainDbWrapper") DatabaseWrapper dbWrapper) {
        super(dbWrapper);
    }

    @GetMapping(ROUTE + "/get/{guildId}")
    @Nullable
    @Override
    public GuildConfig get(@PathVariable("guildId") String id) {
        return super.get(id);
    }

    @DeleteMapping(ROUTE + "/delete/{guildId}")
    @Override
    public void delete(@PathVariable("guildId") String id) {
        super.delete(id);
    }

    @GetMapping(ROUTE + "/fetch/{guildId}")
    @Override
    public GuildConfig fetch(@PathVariable("guildId") String id) {
        return super.fetch(id);
    }

    @PostMapping(ROUTE + "/merge")
    @Override
    public GuildConfig merge(@RequestBody GuildConfig entity) {
        return super.merge(entity);
    }
}
