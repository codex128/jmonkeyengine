/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.post.filters;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.post.Filter;
import com.jme3.post.filters.BloomFilter.GlowMode;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.Image.Format;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Smoother, but more resource intensive version of BloomFilter.
 * 
 * @author codex
 * @see BloomFilter
 */
public class SmoothBloomFilter extends Filter {
    
    private GlowMode glowMode = GlowMode.Scene;
    private float blurScale = 1f;
    private float exposurePower = 5.0f;
    private float exposureCutOff = 0.0f;
    private float bloomIntensity = 2.0f;
    private float downSamplingFactor = 1;
    private int samples = 5;
    private Pass preGlowPass;
    private Pass extractPass;
    private Pass horizontalBlur = new Pass();
    private Pass verticalBlur = new Pass();
    private Material extractMat;
    private Material vBlurMat;
    private Material hBlurMat;
    private int screenWidth;
    private int screenHeight;    
    private RenderManager renderManager;
    private ViewPort viewPort;

    private AssetManager assetManager;
    private int initialWidth;
    private int initialHeight;
    
    /**
     * Creates a Bloom filter
     */
    public SmoothBloomFilter() {
        super("SmoothBloomFilter");
    }

    /**
     * Creates the bloom filter with the specified glow mode
     *
     * @param glowMode the desired mode (default=Scene)
     */
    public SmoothBloomFilter(GlowMode glowMode) {
        this();
        this.glowMode = glowMode;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
             this.renderManager = renderManager;
        this.viewPort = vp;

        this.assetManager = manager;
        this.initialWidth = w;
        this.initialHeight = h;
                
        screenWidth = (int) Math.max(1, (w / downSamplingFactor));
        screenHeight = (int) Math.max(1, (h / downSamplingFactor));
        //    System.out.println(screenWidth + " " + screenHeight);
        if (glowMode != GlowMode.Scene) {
            preGlowPass = new Pass();
            preGlowPass.init(renderManager.getRenderer(), screenWidth, screenHeight, Format.RGBA8, Format.Depth);
        }

        postRenderPasses = new ArrayList<Pass>();
        //configuring extractPass
        extractMat = new Material(manager, "Common/MatDefs/Post/BloomExtractDepth.j3md");
        extractPass = new Pass() {
            @Override
            public boolean requiresSceneAsTexture() {
                return true;
            }
            @Override
            public boolean requiresDepthAsTexture() {
                return true;
            }
            @Override
            public void beforeRender() {
                extractMat.setFloat("ExposurePow", exposurePower);
                extractMat.setFloat("ExposureCutoff", exposureCutOff);
                if (glowMode != GlowMode.Scene) {
                    extractMat.setTexture("GlowMap", preGlowPass.getRenderedTexture());
                }
                extractMat.setBoolean("Extract", glowMode != GlowMode.Objects);
            }
        };

        extractPass.init(renderManager.getRenderer(), screenWidth, screenHeight, Format.RGBA8, Format.Depth, 1, extractMat);
        postRenderPasses.add(extractPass);

        //configuring horizontal blur pass
        hBlurMat = new Material(manager, "Common/MatDefs/Blur/GaussianBlurSmooth.j3md");
        hBlurMat.setBoolean("Horizontal", true);
        horizontalBlur = new Pass() {
            @Override
            public void beforeRender() {
                hBlurMat.setTexture("Texture", extractPass.getRenderedTexture());
                hBlurMat.setFloat("Size", screenWidth);
                hBlurMat.setFloat("Scale", blurScale);
                hBlurMat.setInt("SampleRadius", samples);
            }
        };

        horizontalBlur.init(renderManager.getRenderer(), screenWidth, screenHeight, Format.RGBA8, Format.Depth, 1, hBlurMat);
        postRenderPasses.add(horizontalBlur);

        //configuring vertical blur pass
        vBlurMat = new Material(manager, "Common/MatDefs/Blur/GaussianBlurSmooth.j3md");
        verticalBlur = new Pass() {
            @Override
            public void beforeRender() {
                vBlurMat.setTexture("Texture", horizontalBlur.getRenderedTexture());
                vBlurMat.setFloat("Size", screenHeight);
                vBlurMat.setFloat("Scale", blurScale);
                vBlurMat.setInt("SampleRadius", samples);
            }
        };

        verticalBlur.init(renderManager.getRenderer(), screenWidth, screenHeight, Format.RGBA8, Format.Depth, 1, vBlurMat);
        postRenderPasses.add(verticalBlur);


        //final material
        material = new Material(manager, "Common/MatDefs/Post/BloomFinal.j3md");
        material.setTexture("BloomTex", verticalBlur.getRenderedTexture());
    }


    protected void reInitFilter() {
        initFilter(assetManager, renderManager, viewPort, initialWidth, initialHeight);
    }
    
    @Override
    protected Material getMaterial() {
        material.setFloat("BloomIntensity", bloomIntensity);
        return material;
    }

    @Override
    protected void postQueue(RenderQueue queue) {
        if (glowMode != GlowMode.Scene) {           
            renderManager.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);            
            renderManager.getRenderer().setFrameBuffer(preGlowPass.getRenderFrameBuffer());
            renderManager.getRenderer().clearBuffers(true, true, true);
            renderManager.setForcedTechnique("Glow");
            renderManager.renderViewPortQueues(viewPort, false);         
            renderManager.setForcedTechnique(null);
            renderManager.getRenderer().setFrameBuffer(viewPort.getOutputFrameBuffer());
        }
    }

    @Override
    protected void cleanUpFilter(Renderer r) {
         if (glowMode != GlowMode.Scene) {   
               preGlowPass.cleanup(r);
         }
    }

    /**
     * returns the bloom intensity
     * @return the intensity value
     */
    public float getBloomIntensity() {
        return bloomIntensity;
    }

    /**
     * intensity of the bloom effect default is 2.0
     *
     * @param bloomIntensity the desired intensity (default=2)
     */
    public void setBloomIntensity(float bloomIntensity) {
        this.bloomIntensity = bloomIntensity;
    }

    /**
     * returns the blur scale
     * @return the blur scale
     */
    public float getBlurScale() {
        return blurScale;
    }

    /**
     * sets The spread of the bloom default is 1.5f
     *
     * @param blurScale the desired scale (default=1.5)
     */
    public void setBlurScale(float blurScale) {
        this.blurScale = blurScale;
    }

    /**
     * returns the exposure cutoff<br>
     * for more details see {@link #setExposureCutOff(float exposureCutOff)}
     * @return the exposure cutoff
     */    
    public float getExposureCutOff() {
        return exposureCutOff;
    }

    /**
     * Define the color threshold on which the bloom will be applied (0.0 to 1.0)
     *
     * @param exposureCutOff the desired threshold (&ge;0, &le;1, default=0)
     */
    public void setExposureCutOff(float exposureCutOff) {
        this.exposureCutOff = exposureCutOff;
    }

    /**
     * returns the exposure power<br>
     * for more details see {@link #setExposurePower(float exposurePower)}
     * @return the exposure power
     */
    public float getExposurePower() {
        return exposurePower;
    }

    /**
     * defines how many times the bloom extracted color will be multiplied by itself. default is 5.0<br>
     * a high value will reduce rough edges in the bloom and somehow the range of the bloom area
     *
     * @param exposurePower the desired exponent (default=5)
     */
    public void setExposurePower(float exposurePower) {
        this.exposurePower = exposurePower;
    }

    /**
     * returns the downSampling factor<br>
     * for more details see {@link #setDownSamplingFactor(float downSamplingFactor)}
     * @return the downsampling factor
     */
    public float getDownSamplingFactor() {
        return downSamplingFactor;
    }

    /**
     * Sets the downSampling factor : the size of the computed texture will be divided by this factor. default is 1 for no downsampling
     * A 2 value is a good way of widening the blur
     *
     * @param downSamplingFactor the desired factor (default=1)
     */
    public void setDownSamplingFactor(float downSamplingFactor) {
        this.downSamplingFactor = downSamplingFactor;
        if (assetManager != null) // dirty isInitialised check
            reInitFilter();
    }
    
    /**
     * Returns the number of samples the blur shaders take.
     * 
     * @return 
     */
    public int getNumSamples() {
        return samples;
    }
    
    /**
     * Sets the number of samples the blur shaders take.
     * <p>
     * Higher values produce larger and better quality blurs, but
     * are much more expensive.
     * <p>
     * default=5
     * 
     * @param samples 
     */
    public void setNumSamples(int samples) {
        this.samples = samples;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(glowMode, "glowMode", GlowMode.Scene);
        oc.write(blurScale, "blurScale", 1.5f);
        oc.write(exposurePower, "exposurePower", 5.0f);
        oc.write(exposureCutOff, "exposureCutOff", 0.0f);
        oc.write(bloomIntensity, "bloomIntensity", 2.0f);
        oc.write(downSamplingFactor, "downSamplingFactor", 1);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        glowMode = ic.readEnum("glowMode", GlowMode.class, GlowMode.Scene);
        blurScale = ic.readFloat("blurScale", 1.5f);
        exposurePower = ic.readFloat("exposurePower", 5.0f);
        exposureCutOff = ic.readFloat("exposureCutOff", 0.0f);
        bloomIntensity = ic.readFloat("bloomIntensity", 2.0f);
        downSamplingFactor = ic.readFloat("downSamplingFactor", 1);
    }
}
