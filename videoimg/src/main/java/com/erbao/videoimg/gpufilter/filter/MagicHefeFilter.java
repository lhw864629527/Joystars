package com.erbao.videoimg.gpufilter.filter;

import android.opengl.GLES20;

import com.erbao.videoimg.R;

import com.erbao.videoimg.gpufilter.basefilter.GPUImageFilter;
import com.erbao.videoimg.gpufilter.utils.OpenGlUtils;
import com.erbao.videoimg.videimg_utils.VideoimgInitialize;


public class MagicHefeFilter extends GPUImageFilter {
	private int[] inputTextureHandles = {-1,-1,-1,-1};
	private int[] inputTextureUniformLocations = {-1,-1,-1,-1};
    private int mGLStrengthLocation;

	public MagicHefeFilter(){
		super(NO_FILTER_VERTEX_SHADER, OpenGlUtils.readShaderFromRawResource(R.raw.hefe));
	}
	
	protected void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteTextures(inputTextureHandles.length, inputTextureHandles, 0);
        for(int i = 0; i < inputTextureHandles.length; i++)
        	inputTextureHandles[i] = -1;
    }
	
	protected void onDrawArraysAfter(){
		for(int i = 0; i < inputTextureHandles.length
				&& inputTextureHandles[i] != OpenGlUtils.NO_TEXTURE; i++){
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i+3));
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		}
	}
	  
	protected void onDrawArraysPre(){
		for(int i = 0; i < inputTextureHandles.length 
				&& inputTextureHandles[i] != OpenGlUtils.NO_TEXTURE; i++){
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i+3) );
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureHandles[i]);
			GLES20.glUniform1i(inputTextureUniformLocations[i], (i+3));
		}
	}
	
	protected void onInit(){
		super.onInit();
		for(int i=0; i < inputTextureUniformLocations.length; i++)
			inputTextureUniformLocations[i] = GLES20.glGetUniformLocation(getProgram(), "inputImageTexture"+(2+i));
		mGLStrengthLocation = GLES20.glGetUniformLocation(mGLProgId,
				"strength");
	}
	
	protected void onInitialized(){
		super.onInitialized();
		setFloat(mGLStrengthLocation, 1.0f);
	    runOnDraw(new Runnable(){
		    public void run(){
		    	inputTextureHandles[0] = OpenGlUtils.loadTexture(VideoimgInitialize.getContext(), "filter/edgeburn.png");
				inputTextureHandles[1] = OpenGlUtils.loadTexture(VideoimgInitialize.getContext(), "filter/hefemap.png");
				inputTextureHandles[2] = OpenGlUtils.loadTexture(VideoimgInitialize.getContext(), "filter/hefemetal.png");
				inputTextureHandles[3] = OpenGlUtils.loadTexture(VideoimgInitialize.getContext(), "filter/hefesoftlight.png");
		    }
	    });
	}
}
