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
package com.cyphercove.flexbatch.desktop;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import java.io.File;

public class PackTextures {

    private static final String PACK_FILE_EXTENSION = ".atlas";

    public static void main (String[] args) throws Exception {
        doPack("example/assetPrep/multipage atlas source", "example/assets", "multipageFruit");
    }

    private static void doPack (String sourceDir, String targetDir, String atlasBaseFileName) throws Exception {

        //Delete old pack
        File oldPackFile = new File(targetDir + "/" + atlasBaseFileName + PACK_FILE_EXTENSION);
        if (oldPackFile.exists()){
            System.out.println("Deleting old pack file");
            oldPackFile.delete();
        }

        //Pack them
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.atlasExtension = PACK_FILE_EXTENSION;
        TexturePacker.process(
                settings,
                sourceDir,
                targetDir,
                atlasBaseFileName);

    }


}