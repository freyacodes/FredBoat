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

package fredboat.server.webentity;

import java.util.List;

public class WebGuild {

    private final long id;
    private final String name;
    private final String icon;
    private final long owner;
    private final List<WebRole> roles;
    private final List<WebTextChannel> channels;

    public WebGuild(long id, String name, String icon, long owner, List<WebRole> roles, List<WebTextChannel> channels) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.owner = owner;
        this.roles = roles;
        this.channels = channels;
    }

    public WebGuild(String id, String name, String icon, String owner, List<WebRole> roles, List<WebTextChannel> channels) {
        this.id = Long.parseLong(id);
        this.name = name;
        this.icon = icon;
        this.owner = Long.parseLong(owner);
        this.roles = roles;
        this.channels = channels;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public long getOwner() {
        return owner;
    }

    public List<WebRole> getRoles() {
        return roles;
    }

    public List<WebTextChannel> getChannels() {
        return channels;
    }

}
