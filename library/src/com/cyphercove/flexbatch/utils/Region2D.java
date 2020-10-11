/* ******************************************************************************
 * Copyright 2017 See AUTHORS file.
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

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Minimal data to define a two-dimensional region of any Texture or TextureArray. */
public class Region2D {
	public float u, u2, v, v2;
	public int layer;

	public void setFull () {
		u = v = 0f;
		u2 = v2 = 1f;
		layer = 0;
	}

	public void set (TextureRegion region) {
		u = region.getU();
		u2 = region.getU2();
		v = region.getV();
		v2 = region.getV2();
	}

	public void flip (boolean x, boolean y) {
		if (x) {
			float temp = u;
			u = u2;
			u2 = temp;
		}
		if (y) {
			float temp = v;
			v = v2;
			v2 = temp;
		}
	}
}
