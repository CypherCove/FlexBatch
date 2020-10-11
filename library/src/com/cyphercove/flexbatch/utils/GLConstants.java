/*
 ******************************************************************************
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

/**
 * OpenGL Constants that are missing from {@link com.badlogic.gdx.graphics.GL20 GL20} and
 * {@link com.badlogic.gdx.graphics.GL30 GL30}.
 */
public class GLConstants {
    // Desktop OpenGL only:
    public static final int GL_PROGRAM_POINT_SIZE = 0x8642;
    public static final int GL_LINE_SMOOTH = 0x0B20;
    public static final int GL_POINT_SMOOTH = 0x0B10;
    public static final int GL_SMOOTH_LINE_WIDTH_RANGE = 0x0B22;
    public static final int GL_SMOOTH_LINE_WIDTH_GRANULARITY = 0x0B23;
    public static final int GL_SMOOTH_POINT_SIZE_RANGE = 0x0B12;
    public static final int GL_SMOOTH_POINT_SIZE_GRANULARITY = 0x0B13;
}
