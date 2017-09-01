/*
 *
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
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

package fredboat.feature.togglz;

import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.file.FileBasedStateRepository;
import org.togglz.core.spi.FeatureManagerProvider;
import org.togglz.core.user.NoOpUserProvider;

import java.io.File;

/**
 * Created by napster on 20.05.17.
 * <p>
 * Creates and provides the feature manager
 */
public class FeatureConfig implements FeatureManagerProvider {

    @Override
    public FeatureManager getFeatureManager() {
        return getTheFeatureManager();
    }

    static FeatureManager getTheFeatureManager() {
        return FeatureManagerHolder.instance;
    }

    @Override
    public int priority() {
        return 0;
    }

    //holder class pattern
    private static class FeatureManagerHolder {
        private static final FeatureManager instance = new FeatureManagerBuilder()
                .featureEnum(FeatureFlags.class)
                .stateRepository(new FileBasedStateRepository(new File("./feature_flags.properties")))
                .userProvider(new NoOpUserProvider())
                .build();
    }
}
