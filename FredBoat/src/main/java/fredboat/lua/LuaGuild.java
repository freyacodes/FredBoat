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

package fredboat.lua;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

public class LuaGuild extends LuaTable {

    public Guild guild;
    public JDA jda;

    public LuaGuild(Guild guild) {
        super();
        this.guild = guild;
        jda = guild.getJDA();
        setmetatable(generateMeta());
    }

    private LuaTable generateMeta() {
        LuaTable meta = new LuaTable();

        
        
        meta.set("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue t, LuaValue k) {
                if (k.checkjstring().equals("users")) {
                    LuaTable users =  new LuaTable();
                    int i = 1;
                    for(Member member : guild.getMembers()){
                        users.set(i, new LuaUser(member.getUser()));
                        i++;
                    }
                    return users;
                }
                return LuaValue.NIL;
            }
        });

        return meta;
    }

}
