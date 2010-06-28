package com.stickycoding.rokon;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

import com.stickycoding.rokon.vbo.ArrayVBO;
import com.stickycoding.rokon.vbo.VBO;

/**
 * Texture.java
 * An object representing one texture (one image file)
 * Contains information linking the actual asset file for the texture
 * 
 * @author Richard
 */

public class Texture {
	
	protected TextureAtlas parentAtlas;
	protected int atlasX, atlasY;
	protected Bitmap bmp;
	
	
	protected int textureWidth, textureHeight;
	protected int width, height, columns, rows, tileCount;
	protected String path;
	protected BufferObject[] buffer;
	protected int textureIndex = -1;
	
	protected ArrayVBO[] vbo;
	
	protected void setUnloaded() {
		textureIndex = -1;
	}
	
	public int getTextureIndex() {
		return textureIndex;
	}
	
	public BufferObject[] getBuffer() {
		return buffer;
	}
	
	public BufferObject getBuffer(int index) {
		return buffer[index];
	}
	
	/**
	 * Creates a texture, with a file from the assets
	 * 
	 * @param filename path in assets, relative to RokonActivity.getGraphicsPath
	 */
	public Texture(String filename) {
		this(filename, 1, 1);
	}
	
	protected Texture() {

	}
	
	/**
	 * Creates a texture, with a file from the assets
	 * 
	 * @param filename path in assets, relative to RokonActivity.getGraphicsPath
	 * @param columns number of columns in the texture
	 * @param rows number of rows in the texture
	 */
	public Texture(String filename, int columns, int rows) {
		path = RokonActivity.graphicsPath + filename;
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		try {
			BitmapFactory.decodeStream(Rokon.currentActivity.getAssets().open(path), null, opts);
		} catch (Exception e) {
			Debug.error("Tried creating a Texture, failed while decoding, " + path);
			return;
		}
		width = opts.outWidth;
		height = opts.outHeight;
		this.columns = columns;
		this.rows = rows;
		tileCount = columns * rows;
		textureWidth = nextPowerOfTwo(width);
		textureHeight = nextPowerOfTwo(height);
	}
	
	protected void prepareBuffers() {
		if(parentAtlas == null) {
			buffer = new BufferObject[tileCount];
			for(int i = 0; i < buffer.length; i++) {
				float col = i % columns;
				float row = (i - col) / (float)columns;
				buffer[i] = new BufferObject(8);
				float x = col * (float)(width / columns);
				float y = row * (float)(height / rows);
				buffer[i].update(x / textureWidth, y / textureHeight, (float)(width / columns) / (float)textureWidth, (float)(height / rows) / (float)textureHeight);
			}
		} else {
			buffer = new BufferObject[tileCount];
			for(int i = 0; i < buffer.length; i++) {
				float col = i % columns;
				float row = (i - col) / (float)columns;
				buffer[i] = new BufferObject(8);
				float x = col * (float)(width / columns);
				float y = row * (float)(height / rows);
				
				float finalX = x / textureWidth;
				float finalY = y / textureHeight;
				float finalWidth = (float)(width / columns) / (float)textureWidth;
				float finalHeight = (float)(height / rows) / (float)textureHeight;
				
				float realX = (float)atlasX / (float)parentAtlas.atlasWidth;
				float realY = (float)atlasY / (float)parentAtlas.atlasHeight;
				float realWidth = (float)textureWidth / (float)parentAtlas.atlasWidth;
				float realHeight = (float)textureHeight / (float)parentAtlas.atlasHeight;
				
				float theX = realX + (finalX * realWidth);
				float theY = realY + (finalY * realHeight);
				float theWidth = finalWidth * realWidth;
				float theHeight = finalHeight * realHeight;

				buffer[i].update(theX, theY, theWidth, theHeight);
			}
		}
		if(DrawPriority.drawPriority == DrawPriority.PRIORITY_VBO) {
			vbo = new ArrayVBO[buffer.length];
			for(int i = 0; i < buffer.length; i++) {
				vbo[i] = new ArrayVBO(buffer[i], VBO.STATIC);
			}
		}
	}
	
	protected void freeBuffers() {
		for(int i = 0; i < buffer.length; i++) {
			buffer[i].free();
		}
	}
	
	/**
	 * @param x integer
	 * @return TRUE if x is a power of two, FALSE otherwise
	 */
	public static boolean isPowerOfTwo(int x) {
		return (x != 0) && ((x & (x - 1)) == 0);
	}
	
	/**
	 * Finds the next power of two, from a given minimum
	 * 
	 * @param minimum integer
	 * @return the next (or same, if minimum is power-of-two) power-of-two
	 */
	public static int nextPowerOfTwo(int minimum) {
		if(isPowerOfTwo(minimum)) {
			return minimum;
		}
		int i = 0;
		while(true) {
			i++;
			if(Math.pow(2, i) >= minimum) {
				return (int)Math.pow(2, i);
			}
		}
	}
	
	protected Bitmap getBitmap() {
        try {
        	bmp = BitmapFactory.decodeStream(Rokon.currentActivity.getAssets().open(path));
        	return bmp;
        } catch (Exception e) {
        	Debug.error("Texture.getBitmap() error, bad asset?");
        	return null;
        }
	}
	
	protected void onLoadTexture(GL10 gl) {
		if(parentAtlas == null) {
			prepareBuffers();
			int[] nameArray = new int[1];
			GLHelper.enableTextures();
			gl.glGenTextures(1, nameArray, 0);
			textureIndex = nameArray[0];
			GLHelper.bindTexture(textureIndex);
			Bitmap bmp = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
	        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
	        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);	
	        if(tileCount == 1) {
	            gl.glTexSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buffer[0].get());
	        } else {
	            BufferObject bigBuffer;
	            
	        	bigBuffer = new BufferObject(new float[] { 0, 0, (float)(width / textureWidth), (float)(height / (float)textureHeight) });
				gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bigBuffer.get());
		        
		        bigBuffer.free();
		        bigBuffer = null;
	        }
	        bmp.recycle();
	        bmp = getBitmap();
	        GLUtils.texSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, bmp);
	        clearBitmap();
	        bmp = null;
		} else {
			parentAtlas.onLoadTexture(gl);
		}
	}
	
	protected void clearBitmap() {
		if(bmp == null) return;
		bmp.recycle();
		bmp = null;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getTileWidth() {
		return width / columns;
	}
	
	public int getTileHeight() {
		return height / rows;
	}
	
	public float getRatio() {
		return (float)height / (float)width / columns;
	}
	
	public float getTileRatio() {
		return ((float)height / rows) / ((float)width / columns);
	}
	
	public float getTileHeight(float width) {
		return (width / columns) * getRatio();
	}
	
	public float getHeight(float width) {
		return width * getRatio();
	}

}
