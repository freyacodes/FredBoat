/*
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

package fredboat.config.property;

import fredboat.commandmeta.CommandInitializer;
import fredboat.shared.constant.DistributionEnum;

import java.util.List;

/**
 * Created by napster on 19.02.18.
 */
public interface AppConfig {

    String DEFAULT_PREFIX = ";;";

    DistributionEnum getDistribution();

    default boolean isPatronDistribution() {
        return getDistribution() == DistributionEnum.PATRON;
    }

    default boolean isDevDistribution() {
        return getDistribution() == DistributionEnum.DEVELOPMENT;
    }

    default boolean isMusicDistribution() {
        return getDistribution() == DistributionEnum.MUSIC;
    }

    default String getPrefix() {
        return DEFAULT_PREFIX;
    }

    boolean isRestServerEnabled();

    List<Long> getAdminIds();

    boolean useAutoBlacklist();

    int getPlayerLimit();

    /**
     * @return empty string for default status
     */
    String getGame();

    default String getStatus() {
        String game = getGame();
        if (game.isEmpty()) {
            return "Say " + getPrefix() + CommandInitializer.HELP_COMM_NAME;
        } else {
            return game;
        }
    }

    boolean getContinuePlayback();
}
