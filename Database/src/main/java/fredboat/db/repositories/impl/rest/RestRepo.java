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
import com.google.gson.JsonSyntaxException;
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
 * Counterpart to the EntityController of the Quarterdeck module.
 */
public abstract class RestRepo<I extends Serializable, E extends SaucedEntity<I, E>> implements Repo<I, E> {

    protected static final Logger log = LoggerFactory.getLogger(RestRepo.class);

    public static final int API_VERSION = 0;
    public static final String VERSION_PATH = "v" + API_VERSION + "/";

    protected final String path;
    protected final Class<E> entityClass;
    protected final Http http;
    protected final Gson gson;
    protected final String auth;

    /**
     * @param path base path of this resource, including the version and a trailing slash
     *             Example: http://quarterdeck:4269/v1/blacklist/
     */
    public RestRepo(String path, Class<E> entityClass, Http http, Gson gson, String auth) {
        this.path = path;
        this.entityClass = entityClass;
        this.http = http;
        this.gson = gson;
        this.auth = auth;
    }

    public Class<E> getEntityClass() {
        return entityClass;
    }

    @Override
    public void delete(I id) { //todo success handling?
        try {
            Http.SimpleRequest delete = http.post(path + "delete", gson.toJson(id), "application/json");
            //noinspection ResultOfMethodCallIgnored
            auth(delete).execute();
        } catch (IOException | JsonSyntaxException e) {
            throw new BackendException(String.format("Could not delete entity with id %s of class %s", id, entityClass), e);
        }
    }

    @Override
    public E fetch(I id) {
        try {
            Http.SimpleRequest fetch = http.post(path + "fetch", gson.toJson(id), "application/json");
            return gson.fromJson(auth(fetch).asString(), entityClass);
        } catch (IOException | JsonSyntaxException e) {
            throw new BackendException(String.format("Could not fetch entity with id %s of class %s", id, entityClass), e);
        }
    }

    @Override
    public E merge(E entity) {
        try {
            Http.SimpleRequest merge = http.post(path + "merge", gson.toJson(entity), "application/json");
            return gson.fromJson(auth(merge).asString(), entityClass);
        } catch (IOException | JsonSyntaxException e) {
            throw new BackendException(String.format("Could not merge entity with id %s of class %s", entity.getId(), entityClass), e);
        }
    }

    /**
     * @return the provided request but authed
     */
    protected Http.SimpleRequest auth(Http.SimpleRequest request) {
        return request.auth(auth);
    }
}
