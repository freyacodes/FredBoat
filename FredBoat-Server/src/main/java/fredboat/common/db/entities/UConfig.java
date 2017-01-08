/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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
 *
 */

package fredboat.common.db.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_config")
public class UConfig {

    @Id
    private long userId;
    private String bearer;
    private String refresh;
    private long bearerExpiration; //Unix epoch milliseconds

    public String getBearer() {
        return bearer;
    }

    public String getRefresh() {
        return refresh;
    }

    public long getUserId() {
        return userId;
    }

    public long getBearerExpiration() {
        return bearerExpiration;
    }

    public UConfig setBearer(String bearer) {
        this.bearer = bearer;
        return this;
    }

    public UConfig setRefresh(String refresh) {
        this.refresh = refresh;
        return this;
    }

    public UConfig setUserId(long userId) {
        this.userId = userId;
        return this;
    }

    public UConfig setBearerExpiration(long bearerExpiration) {
        this.bearerExpiration = bearerExpiration;
        return this;
    }

}
