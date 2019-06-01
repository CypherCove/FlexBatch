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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;

public final class BatchablePreparation {

	/** Generate vertex attributes suitable for multi-texturing and vertex color. 32 bit floats are used for each position
	 * component and texture coordinate. The four color components are packed into a single 32 bit float.
	 * @param textureCount The number of textures to support.
	 * @param position3D Whether the position attribute should include a Z component.
	 * @param textureCoordinates3D Whether the texture coordinate attribute(s) should include a third component.
	 * @param attributes The array to add the vertex attributes to. They are added with position and color in the first two
	 *           available positions, followed by texture coordinates. */
	public static void addBaseAttributes (Array<VertexAttribute> attributes, int textureCount, boolean position3D,
		boolean textureCoordinates3D) {
		attributes.add(new VertexAttribute(Usage.Position, position3D ? 3 : 2, ShaderProgram.POSITION_ATTRIBUTE));
		attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
		for (int i = 0; i < textureCount; i++) {
			attributes.add(
				new VertexAttribute(Usage.TextureCoordinates, textureCoordinates3D ? 3 : 2, ShaderProgram.TEXCOORD_ATTRIBUTE + i, i));
		}
	}

	public static String generateGenericVertexShader (int textureCount) {
		boolean v3 = Gdx.gl30 != null;
		String attribute = v3 ? "in" : "attribute";
		String varying = v3 ? "out" : "varying";

		StringBuilder sb = new StringBuilder();

		if (v3) sb.append("#version 300 es\n");
		sb.append(attribute).append(" vec4 ").append(ShaderProgram.POSITION_ATTRIBUTE).append(";\n");
		sb.append(attribute).append(" vec4 ").append(ShaderProgram.COLOR_ATTRIBUTE).append(";\n");
		for (int i = 0; i < textureCount; i++)
			sb.append(attribute).append(" vec2 ").append(ShaderProgram.TEXCOORD_ATTRIBUTE).append(i).append(";\n");
		sb.append("uniform mat4 u_projTrans;\n");
		sb.append(varying).append(" vec4 v_color;\n");
		for (int i = 0; i < textureCount; i++)
			sb.append(varying).append(" vec2 v_texCoords").append(i).append(";\n\n");

		sb.append("void main()\n");
		sb.append("{\n");
		sb.append("   v_color = ").append(ShaderProgram.COLOR_ATTRIBUTE).append(";\n");
		sb.append("   v_color.a = v_color.a * (255.0/254.0);\n");
		for (int i = 0; i < textureCount; i++)
			sb.append("   v_texCoords").append(i).append(" = ").append(ShaderProgram.TEXCOORD_ATTRIBUTE).append(i).append(";\n");
		sb.append("   gl_Position =  u_projTrans * ").append(ShaderProgram.POSITION_ATTRIBUTE).append(";\n");
		sb.append("}\n");

		return sb.toString();
	}

	public static String generateGenericFragmentShader (int textureCount) { // TODO default should only use first texture
		boolean v3 = Gdx.gl30 != null;
		String varying = v3 ? "in" : "varying";
		String outColor = v3 ? "fragmentColor" : "gl_FragColor";
		String tex2D = v3 ? "texture, " : "texture2D";

		StringBuilder sb = new StringBuilder();

		if (v3) sb.append("#version 300 es\n");
		sb.append("#ifdef GL_ES\n");
		sb.append("#define LOWP lowp\n");
		sb.append("precision mediump float;\n");
		sb.append("#else\n");
		sb.append("#define LOWP \n");
		sb.append("#endif\n\n");

		sb.append(varying).append(" LOWP vec4 v_color;\n");
		for (int i = 0; i < textureCount; i++)
			sb.append(varying).append(" vec2 v_texCoords").append(i).append(";\n");
		for (int i = 0; i < textureCount; i++)
			sb.append("uniform sampler2D u_texture").append(i).append(";\n");
		if (v3) sb.append("out LOWP vec4 ").append(outColor).append("\n");

		sb.append("\n");
		sb.append("void main()\n");
		sb.append("{\n");
		if (textureCount == 0)
			sb.append("  ").append(outColor).append(" = v_color;\n");
		else if (textureCount == 1)
			sb.append("  ").append(outColor).append(" = v_color * texture2D(u_texture0, v_texCoords0);\n");
		else {
			sb.append("LOWP vec4 color = ").append(tex2D).append("(u_texture0, v_texCoords0);\n");
			for (int i = 1; i < textureCount; i++)
				sb.append("color += ").append(tex2D).append("(u_texture").append(i).append(", v_texCoords").append(i).append(");\n");
			sb.append("  ").append(outColor).append(" = v_color * color / ").append(textureCount).append(";\n");
		}
		sb.append("}");

		return sb.toString();
	}

	/** Populates an array of triangle indices for quadrangles made up of two triangles. The indices are ordered such that they
	 * produce counter-clockwise-wound triangles if the vertices are ordered as follows: <code><br>
	 * <br>2--3
	 * <br>| /|
	 * <br>|/ |
	 * <br>1--4
	 * </code>
	 * @param triangles The array that will be filled with triangle indices. */
	public static void populateQuadrangleIndices (short[] triangles) {
		short j = 0;
		for (int i = 0; i + 5 < triangles.length; i += 6, j += 4) {
			triangles[i] = j;
			triangles[i + 1] = (short)(j + 2);
			triangles[i + 2] = (short)(j + 1);
			triangles[i + 3] = j;
			triangles[i + 4] = (short)(j + 3);
			triangles[i + 5] = (short)(j + 2);
		}
	}
}
