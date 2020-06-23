/*******************************************************************************
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

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.utils.ObjectIntMap;
import org.jetbrains.annotations.NotNull;

/** Provides fast and convenient access to VertexAttribute offsets, in float-size units.
 * 
 * @author cypherdare */
public class AttributeOffsets {

	public final @NotNull VertexAttributes attributes;
	private final ObjectIntMap<String> byAlias;
	private final int[] byIndex;
	public final int position, color0, color1, color2, color3, textureCoordinate0, textureCoordinate1, textureCoordinate2,
		textureCoordinate3, generic0, generic1, generic2, generic3, normal, tangent, biNormal;

	public AttributeOffsets (VertexAttributes attributes) {
		this.attributes = attributes;
		byAlias = new ObjectIntMap<String>(attributes.size());
		byIndex = new int[attributes.size()];
		int cIdx = 0, tcIdx = 0, gIdx = 0;
		int color0 = 0, color1 = 0, color2 = 0, color3 = 0, textureCoordinate0 = 0, textureCoordinate1 = 0, textureCoordinate2 = 0,
			textureCoordinate3 = 0, generic0 = 0, generic1 = 0, generic2 = 0, generic3 = 0;
		for (int i = 0; i < byIndex.length; i++) {
			VertexAttribute attribute = attributes.get(i);
			int offset = attribute.offset / 4;
			byAlias.put(attribute.alias, offset);
			byIndex[i] = offset;

			switch (attribute.usage) {
			case Usage.ColorPacked:
			case Usage.ColorUnpacked:
				switch (cIdx++) {
				case 0:
					color0 = offset;
					break;
				case 1:
					color1 = offset;
					break;
				case 2:
					color2 = offset;
					break;
				case 3:
					color3 = offset;
					break;
				}
				break;
			case Usage.TextureCoordinates:
				switch (tcIdx++) {
				case 0:
					textureCoordinate0 = offset;
					break;
				case 1:
					textureCoordinate1 = offset;
					break;
				case 2:
					textureCoordinate2 = offset;
					break;
				case 3:
					textureCoordinate3 = offset;
					break;
				}
				break;
			case Usage.Generic:
				switch (gIdx++) {
				case 0:
					generic0 = offset;
					break;
				case 1:
					generic1 = offset;
					break;
				case 2:
					generic2 = offset;
					break;
				case 3:
					generic3 = offset;
					break;
				}
				break;
			}
		}

		position = attributes.getOffset(Usage.Position);
		this.color0 = color0;
		this.color1 = color1;
		this.color2 = color2;
		this.color3 = color3;
		this.textureCoordinate0 = textureCoordinate0;
		this.textureCoordinate1 = textureCoordinate1;
		this.textureCoordinate2 = textureCoordinate2;
		this.textureCoordinate3 = textureCoordinate3;
		this.generic0 = generic0;
		this.generic1 = generic1;
		this.generic2 = generic2;
		this.generic3 = generic3;
		normal = attributes.getOffset(Usage.Normal);
		tangent = attributes.getOffset(Usage.Tangent);
		biNormal = attributes.getOffset(Usage.BiNormal);

	}

	/** Refreshes the mapping of aliases to offsets. Call if any {@link VertexAttribute#alias alias} has been changed. */
	public void update () {
		byAlias.clear();
		for (VertexAttribute attribute : attributes)
			byAlias.put(attribute.alias, attribute.offset / 4);
	}

	/**
	 * Get the VertexAttribute offset, in float-size units, looking it up by its {@link VertexAttribute#alias alias}.
	 *
	 * @param alias The VertexAttribute alias used to find the attribute offset.
	 * @return The offset, or -1 if the value is not found.
	 * */
	public int get (String alias) {
		return byAlias.get(alias, -1);
	}

	/**
	 * Get the VertexAttribute offset, in float-size units, looking it up by its index in the VertexAttributes.
	 *
	 * @param attributeIndex The index of the VertexAttribute to retrieve.
	 * @return The offset.
	 * @throws IndexOutOfBoundsException If the given attribute index does not exist in the VertexAttributes. */
	public int get (int attributeIndex) {
		return byIndex[attributeIndex];
	}
}
