/*
 * Copyright (c) 2009-2023 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.framegraph;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;

public class FGRenderTargetSource extends AbstractFGSource {
    
    RenderTargetSourceProxy renderTargetSourceProxy;
    
    public final static class RenderTargetSourceProxy implements FGBindable {
        FrameBuffer.FrameBufferTextureTarget renderTarget;

        public RenderTargetSourceProxy(FrameBuffer.FrameBufferTextureTarget renderTarget) {
            this.renderTarget = renderTarget;
        }

        /**
         * return RT.<br/>
         * @return
         */
        public FrameBuffer.FrameBufferTextureTarget getRenderTarget() {
            return renderTarget;
        }

        /**
         * return RT shaderResource.<br/>
         * @return
         */
        public Texture getShaderResource(){
            return renderTarget.getTexture();
        }
        
        @Override
        public void bind(FGRenderContext renderContext) {}
        
    }
    public FGRenderTargetSource(String name, FrameBuffer.FrameBufferTextureTarget renderTarget) {
        super(name);
        renderTargetSourceProxy = new RenderTargetSourceProxy(renderTarget);
    }

    @Override
    public void postLinkValidate() {

    }

    @Override
    public FGBindable yieldBindable() {
        return renderTargetSourceProxy;
    }
    
}
