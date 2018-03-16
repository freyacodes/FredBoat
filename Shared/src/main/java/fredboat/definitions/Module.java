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

package fredboat.definitions;

import fredboat.util.Emojis;

import javax.annotation.Nonnull;

/**
 * Created by napster on 15.02.18.
 */ //a locked module cannot be enabled/disabled
public enum Module {

    //@formatter:off                               locked
    //                                    enabledByDef
    ADMIN ("moduleAdmin",      Emojis.KEY,    true, true),
    INFO  ("moduleInfo",       Emojis.INFO,   true, true),
    CONFIG("moduleConfig",     Emojis.GEAR,   true, true),
    MUSIC ("moduleMusic",      Emojis.MUSIC,  true, true),
    MOD   ("moduleModeration", Emojis.HAMMER, true, false),
    UTIL  ("moduleUtility",    Emojis.TOOLS,  true, false),
    FUN   ("moduleFun",        Emojis.DIE,    true, false),
    ;
    //@formatter:on

    @Nonnull
    public final String translationKey;
    @Nonnull
    public final String emoji;
    public final boolean enabledByDefault;
    public final boolean lockedModule;

    Module(@Nonnull String translationKey, @Nonnull String emoji, boolean enabledByDefault, boolean lockedModule) {
        this.translationKey = translationKey;
        this.emoji = emoji;
        this.enabledByDefault = enabledByDefault;
        this.lockedModule = lockedModule;
    }
}
