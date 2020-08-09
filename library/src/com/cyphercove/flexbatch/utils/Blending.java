/* ******************************************************************************
 * Copyright 2020 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cyphercove.flexbatch.utils;

import com.badlogic.gdx.graphics.GL20;

/**
 * Commonly used blend function factor pairs.
 */
public enum Blending {
    /** GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA */
    Alpha(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
    /** GL_ONE, GL_ONE_MINUS_SRC_ALPHA */
    PremultipliedAlpha(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA),
    /** GL_ONE, GL_ONE */
    Additive(GL20.GL_ONE, GL20.GL_ONE);

    public final int srcBlendFactor, dstBlendFactor;

    Blending(int srcBlendFactor, int dstBlendFactor) {
        this.srcBlendFactor = srcBlendFactor;
        this.dstBlendFactor = dstBlendFactor;
    }
}
