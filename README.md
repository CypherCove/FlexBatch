# FlexBatch
FlexBatch is a library for [LibGDX](https://github.com/libgdx/libgdx) used for batching many small custom objects optimally. It can be used as a flexible version of SpriteBatch, but where the sprites can have custom parameters, multi-texturing, bump-mapped lighting, etc. It can also be used for small objects in 3D (for example, a flexible version of DecalBatch).

[![JitPack](https://img.shields.io/badge/JitPack-1.2.4-blue.svg)](https://jitpack.io/#CypherCove/FlexBatch/)
[![libGDX](https://img.shields.io/badge/libgdx-1.9.11-red.svg)](http://www.libgdx.com/)

## Project Dependency
The current version of FlexBatch is available via [JitPack](https://jitpack.io/#CypherCove/FlexBatch/).

    implementation "com.github.CypherCove:FlexBatch:1.2.4"
    
To use with GWT, add this to the `.gwt.xml` file:

    <inherits name="com.cyphercove.flexbatch"/>

See [CHANGES.md](CHANGES.md) for the change log, which lists breaking changes and libGDX version increases.

## Basic Usage
A **FlexBatch** is capable of drawing a specific type of **Batchable**, so it is instantiated using that Batchable. For example, to create a FlexBatch that can draw the included **Quad2D** type of Batchable:

    FlexBatch<Quad2D> quad2dBatch = new FlexBatch<Quad2D>(Quad2D.class, 1000, 0);
    
`0` can be used for the max triangles parameter to indicate that all Batchables drawn with this FlexBatch will be of equal size, which allows for some optimizations. This is only allowed if the Batchable is a descendant of FixedSizeBatchable. Of the included Batchable implementations, the quads are fixed size compatible, and the polys are not.

Unlike LibGDX's SpriteBatch, FlexBatch does not automatically compile its own shader. You must set a shader on it before drawing.

This batch works similarly to LibGDX's SpriteBatch, but does not include draw methods that specify the texture, position, color, etc. Instead, these parameters are specified on the Batchable itself. The included Batchable types are designed for chained method calls. Draw calls must be made between calls to `begin()` and `end()`.

	quad2dBatch.setProjectionMatrix(camera.combined);
	quad2dBatch.begin();
	quad2dBatch.draw().textureRegion(myRegion).position(100, 200).rotation(45f);
	quad2dBatch.end();
	
You can also create many instances of a Batchable and pass that into the batch for drawing. In this case, the Batchables are analogous to LibGDX's Sprite class, in that they store their own parameters for drawing:

	quad2dBatch.setProjectionMatrix(camera.combined);
	quad2dBatch.begin();
	for (Quad2D quad : myQuads) quad2dBatch.draw(quad);
	quad2dBatch.end();
	
Although any Batchable instance can be passed to the FlexBatch, they will not draw correctly if they are not compatible with the type of Batchable defined in the FlexBatch constructor. In order to be compatible the following must be true.

* The passed-in Batchable's vertex attributes must be the same as or a subset of those of the FlexBatch's type. If a subset, it must a subset from the beginning. 
* The `prepareSharedContext()` method of the Batchables must produce equivalent results.
* If the constructor was set up to use a fixed size (by using a FixedSizeBatchable type and passing `0` for the max triangles count), then only FixedSizeBatchables can be drawn.

### CompliantBatch

**CompliantBatch** is a FlexBatch that complies with the LibGDX Batch interface so it can be used with Scene2d, BitmapFont, Sprite, NinePatch, etc. In order to comply, it restricts the Batchable type to a Quad2D or subclass. A subclass with multi-texturing or other additional attributes can still be used.

The flexibility provided by FlexBatch comes with a bit of method call overhead, so if a CompliantBatch is set up with a plain old Quad2D (no multi-texturing or additional attributes), it will use more CPU than a SpriteBatch when drawing equivalent scenes.

Unlike FlexBatch, CompliantBatch can generate its own default shader via constructor parameters. The default shader is equivalent to the one in SpriteBatch.

Although CompliantBatch has a Quad2D batchable type that it returns in its `draw()` method, it is still capable of drawing Poly2Ds by passing them into the `draw(Batchable)` method. You must enable this capability in the constructor.

## Available Batchable Types

Some Batchable implementations are provided in this library.

### Quad2D
**Quad2D** is for drawing rectangular sprites on a 2D plane. If Quad2D is not customized by subclassing, it provides similar functionality as LibGDX's SpriteBatch. Its vertex data include a position, a color, and texture coordinates representing a region of a texture. It passes a texture parameter to the FlexBatch. All its parameters can be set with chained method calls.

	myQuad2D.textureRegion(myRegion).position(100, 200).rotation(45f).scale(1.5f, 1.5f);
	
All its parameters with the exception of Texture/TextureRegion can be set directly on its public members as well.

    myQuad2D.textureRegion(myRegion);
    myQuad2D.x = 100;
    myQuad2D.y = 200;
    myQuad2D.rotation = 45f;
    myQuad2D.scaleX = 1.5f;
    myQuad2D.scaleY = 1.5f;
    
The included parameters are similar to parameters that can be set on a LibGDX Sprite.

Quad2D can be subclassed to easily support multi-texturing by overriding its `getNumberOfTextures()` and supplying a constant. For example:

	public class BumpmappedQuad extends Quad2D {
	    protected int getNumberOfTextures () { return 2; }
	}
	
That's all! To pass the two textures, simply call the `texture()` or `textureRegion()` method twice:

	myBumpQuad.textureRegion(brickRegion).textureRegion(brickNormalMapRegion);
	
A bump-mapped quad probably needs to pass its angle to the shader, so a binormal can be calculated in the shader for proper lighting. For this we need to add a vertex attribute by overriding the `addVertexAttributes()` method and apply the rotation value to the vertex data that's passed to FlexBatch by overriding the `apply()` method.

```
public class BumpmappedQuad extends Quad2D {
    protected int getNumberOfTextures () { return 2; }
    
    protected void addVertexAttributes(Array<VertexAttribute> attributes) {
        super.addVertexAttributes(attributes);
        attributes.add(new VertexAttribute(Usage.Generic, 1, "a_rotation"));
    }
    
    protected int apply(float[] vertices, int vertexStartingIndex, 
            AttributeOffsets offsets, int vertexSize) {
        super.apply(vertices, vertexStartingIndex, offsets, vertexSize);
        
        // Shader functions like radians, so convert
        float rotation = this.rotation * MathUtils.degRad; 
        
        // The AttributeOffsets object provides an easy way to get the array index 
        // of the first vertex's rotation attribute. generic0 corresponds with the 
        // first generic vertex attribute in the Batchable's vertex attributes.
        int rotationIndex = vertexStartingIndex + offsets.generic0; 
        
        // Apply the data to each of the four vertices
        vertices[rotationIndex] = rotation;
        rotationIndex += vertexSize;
        vertices[rotationIndex] = rotation;
        rotationIndex += vertexSize;
        vertices[rotationIndex] = rotation;
        rotationIndex += vertexSize;
        vertices[rotationIndex] = rotation;
        
        return 4; // Quads always have 4 vertices.
    }
}
```	

For an example of a bump mapped quad and matching shader, see the `FlexBatchExamplesMain` class in the examples in the source code.

### Quad3D
**Quad3D** is analogous to a Decal in LibGDX, but it can be customized with additional textures and vertex attributes, much like Quad2D. It is designed for positioning in 3D space relative to the center of the quadrangle. It has some convenience methods for modifying its rotation in relation to a camera.

Its primary difference with Quad2D is that it also has parameters for opaqueness and blend function and causes the FlexBatch to automatically toggle blending and set blend function parameters as appropriate, flushing before changes. This is because non-opaque quads in 3D space must be sorted far-to-near for drawing. Typically, opaque quads should all be drawn before transparent quads.

The BatchableSorter is here to help with this sorting. Quad3Ds can be passed to the sorter, and the sorter can pass the whole group to FlexBatch in optimal order. This is analogous to the functionality of the LibGDX DecalBatch's CameraGroupStrategy.

	for (Quad3D quad : quad3ds) {
		quad.billboard(cam);
		quad3dSorter.add(quad);
	}
	quad3dBatch.setProjectionMatrix(cam.combined);
	quad3dBatch.begin();
	quad3dSorter.flush(quad3dBatch);
	quad3dBatch.end();
	
### Poly2D
**Poly2D** is similar to Quad2D but uses LibGDX PolygonRegions instead of TextureRegions. It is analogous to LibGDX's PolygonSprite. It is not a FixedSizeBatchable, so the FlexBatch constructor must be provided a maximum triangles parameter, and the FlexBatch cannot be optimized for fixed size batchables. A `FlexBatch<Poly2D>` is capable of drawing Quad2Ds, and if you use a subclass to customize Poly2D, its FlexBatch can also draw a Quad2D subclass that was customized in the same way (same number of textures and extra vertex attributes).

A `FlexBatch<Quad2D>` is capable of drawing Poly2D if a non-zero value was provided to the constructor for maximum triangles. If a FlexBatch uses a subclass of Quad2D, it can also draw a Poly2D subclass that was customized in the same way (same number of textures and extra vertex attributes).

## Custom Batchables
You can also write your own subclass of Batchable or FixedSizeBatchable in order to make optimizations over the included classes, or to batch other shapes, such as 3D tetrahedra or 2D  parallelograms. Look over the Javadocs for the Batchable class and browse the Quad source code for hints on how to implement a Batchable class from scratch.

### Philosophy
FlexBatch is meant to provide flexibility to draw almost anything, so it is inherently more complicated than SpriteBatch. To reduce complexity and promote customization, minimal public methods are exposed. This is why Batchable is an abstract class rather than interface, and why the included Batchables use chained method calls for parameters instead of a bunch of overloaded draw methods like SpriteBatch has. Performance is important, but flexibility generally comes first, because FlexBatch's purpose is to do what the highly optimized SpriteBatch cannot do with its limited scope. The included Batchables can be customized and used quickly, leaving open the possibility for a specific, highly optimized implementation to be dropped in by users as needed.
