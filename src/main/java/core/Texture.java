package core;

import org.lwjgl.system.MemoryStack;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

public class Texture {
    private int textureId;
    private boolean loaded;

    public Texture(String resourcePath){
        this.loaded = false;
        this.textureId = createPlaceholder();
        loadImage(resourcePath);
    }

    // builds a texture straight from pixels you already have - flips vertically,
    // correct for normal display images (BufferedImage is top-down, GL is bottom-up)
    public Texture(BufferedImage image){
        this(image, true);
    }

    // flipVertically=false for DATA textures (e.g. particle position encoding) -
    // there's no "up/down" to a data texture, and your own indexing math
    // (row = i / side) assumes the row order is untouched. Flipping here would
    // silently swap which row each particle's encoded bytes end up in.
    public Texture(BufferedImage image, boolean flipVertically){
        this.loaded = false;
        this.textureId = createPlaceholder();
        loadFromBufferedImage(image, flipVertically);
    }

    private int createPlaceholder() {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        try (MemoryStack stack = stackPush()) {
            ByteBuffer pixel = stack.malloc(4);
            pixel.put((byte) 160).put((byte) 160).put((byte) 160).put((byte) 255);
            pixel.flip();
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
        }

        return id;
    }

    private boolean isPowerOfTwo(int value){
        return(value&(value-1))==0;
    }

    private void loadImage(String resourcePath){
        ByteBuffer fileBuffer = readResourceToBuffer(resourcePath);
        if (fileBuffer == null) return;

        try (MemoryStack stack = stackPush()){
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            stbi_set_flip_vertically_on_load(true);
            ByteBuffer image = stbi_load_from_memory(fileBuffer, w, h, channels, 4); 
            memFree(fileBuffer);
            if(image ==null){
                System.err.println("Failed to load texture: "+resourcePath+" - "+stbi_failure_reason());
                return;
            }
            int width = w.get(0);
            int height = h.get(0);

            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,width,height,0,GL_RGBA, GL_UNSIGNED_BYTE,image);

            boolean powerOfTwo = isPowerOfTwo(width) && isPowerOfTwo(height);
            if (powerOfTwo) {
                glGenerateMipmap(GL_TEXTURE_2D);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            } else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            }
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            stbi_image_free(image);
            this.loaded=true;
        }
    }

    private void loadFromBufferedImage(BufferedImage image, boolean flipVertically) {
        int width = image.getWidth();
        int height = image.getHeight();

        ByteBuffer buffer = memAlloc(width * height * 4);
        for (int row = 0; row < height; row++) {
            // flipVertically=true: same convention stbi_set_flip_vertically_on_load gives loadImage().
            // flipVertically=false: preserve row order exactly, for data textures.
            int y = flipVertically ? (height - 1 - row) : row;
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                buffer.put((byte) ((argb >> 16) & 0xFF)); // R
                buffer.put((byte) ((argb >> 8) & 0xFF));  // G
                buffer.put((byte) (argb & 0xFF));         // B
                buffer.put((byte) ((argb >> 24) & 0xFF)); // A
            }
        }
        buffer.flip();

        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        memFree(buffer);
        this.loaded = true;
    }

    private ByteBuffer readResourceToBuffer(String resourcePath){
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if(in==null){
                System.err.println("Texture resource not found: "+resourcePath);
                return null;
            }
            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getTextureId(){return textureId;}
    public boolean isLoaded(){return loaded;}
}