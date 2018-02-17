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

package fredboat.backend.rest;

import fredboat.backend.Application;
import fredboat.db.entity.main.BlacklistEntry;
import fredboat.db.repositories.api.BlacklistRepo;
import fredboat.db.repositories.impl.rest.RestBlacklistRepo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by napster on 17.02.18.
 */
@RestController
@RequestMapping("/" + Application.API_VERSION + RestBlacklistRepo.PATH)
public class BlacklistController extends EntityController<Long, BlacklistEntry> implements BlacklistRepo {

    protected final BlacklistRepo blacklistRepo;

    public BlacklistController(BlacklistRepo repo) {
        super(repo);
        this.blacklistRepo = repo;
    }

    @GetMapping("/loadall")
    @Override
    public List<BlacklistEntry> loadBlacklist() {
        return blacklistRepo.loadBlacklist();
    }
}
