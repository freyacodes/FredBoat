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

package fredboat.db.repositories.impl;

import fredboat.db.repositories.api.IRepo;
import space.npstr.sqlsauce.DatabaseWrapper;
import space.npstr.sqlsauce.entities.SaucedEntity;
import space.npstr.sqlsauce.fp.types.EntityKey;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Created by napster on 05.02.18.
 */
public abstract class SqlSauceRepo<I extends Serializable, E extends SaucedEntity<I, E>> implements IRepo<I, E> {

    protected final DatabaseWrapper dbWrapper;
    protected final Class<E> entityClass;

    public SqlSauceRepo(DatabaseWrapper dbWrapper, Class<E> entityClass) {
        this.dbWrapper = dbWrapper;
        this.entityClass = entityClass;
    }

    public DatabaseWrapper getDatabaseWrapper() {
        return dbWrapper;
    }

    public Class<E> getEntityClass() {
        return entityClass;
    }

    @Nullable
    @Override
    public E get(I id) {
        return dbWrapper.getEntity(EntityKey.of(id, entityClass));
    }

    @Override
    public void delete(I id) {
        dbWrapper.deleteEntity(EntityKey.of(id, entityClass));
    }

    @Override
    public E fetch(I id) {
        return dbWrapper.getOrCreate(EntityKey.of(id, entityClass));
    }

    @Override
    public E merge(E entity) {
        return dbWrapper.merge(entity);
    }
}
