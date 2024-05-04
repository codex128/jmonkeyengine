/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef;
import com.jme3.material.logic.DeferredSinglePassLightingLogic;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class DeferredPass extends RenderPass {

    private ResourceTicket<Texture2D> depth, diffuse, specular, emissive, normal, outColor;
    private ResourceTicket<LightList> lights;
    private ResourceTicket<FrameBuffer> frameBuffer;
    private Material material;
    private final CameraSize camSize = new CameraSize();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/ShadingCommon/DeferredShading.j3md");
        for (TechniqueDef t : material.getMaterialDef().getTechniqueDefs("DeferredPass")) {
            t.setLogic(new DeferredSinglePassLightingLogic(t));
        }
    }
    @Override
    protected void prepare(FGRenderContext context) {
        int w = context.getWidth();
        int h = context.getHeight();
        outColor = register(new TextureDef2D(w, h, Image.Format.RGBA8), outColor);
        frameBuffer = register(new FrameBufferDef(w, h, 1), frameBuffer);
        reference(depth, diffuse, specular, emissive, normal);
        referenceOptional(lights);
    }
    @Override
    protected void execute(FGRenderContext context) {
        FrameBuffer fb = resources.acquire(frameBuffer);
        fb.clearColorTargets();
        fb.setDepthTarget((FrameBuffer.FrameBufferTextureTarget)null);
        resources.acquireColorTargets(fb, outColor);
        context.setFrameBuffer(fb, true, true, true);
        material.setTexture("Context_InGBuff0", resources.acquire(diffuse));
        material.setTexture("Context_InGBuff1", resources.acquire(specular));
        material.setTexture("Context_InGBuff2", resources.acquire(emissive));
        material.setTexture("Context_InGBuff3", resources.acquire(normal));
        material.setTexture("Context_InGBuff4", resources.acquire(depth));
        material.selectTechnique("DeferredPass", context.getRenderManager());
        LightList lightList = resources.acquire(lights, null);
        if (lightList != null) {
            context.getScreen().render(context.getRenderManager(), material, lightList);
        } else {
            context.renderFullscreen(material);
        }
        fb.clearColorTargets();
    }
    @Override
    public void reset(FGRenderContext context) {}
    @Override
    public void cleanup(FrameGraph frameGraph) {}

    public void setDepth(ResourceTicket<Texture2D> depth) {
        this.depth = depth;
    }
    public void setDiffuse(ResourceTicket<Texture2D> diffuse) {
        this.diffuse = diffuse;
    }
    public void setSpecular(ResourceTicket<Texture2D> specular) {
        this.specular = specular;
    }
    public void setEmissive(ResourceTicket<Texture2D> emissive) {
        this.emissive = emissive;
    }
    public void setNormal(ResourceTicket<Texture2D> normal) {
        this.normal = normal;
    }
    public void setLights(ResourceTicket<LightList> lights) {
        this.lights = lights;
    }

    public ResourceTicket<Texture2D> getOutColor() {
        return outColor;
    }
    
}
