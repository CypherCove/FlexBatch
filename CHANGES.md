### Version 1.2.6

### Version 1.2.5
 * Exposed an optional optimization for custom batchables who have vertex attributes that never change
 regardless of what is drawn. For example, quads that always share a full-screen texture use the same
 texture coordinates for every batchable instance, so these don't need to be reapplied for every item
 submitted for drawing. See `Batchable.applyAll()` docs for explanation.

### Version 1.2.3
 * Updated to libGDX 1.9.11
 * Removed nullability annotations to support GWT.
 * **[Breaking]** Removed Quad3D and LitQuad3D constructors that have blending parameters because they falsely imply 
 that blend state is inherent to the instance whereas the `refresh()` method always resets blending to opaque.
 * Added `FlexBatch.requireShader()` to obtain non-null `ShaderProgram` reference. `FlexBatch.getShader()` is now nullable 
 so `CompliantBatch.setShader()` will be considered nullable in Kotlin.
 * Added ability to create `Batchable`s that render as points or lines. Added `Point` and `Point3D` `Batchable`s.
 * **[Breaking]** Added `Batchable.getPrimitiveType()` and `Batchable.getIndicesPerPrimitive()`. For equivalent behavior
 to previous version, return `GL20.GL_TRIANGLES` and `3` respectively.
 * **[Breaking]** Added types to `Quad` and `Poly` that represent the returned type in chain methods. Effectively no change
 to `Poly2D`, `Quad2D`, or `Quad3D`, as they extend the base class with the appropriate types.
 * **[Breaking]** Renamed `FixedSizeBatchable.populateTriangleIndices()` to `populateIndices()`. Renamed
 `FixedSizeBatchable.getTrianglesPerBatchable()` to `FixedSizeBatchable.getPrimitivesPerBatchable()`.
 * **[Breaking]** Moved `Quad3D.Blending` to `.utils.Blending`.
 * **[Breaking]** Removed protected `BatchableSorter.cameraPosition` field.

### Version 1.2.2
 * Updated to libGDX 1.9.10
 * Added Jetbrains nullability annotations for nicer Kotlin interop.
 * **[Breaking]** `SortableBatchable.hasEquivalentTextures()` moved to `Batchable`.
 * **[Breaking]** Rename `AttributesOffset.udpate()` to `AttributesOffset.update()`.
 