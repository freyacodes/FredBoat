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

import javax.annotation.Nullable;

/**
 * Created by napster on 05.02.18.
 */
public interface IRepo<I, E> {

    /**
     * @param id id of the entity that shall be returned
     * @return the entity of the provided id, if such an entity exists in the database, null otherwise
     */
    @Nullable
    E get(I id);

    /**
     * @param id of the entity that shall be deleted
     */
    void delete(I id);

    /**
     * @param id id of the entity that shall be returned
     * @return the entity of the provided id, if such an entity exists in the database, a default entity otherwise.
     * Expect this to return a entity constructed through its default constructor, with setId(id) called on it.
     */
    E fetch(I id);

    /**
     * @param entity entity to be merged into the database
     * @return the merged entity
     */
    E merge(E entity);
}
