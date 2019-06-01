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
package com.cyphercove.flexbatch;

import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ShortBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Mesh.VertexDataType;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.cyphercove.flexbatch.batchable.Quad3D;
import com.cyphercove.flexbatch.Batchable.FixedSizeBatchable;
import com.cyphercove.flexbatch.utils.AttributeOffsets;
import com.cyphercove.flexbatch.utils.RenderContextAccumulator;

/**
 * Draws batched {@link Batchable} objects, optimizing the drawing process by combining them into a single Mesh. FlexBatch can be
 * customized to batch almost any kind of small object by defining how to draw the object in a Batchable implementation. Most of
 * the available Batchable types in this library are designed for subclassing, to minimize the work needed to define a Batchable.
 * A FlexBatch is instantiated using a Batchable class that defines the types of Batchables it can draw. A FlexBatch must be
 * {@link #dispose() disposed of} when no longer used to avoid leaking memory.
 * <p>
 * Batchables can only be drawn between calls to {@link #begin()} and {@link #end()}. The current queue of Batchables can be
 * flushed to screen immediately with a call to {@link #flush()}. Flushes also occur automatically when {@link #end()} is called
 * or if any relevant OpenGL state changes or drawing changes are made (such as drawing a Batchable with different Texture(s) than
 * the last, changing blend function parameters, changing the shader used, or changing the projection matrix).
 * <p>
 * Batchable objects can be passed to the {@link #draw(Batchable)} method to queue them for drawing. Alternatively, the
 * {@link #draw()} method can be called to acquire a Batchable matching the class passed to the constructor parameter. This
 * Batchable can be immediately customized for drawing, and will be automatically queued for drawing on the next call to any
 * FlexBatch method.
 * <p>
 * A single FlexBatch might be capable of drawing more than one type of Batchable. See {@link #draw(Batchable)} for details.
 * <p>
 * Unlike SpriteBatch, no default shader is provided automatically. A call to {@link #setShader(ShaderProgram)} must be made
 * before any drawing can occur. The attribute names to be used in the vertex shader are defined by the Batchable. By default
 * FlexBatch automatically passes its combined transform and projection matrices to the shader with the uniform name
 * {@code "u_projTrans"}. This can be customized by overriding {@link #applyMatrices()}. By default, FlexBatch automatically
 * passes texture uniforms to the shader, with a count defined by the template Batchable class. The texture uniforms are given the
 * names {@code "u_texture0"}, {@code "u_texture1"}, etc., but this can be customized by overriding
 * {@link #applyTextureUniforms()}.
 * <p>
 * Other uniforms can be manually applied to the shader in between calls to {@link #begin()} and {@link #end()}. Take care of when
 * these changes are made in relation to batch flushes, as only the most recently set uniforms are used when a flush occurs.
 * <p>
 * <i>This API is based on SpriteBatch and NewSpriteBatch from the LibGDX project.</i>
 *
 * @param <T> The type of Batchable that is returned when acquiring one with {@link #draw()}. This must match the class type that
 *            is passed to the constructor.
 * @author cypherdare
 */
public class FlexBatch<T extends Batchable> implements Disposable {

    public final Class<T> batchableType;
    private T internalBatchable;
    private boolean havePendingInternal;
    private final Mesh mesh;
    private final AttributeOffsets attributeOffsets;
    private final float[] vertices;
    private final short[] triangles;
    private int vertIdx, triIdx;
    private int previousTriIdx;
    private boolean reflushUsed;
    private int unfixedVertCount; // only for non-fixed indices, for
    // optimization
    private final int maxVertices, vertexSize, maxIndices;
    private final boolean fixedIndices;

    // only for fixedIndices
    private final int indicesPerBatchable, verticesPerBatchable, vertexDataPerBatchable;

    private boolean drawing = false;

    /**
     * Number of render calls since the last {@link #begin()}.
     **/
    public int renderCalls = 0;
    private boolean flushCalled;
    /**
     * Number of rendering calls, ever. Will not be reset unless set manually.
     **/
    public int totalRenderCalls = 0;

    private final Matrix4 transformMatrix = new Matrix4();
    private final Matrix4 projectionMatrix = new Matrix4();
    private final Matrix4 combinedMatrix = new Matrix4();

    private String[] textureUnitUniforms;

    private RenderContextAccumulator renderContext;

    private ShaderProgram shader;

    /**
     * Construct a FlexBatch capable of drawing the given Batchable type and other compatible Batchables (ones with the same + *
     * VertexAttributes or subset of beginning VertexAttributes). The FlexBatch will not be limited to FixedSizeBatchables.
     *
     * @param batchableType The type of Batchable that defines the VertexAttributes supported by this FlexBatch, and the + *
     *                      default Batchable type drawn by the {@link #draw()} method.
     * @param maxVertices   The number of vertices this FlexBatch can batch at once. Maximum of 32767. If the Batchable is a
     *                      FixedSizeBatchable and 0 is used for maxTriangles, this value will be rounded down to a multiple of the
     *                      Batchable's size.
     * @param maxTriangles  The number of triangles this FlexBatch can batch at once, or 0 to optimize this FlexBatch to draw only
     *                      FixedSizeBatchables.
     */
    public FlexBatch (Class<T> batchableType, int maxVertices, int maxTriangles) {
        // 32767 is max vertex index.
        if (maxVertices > 32767)
            throw new IllegalArgumentException("Can't have more than 32767 vertices per batch: " + maxTriangles);
        if (Modifier.isAbstract(batchableType.getModifiers()))
            throw new IllegalArgumentException("Can't use an abstract batchableType");

        this.batchableType = batchableType;

        try {
            internalBatchable = batchableType.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Batchable classes must be public and have an empty constructor.", e);
        }

        Array<VertexAttribute> attributesArray = new Array<VertexAttribute>(true, 10, VertexAttribute.class);
        internalBatchable.addVertexAttributes(attributesArray);
        VertexAttributes vertexAttributes = new VertexAttributes(attributesArray.toArray());
        attributeOffsets = new AttributeOffsets(vertexAttributes);
        vertexSize = vertexAttributes.vertexSize / 4;
        final int vertexArraySize = vertexSize * maxVertices;
        vertices = new float[vertexArraySize];
        fixedIndices = internalBatchable instanceof FixedSizeBatchable && maxTriangles == 0;

        if (fixedIndices) {
            FixedSizeBatchable fixedSizeBatchable = (FixedSizeBatchable) internalBatchable;
            verticesPerBatchable = fixedSizeBatchable.getVerticesPerBatchable();
            vertexDataPerBatchable = verticesPerBatchable * vertexSize;
            this.maxVertices = maxVertices - (maxVertices % verticesPerBatchable);
            this.maxIndices = (this.maxVertices / verticesPerBatchable) * fixedSizeBatchable.getTrianglesPerBatchable() * 3;
            indicesPerBatchable = fixedSizeBatchable.getTrianglesPerBatchable() * 3;
            triangles = new short[maxIndices];
            fixedSizeBatchable.populateTriangleIndices(triangles);
        } else {
            if (maxTriangles == 0) throw new IllegalArgumentException(
                    "maxTriangles must be greater than 0 if batchableType is not a FixedSizeBatchable");
            this.maxVertices = maxVertices;
            maxIndices = maxTriangles * 3;
            triangles = new short[maxIndices];
            indicesPerBatchable = verticesPerBatchable = vertexDataPerBatchable = 0;
        }

        Mesh.VertexDataType vertexDataType = Gdx.gl30 != null ? VertexDataType.VertexBufferObjectWithVAO
                : Mesh.VertexDataType.VertexArray;
        mesh = new Mesh(vertexDataType, false, this.maxVertices, maxIndices, attributesArray.toArray());
        if (fixedIndices) mesh.setIndices(triangles);

        textureUnitUniforms = new String[internalBatchable.getNumberOfTextures()];
        for (int i = 0; i < textureUnitUniforms.length; i++) {
            ;
            textureUnitUniforms[i] = "u_texture" + i;
        }

        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        renderContext = new RenderContextAccumulator();
        renderContext.setBlending(true);
        renderContext.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void begin () {
        if (drawing) throw new IllegalStateException("end() must be called before begin().");
        renderCalls = 0;

        renderContext.begin();
        internalBatchable.prepareSharedContext(renderContext);
        renderContext.executeChanges();
        shader.begin();
        applyMatrices();

        drawing = true;
        flushCalled = false;
        reflushUsed = false;
    }

    public void end () {
        if (!drawing) throw new IllegalStateException("begin() must be called before end().");
        if (!reflushUsed && !flushCalled) // didn't reflush and didn't draw anything, so next reflush should use 0 count.
            previousTriIdx = 0;
        flush();
        drawing = false;

        renderContext.end();

        // Avoid hanging onto native resource object references
        renderContext.clearAllTextureUnits();
        internalBatchable.reset();

        shader.end();
    }

    /**
     * Returns the render context, which can be used to modify drawing parameters for the next batch flush. Changes to
     * the render parameters will override any that have been set by batchables that are queued and awaiting the next
     * flush, with the exception of the last batchable drawn with {@link #draw()}, if there is one. Subsequent batchables
     * might reverse these changes in their {@link Batchable#prepareContext(RenderContextAccumulator, int, int)} methods,
     * and therefore trigger a flush before they are queued. For example, {@link Quad3D}
     * manages blending parameters per batchable, and therefore setting blend parameters on the RenderContextAccumulator
     * will have no effect.
     *
     * @return the render context, whose parameters may be set to affect drawing.
     */
    public RenderContextAccumulator getRenderContext () {
        return renderContext;
    }

    private void drawPending () {
        havePendingInternal = false;
        draw(internalBatchable);
    }

    /**
     * @return A Batchable that will automatically be queued for drawing upon the next call to draw(), flush(), or end(). The
     * Batchable will be of the same type as the {@link #batchableType} of this FlexBatch.
     * <p>
     * Do not cache and reuse the returned Batchable.
     */
    public T draw () {
        if (havePendingInternal) drawPending();
        havePendingInternal = true;
        internalBatchable.refresh();
        return internalBatchable;
    }

    /**
     * Queues a Batchable for drawing. Certain characteristics of the supplied Batchable class must match those of the
     * {@link #batchableType} of this FlexBatch:
     * <ul>
     * <li>Vertex attributes must be the same or be a beginning subset of the FlexBatch's type.
     * <li>{@link Batchable#prepareSharedContext(RenderContextAccumulator)} must perform equivalent changes.
     * <li>This FlexBatch may have been set up to draw only FixedSizeBatchables in the constructor.
     * </ul>
     * The above criteria are not checked. If the supplied Batchable class's {@link VertexAttributes}) do not match those of the
     * {@link #batchableType}, a different shader may be needed for this Batchable, and some vertex data will go unused. The docs
     * for the built-in Batchables explain their compatibility.
     *
     * @param batchable
     */
    public void draw (Batchable batchable) {
        if (havePendingInternal) drawPending();
        if (!drawing) throw new IllegalStateException("begin() must be called before drawing.");
        if (fixedIndices) {
            if (batchable.prepareContext(renderContext, maxVertices - vertIdx / vertexSize, 0)) flush();
            batchable.apply(vertices, vertIdx, attributeOffsets, vertexSize);
            triIdx += indicesPerBatchable;
            vertIdx += vertexDataPerBatchable;
        } else {
            if (batchable.prepareContext(renderContext, maxVertices - unfixedVertCount, maxIndices - triIdx)) flush();
            triIdx += batchable.apply(triangles, triIdx, (short) unfixedVertCount);
            int verticesAdded = batchable.apply(vertices, vertIdx, attributeOffsets, vertexSize);
            unfixedVertCount += verticesAdded;
            vertIdx += vertexSize * verticesAdded;
        }
    }

    /**
     * Draws explicit vertex data, using only the render context and Texture parameter(s) of the passed in FixedSizeBatchable. The
     * restrictions on the supplied Batchable class are the same as those in {@link #draw(Batchable)}.
     *
     * @param batchable        A Batchable that defines the render context and textures to draw, but not the actual vertex data.
     * @param explicitVertices Pre-computed vertex data that is large enough for the Batchable type.
     * @param offset           Starting index of the data in the array.
     * @param count            The number of array elements to pass. This must be a multiple of the size of the FixedSizeBatchable, and is not
     *                         checked.
     * @param vertexSize       The size of the vertices in the data (from the {@link VertexAttributes}. Must be the same or smaller than
     *                         the size of the vertices for the Batchable type. If smaller, the data for the excess vertex attributes will be
     *                         garbage, but this may be acceptable if the current shader doesn't use them. It is assumed that the
     *                         VertexAttributes being drawn match the first of the VertexAttributes of the Batchable type.
     */
    protected void draw (FixedSizeBatchable batchable, float[] explicitVertices, int offset, int count, int vertexSize) {
        if (havePendingInternal) drawPending();
        if (!drawing) throw new IllegalStateException("begin() must be called before drawing.");
        if (batchable.prepareContext(renderContext, maxVertices - vertIdx / vertexSize, 0)) {
            flush();
        }

        int verticesLength = vertices.length;
        int remainingVertices = verticesLength - vertIdx;
        // room for at least one Batchable size is assured by prepareContext()
        // call above

        if (this.vertexSize == vertexSize) {
            int copyCount = Math.min(remainingVertices, count);
            System.arraycopy(explicitVertices, offset, vertices, vertIdx, copyCount);
            vertIdx += copyCount;
            if (fixedIndices)
                triIdx += (copyCount / this.vertexDataPerBatchable) * this.indicesPerBatchable;
            else {
                int verticesPerBatchable = batchable.getVerticesPerBatchable();
                for (int i = 0, n = copyCount / (verticesPerBatchable * vertexSize); i < n; i++) {
                    int indicesAdded = batchable.apply(triangles, triIdx, (short) unfixedVertCount);
                    triIdx += indicesAdded;
                    unfixedVertCount += verticesPerBatchable;
                }
            }
            count -= copyCount;
            while (count > 0) {
                offset += copyCount;
                flush();
                copyCount = Math.min(verticesLength, count);
                System.arraycopy(explicitVertices, offset, vertices, vertIdx, copyCount);
                vertIdx += copyCount;
                if (fixedIndices)
                    triIdx += (copyCount / this.vertexDataPerBatchable) * this.indicesPerBatchable;
                else {
                    int verticesPerBatchable = batchable.getVerticesPerBatchable();
                    for (int i = 0, n = copyCount / (verticesPerBatchable * vertexSize); i < n; i++) {
                        int indicesAdded = batchable.apply(triangles, triIdx, (short) unfixedVertCount);
                        triIdx += indicesAdded;
                        unfixedVertCount += verticesPerBatchable;
                    }
                }
                count -= copyCount;
            }
        } else {
            int dstCount = (count / vertexSize) * this.vertexSize;
            int dstCopyCount = Math.min(remainingVertices, dstCount);
            int vertexCount = dstCopyCount / this.vertexSize;
            for (int i = 0; i < vertexCount; i++) {
                System.arraycopy(explicitVertices, offset, vertices, vertIdx, vertexSize);
                vertIdx += this.vertexSize;
                offset += vertexSize;
            }
            if (fixedIndices)
                triIdx += (vertexCount * this.indicesPerBatchable) / this.verticesPerBatchable;
            else {
                int verticesPerBatchable = batchable.getVerticesPerBatchable();
                for (int i = 0, n = vertexCount / verticesPerBatchable; i < n; i++) {
                    int indicesAdded = batchable.apply(triangles, triIdx, (short) unfixedVertCount);
                    triIdx += indicesAdded;
                    unfixedVertCount += verticesPerBatchable;
                }
            }
            dstCount -= dstCopyCount;
            while (dstCount > 0) {
                flush();
                dstCopyCount = Math.min(verticesLength, dstCount);
                vertexCount = dstCopyCount / this.vertexSize;
                for (int i = 0; i < vertexCount; i++) {
                    System.arraycopy(explicitVertices, offset, vertices, vertIdx, vertexSize);
                    vertIdx += this.vertexSize;
                    offset += vertexSize;
                }
                if (fixedIndices)
                    triIdx += (vertexCount * this.indicesPerBatchable) / this.verticesPerBatchable;
                else {
                    int verticesPerBatchable = batchable.getVerticesPerBatchable();
                    for (int i = 0, n = vertexCount / verticesPerBatchable; i < n; i++) {
                        int indicesAdded = batchable.apply(triangles, triIdx, (short) unfixedVertCount);
                        triIdx += indicesAdded;
                        unfixedVertCount += verticesPerBatchable;
                    }
                }
                dstCount -= dstCopyCount;
            }
        }
    }

    /**
     * Draws explicit vertex data, using only the render context and Texture parameter(s) of the passed-in Batchable. This must
     * only be called if this FlexBatch is not limited to drawing FixedSizedBatchables. The restrictions on the supplied Batchable
     * class are the same as those in {@link #draw(Batchable)}.
     * <p>
     * The batch must have enough total capacity for the entire set of vertices and triangles. This is not checked.
     *
     * @param batchable         A Batchable that defines the render context and textures to draw, but not the actual vertex data.
     * @param explicitVertices  Pre-computed vertex data that is large enough for the Batchable type.
     * @param verticesOffset    Starting index of the data in the array.
     * @param vertexDataCount   The number of array elements to pass. This must be the correct size for the Batchable type, and is
     *                          not checked.
     * @param vertexSize        The size of the vertices in the data (from the {@link VertexAttributes}. Must be the same or smaller than
     *                          the size of the vertices for the Batchable type. If smaller, the data for the excess vertex attributes will be
     *                          garbage, but this may be acceptable if the current shader doesn't use them. It is assumed that the
     *                          VertexAttributes being drawn match the first of the VertexAttributes of the Batchable type.
     * @param explicitTriangles Vertex index data for the drawn item, starting from index 0.
     * @param trianglesOffset   Starting index of the data in the array.
     * @param trianglesCount    The number of array elements to pass. Must be a multiple of 3.
     */
    protected void draw (Batchable batchable, float[] explicitVertices, int verticesOffset, int vertexDataCount, int vertexSize,
                         short[] explicitTriangles, int trianglesOffset, int trianglesCount) {
        if (havePendingInternal) drawPending();
        if (!drawing) throw new IllegalStateException("begin() must be called before drawing.");
        if (fixedIndices)
            throw new UnsupportedOperationException("This method can only be used for Batchables without fixed size");
        if (batchable.prepareContext(renderContext, maxVertices - unfixedVertCount, maxIndices - triIdx)) {
            flush();
        }

        int verticesLength = vertices.length;
        int trianglesLength = triangles.length;
        final int vertexCount = vertexDataCount / vertexSize;
        if (verticesLength - vertIdx < vertexCount * this.vertexSize || trianglesLength - triIdx < trianglesCount)
            flush();

        System.arraycopy(explicitTriangles, trianglesOffset, triangles, triIdx, trianglesCount);
        final short startingVertex = (short) unfixedVertCount;
        final int upTo = triIdx + trianglesCount;
        for (int i = triIdx; i < upTo; i++)
            triangles[i] += startingVertex;
        triIdx += trianglesCount;

        if (this.vertexSize == vertexSize) {
            System.arraycopy(explicitVertices, verticesOffset, vertices, vertIdx, vertexDataCount);
            vertIdx += vertexDataCount;
        } else {
            for (int i = 0; i < vertexCount; i++) {
                System.arraycopy(explicitVertices, verticesOffset, vertices, vertIdx, vertexSize);
                vertIdx += this.vertexSize;
                verticesOffset += vertexSize;
            }
        }

        unfixedVertCount += vertexCount;
    }

    public void flush () {
        if (havePendingInternal) drawPending();
        flushCalled = true;
        if (vertIdx == 0) {
            if (drawing) renderContext.executeChanges(); // first item
            return;
        }

        Mesh mesh = this.mesh;
        mesh.setVertices(vertices, 0, vertIdx);
        if (fixedIndices) {
            Buffer buffer = mesh.getIndicesBuffer(); //cast to buffer so JDK 9+ compiler doesn't compile Java 7/8-incompatible method signature
            buffer.position(0);
            buffer.limit(triIdx);
        } else {
            mesh.setIndices(triangles, 0, triIdx);
        }

        mesh.render(shader, GL20.GL_TRIANGLES, 0, triIdx);

        renderContext.executeChanges(); // might have flushed for new item

        previousTriIdx = triIdx;
        vertIdx = 0;
        unfixedVertCount = 0;
        triIdx = 0;
        renderCalls++;
        totalRenderCalls++;
    }

    /** Flushes the same triangles drawn the last time a flush occurred. This is an expert method with no error checks. It
     * provides a way to avoid re-submitting the batchable data for drawing if none of them have changed since the last
     * flush and nothing in the render context has changed either (textures, blend modes, etc.). Note that there are
     * several reasons the previous flush may have occurred other than a manual call to {@link #flush()}, so care must
     * be taken to be aware of these and avoid them if necessary.
     */
    public void repeatPreviousFlush (){
        if (previousTriIdx != 0) {
            mesh.render(shader, GL20.GL_TRIANGLES, 0, previousTriIdx);
            renderCalls++;
            totalRenderCalls++;
        }
        reflushUsed = true;
    }

    /** Skips ahead in draw submissions by the specified count of FixedSizeBatchables, which effectively resubmits the
     * the same batchables from the previous flush with no modifications to render context (textures, blend modes, etc.),
     * while avoiding all the overheads of calculating and copying vertex data. This is an expert method with no error
     * checks. Note that there are several reasons the previous flush may have occurred other than a manual call to
     * {@link #flush()}, so care must be taken to be aware of these and avoid them if necessary.
     * @param count The number of FixedSizeBatchables to assume are unchanged from the previous flush.
     */
    public void redraw (int count){
        triIdx += count * indicesPerBatchable;
        vertIdx += count * vertexDataPerBatchable;
    }

    /** Behaves like calling {@link #redraw(int)} with a count of 1. */
    public void redraw (){
        triIdx += indicesPerBatchable;
        vertIdx += vertexDataPerBatchable;
    }

    public ShaderProgram getShader () {
        return shader;
    }

    public void setShader (ShaderProgram shader) {
        if (drawing) {
            flush();
            shader.end();
        }
        this.shader = shader;
        if (drawing) {
            shader.begin();
            applyMatrices();
        }
    }

    public void disableBlending () {
        if (!renderContext.isBlendingEnabled()) return;
        flush();
        renderContext.setBlending(false);
    }

    public void enableBlending () {
        if (renderContext.isBlendingEnabled()) return;
        flush();
        renderContext.setBlending(true);
    }

    public void setBlendFunction (int srcFunc, int dstFunc) {
        RenderContextAccumulator renderContext = this.renderContext;
        if (!renderContext.isBlendFuncSeparate() && renderContext.getBlendFuncSrcColor() == srcFunc
                && renderContext.getBlendFuncDstColor() == dstFunc) return;

        flush();
        renderContext.setBlendFunction(srcFunc, dstFunc);
    }

    public void setBlendFunctionSeparate (int srcColorFunc, int dstColorFunc, int srcAlphaFunc, int dstAlphaFunc) {
        if (renderContext.getBlendFuncSrcColor() == srcColorFunc && renderContext.getBlendFuncDstColor() == dstColorFunc
                && renderContext.getBlendFuncSrcAlpha() == srcAlphaFunc && renderContext.getBlendFuncDstAlpha() == dstAlphaFunc)
            return;
        flush();
        renderContext.setBlendFunction(srcColorFunc, dstColorFunc, srcAlphaFunc, dstAlphaFunc);
    }

    public int getBlendSrcFunc () {
        return renderContext.getBlendFuncSrcColor();
    }

    public int getBlendDstFunc () {
        return renderContext.getBlendFuncDstColor();
    }

    public int getBlendSrcFuncAlpha () {
        return renderContext.getBlendFuncSrcAlpha();
    }

    public int getBlendDstFuncAlpha () {
        return renderContext.getBlendFuncDstAlpha();
    }

    public boolean isBlendingEnabled () {
        return renderContext.isBlendingEnabled();
    }

    public Matrix4 getProjectionMatrix () {
        return projectionMatrix;
    }

    public Matrix4 getTransformMatrix () {
        return transformMatrix;
    }

    public void setProjectionMatrix (Matrix4 projection) {
        if (drawing) flush();
        projectionMatrix.set(projection);
        if (drawing) applyMatrices();
    }

    public void setTransformMatrix (Matrix4 transform) {
        if (drawing) flush();
        transformMatrix.set(transform);
        if (drawing) applyMatrices();
    }

    /**
     * Called only while drawing. Recalculates the matrices and sets their values to shader uniforms. The default implementation
     * combines the projection and transform matrices and sets them to a single uniform named "u_projTrans".
     */
    protected void applyMatrices () {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);
        getShader().setUniformMatrix("u_projTrans", combinedMatrix);
    }

    /**
     * Sets shader uniform values for the textures. The default implementation uses the uniform name "u_texture" with the texture
     * unit appended. For example, if the Batchable type supports two textures, uniforms will be set for "u_texture0" and
     * "u_texture1".
     */
    protected void applyTextureUniforms () {
        for (int i = 0; i < textureUnitUniforms.length; i++)
            getShader().setUniformi(textureUnitUniforms[i], i);
    }

    /**
     * @return Whether this batch is between {@link #begin()} and {@link #end()} calls.
     */
    public boolean isDrawing () {
        return drawing;
    }

    /**
     * @return The number of vertices that can be drawn between flushes.
     */
    public int getVertexCapacity (){
        return vertices.length / vertexSize;
    }

    /**
     * @return The number of batchables that can be drawn between flushes, or 0 if the batchable is not a {@link FixedSizeBatchable}.
     */
    public int getBatchableCapacity (){
        if (fixedIndices)
            return vertices.length / (vertexSize * verticesPerBatchable);
        return 0;
    }

    public void dispose () {
        mesh.dispose();
    }
}
