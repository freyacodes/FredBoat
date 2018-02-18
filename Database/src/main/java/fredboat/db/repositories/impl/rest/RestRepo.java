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

package fredboat.db.repositories.impl.rest;

import com.google.gson.Gson;
import fredboat.db.repositories.api.Repo;
import fredboat.util.rest.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.entities.SaucedEntity;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by napster on 17.02.18.
 *
 * Counterpart to the EntityController of the Backend module.
 */
public abstract class RestRepo<I extends Serializable, E extends SaucedEntity<I, E>> implements Repo<I, E> {

    protected static final Logger log = LoggerFactory.getLogger(RestRepo.class);

    protected final String path;
    protected final Class<E> entityClass;
    protected final Http http;
    protected final Gson gson;

    public RestRepo(String path, Class<E> entityClass, Http http, Gson gson) {
        this.path = path;
        this.entityClass = entityClass;
        this.http = http;
        this.gson = gson;
    }

    public Class<E> getEntityClass() {
        return entityClass;
    }

    @Override
    public void delete(I id) { //todo success handling?
        try {
            Http.SimpleRequest delete = http.post(path + "/delete", gson.toJson(id), "application/json");
            //noinspection ResultOfMethodCallIgnored
            delete.execute();
        } catch (IOException e) { //todo decide on error handling strategy
            log.error("Could not DELETE entity with id {} of class {}", id, entityClass, e);
        }
    }

    @Override
    public E fetch(I id) {
        try {
            Http.SimpleRequest fetch = http.post(path + "/fetch", gson.toJson(id), "application/json");
            return gson.fromJson(fetch.asString(), entityClass);
        } catch (IOException e) { //todo decide on error handling strategy
            log.error("Could not FETCH entity with id {} of class {}", id, entityClass, e);
            return null;
        }
    }

    @Override
    public E merge(E entity) {
        try {
            Http.SimpleRequest merge = http.post(path + "/merge", gson.toJson(entity), "application/json");
            return gson.fromJson(merge.asString(), entityClass);
        } catch (IOException e) { //todo decide on error handling strategy
            log.error("Could not MERGE entity with id {} of class {}", entity.getId(), entityClass, e);
            return null;
        }
    }
}
