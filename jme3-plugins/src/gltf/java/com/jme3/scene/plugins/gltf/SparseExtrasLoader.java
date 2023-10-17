/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.scene.plugins.gltf;

import com.jme3.plugins.json.JsonElement;
import com.jme3.plugins.json.JsonObject;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author codex
 */
public class SparseExtrasLoader implements ExtrasLoader {

    @Override
    public Object handleExtras(GltfLoader loader, String parentName, JsonElement parent, JsonElement extras, Object input) {
        
        // get info from Json objects
        JsonObject ex = extras.getAsJsonObject();
        int count = GltfUtils.getAsInteger(ex, "count");
        JsonObject indices = ex.getAsJsonObject("indices");
        int iBufferView = GltfUtils.getAsInteger(indices, "bufferView");
        int iByteOffset = GltfUtils.getAsInteger(indices, "byteOffset");
        JsonObject values = ex.getAsJsonObject("values");
        int vBufferView = GltfUtils.getAsInteger(values, "bufferView");
        int vByteOffset = GltfUtils.getAsInteger(values, "byteOffset");
        
        // create index and value buffers
        FloatBuffer indexBuffer = BufferUtils.createFloatBuffer(count);
        FloatBuffer valueBuffer = BufferUtils.createFloatBuffer(count*3);
        
        try {
            // read buffers
            loader.readBuffer(iBufferView, iByteOffset, count, indexBuffer, 1, VertexBuffer.Format.Int);
            loader.readBuffer(vBufferView, vByteOffset, count, valueBuffer, 3, VertexBuffer.Format.Float);
        } catch (IOException e) {
            Logger.getLogger(SparseExtrasLoader.class.getName()).log(Level.SEVERE, "Exception occured while reading buffers", e);
        }
        
        // The index buffer stores the vertices affected by the sparse.
        // The vertex buffer stores the new positions of those vertices.
        
    }
    
}
