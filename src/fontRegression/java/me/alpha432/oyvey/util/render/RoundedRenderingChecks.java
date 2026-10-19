package me.alpha432.oyvey.util.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import me.alpha432.oyvey.util.render.state.CornerMaskRenderState;
import me.alpha432.oyvey.util.render.state.RoundedOutlineRenderState;
import me.alpha432.oyvey.util.render.state.RoundedRectRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;

/** Explicit local GPU check: compiles the real GLSL and reads its rendered pixels. */
public final class RoundedRenderingChecks {
   private static final int W = 320, H = 160;
   private static int checks;

   public static void main(String[] args) throws Exception {
      require(GLFW.glfwInit(), "GLFW initialized");
      GLFW.glfwDefaultWindowHints();
      GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
      GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
      GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
      GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
      long window = GLFW.glfwCreateWindow(W, H, "SR rounded rendering checks", 0, 0);
      require(window != 0, "hidden OpenGL context created");
      try {
         GLFW.glfwMakeContextCurrent(window);
         GL.createCapabilities();
         // Exercise Minecraft's pipeline construction too, not just standalone
         // GLSL: native GUI pipelines legitimately have no depth/stencil state.
         require(Pipelines.ROUNDED_RECT_PIPELINE.getVertexFormat() == com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            "fill pipeline supplies the shape metadata attributes");
         require(Pipelines.ROUNDED_OUTLINE_PIPELINE.getVertexFormat() == com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            "outline pipeline supplies the shape metadata attributes");
         require(Pipelines.GUI_TARGET_GLOW.getVertexFormat() == net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED.getVertexFormat(),
            "glow is compatible with the native GUI texture vertices");
         require(java.util.Objects.equals(Pipelines.GUI_TARGET_GLOW.getDepthStencilState(),
            net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED.getDepthStencilState()), "glow preserves an absent native GUI depth state");
         require(Pipelines.GUI_TARGET_HALO.getVertexFormat() == Pipelines.GUI_TARGET_GLOW.getVertexFormat(), "halo accepts native texture vertices");
         require(Pipelines.GUI_TARGET_HALO.getSamplers().contains("Sampler0"), "halo has its source texture sampler");
         int fill = program("rounded_rect"), outline = program("rounded_outline");
         int framebuffer = GL33C.glGenFramebuffers(), texture = GL33C.glGenTextures();
         GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, texture);
         GL33C.glTexImage2D(GL33C.GL_TEXTURE_2D, 0, GL33C.GL_RGBA8, W, H, 0,
            GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE, (ByteBuffer)null);
         GL33C.glBindFramebuffer(GL33C.GL_FRAMEBUFFER, framebuffer);
         GL33C.glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0, GL33C.GL_TEXTURE_2D, texture, 0);
         require(GL33C.glCheckFramebufferStatus(GL33C.GL_FRAMEBUFFER) == GL33C.GL_FRAMEBUFFER_COMPLETE, "RGBA framebuffer ready");
         GL33C.glViewport(0, 0, W, H);
         GL33C.glDisable(GL33C.GL_BLEND);
         int vao = GL33C.glGenVertexArrays(), vbo = GL33C.glGenBuffers();
         GL33C.glBindVertexArray(vao);
         GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, vbo);
         GL33C.glVertexAttribPointer(0, 3, GL33C.GL_FLOAT, false, 28, 0);
         GL33C.glVertexAttribPointer(1, 2, GL33C.GL_FLOAT, false, 28, 12);
         GL33C.glVertexAttribPointer(2, 4, GL33C.GL_UNSIGNED_BYTE, true, 28, 20);
         GL33C.glVertexAttribIPointer(3, 2, GL33C.GL_SHORT, 28, 24);
         for (int i = 0; i < 4; i++) GL33C.glEnableVertexAttribArray(i);
         var sheet = new BufferedImage(W * 3, H * 4, BufferedImage.TYPE_INT_ARGB);
         int row = 0;
         double worst = 0;
         for (float scale : new float[]{0.7F, 1, 1.5F, 2.25F}) {
            var pose = new Matrix3x2f().scale(scale);
            for (int column = 0; column < 3; column++) {
               float radius = column == 0 ? 3.5F : 10.25F;
               float thickness = column == 2 ? 0.65F : 0;
               var state = thickness == 0
                  ? RoundedRectRenderState.solid(pose, 16.3F, 12.2F, 110, 28, radius, -1, null, 1)
                  : RoundedOutlineRenderState.create(pose, 16.3F, 12.2F, 110, 28, radius, thickness, -1, null, 1);
               ByteBuffer pixels = render(state, thickness == 0 ? fill : outline);
               worst = Math.max(worst, compare(pixels, scale, radius, thickness));
               copy(pixels, sheet, column * W, row * H);
            }
            row++;
         }
         // Thin fractional-radius borders must leave their center empty.
         var thin = RoundedOutlineRenderState.create(new Matrix3x2f(), 20, 20, 80, 40, 3.5F, 0.65F, -1, null, 1);
         require(alpha(render(thin, outline), 60, 40) == 0, "fractional radius does not inflate outline thickness");
         // A non-integer edge must fade both inside and outside its geometry.
         var edge = RoundedRectRenderState.solid(new Matrix3x2f(), 20.25F, 20.25F, 80, 40, 6, -1, null, 1);
         var pixels = render(edge, fill);
         require(alpha(pixels, 100, 40) > 0 && alpha(pixels, 100, 40) < 255, "outside edge AA is not clipped");
         for (boolean right : new boolean[]{false, true}) for (boolean bottom : new boolean[]{false, true}) {
            pixels = render(CornerMaskRenderState.create(new Matrix3x2f().scale(2), 10, 10, 12, right, bottom, -1, null), fill);
            int outsideX = right ? 43 : 20, outsideY = bottom ? 43 : 20;
            int insideX = right ? 20 : 43, insideY = bottom ? 20 : 43;
            require(alpha(pixels, outsideX, outsideY) == 255, "corner mask covers outside corner");
            require(alpha(pixels, insideX, insideY) == 0, "corner mask leaves circle interior clear");
         }
         targetHalo();
         require(GL33C.glGetError() == GL33C.GL_NO_ERROR, "GPU completed without GL errors");
         Path preview = Path.of("build/font-regression/rounded-gpu.png");
         Files.createDirectories(preview.getParent());
         ImageIO.write(sheet, "PNG", preview.toFile());
         System.out.println("Rounded GPU checks passed: " + checks + "; maximum pixel coverage error: " + worst + "; preview: " + preview);
         GL33C.glDeleteBuffers(vbo); GL33C.glDeleteVertexArrays(vao);
         GL33C.glDeleteTextures(texture); GL33C.glDeleteFramebuffers(framebuffer);
         GL33C.glDeleteProgram(fill); GL33C.glDeleteProgram(outline);
      } finally {
         GLFW.glfwDestroyWindow(window);
         GLFW.glfwTerminate();
      }
   }

   private static void targetHalo() throws Exception {
      int haloProgram = program("target_halo");
      var image = ImageIO.read(Path.of("src/main/resources/assets/oyvey/textures/esp/target.png").toFile());
      int source = GL33C.glGenTextures();
      GL33C.glActiveTexture(GL33C.GL_TEXTURE0);
      GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, source);
      ByteBuffer texels = ByteBuffer.allocateDirect(128 * 128 * 4);
      for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) {
         int pixel = image.getRGB(x, y);
         texels.put((byte)255).put((byte)255).put((byte)255).put((byte)(pixel >>> 24));
      }
      texels.flip();
      GL33C.glTexImage2D(GL33C.GL_TEXTURE_2D, 0, GL33C.GL_RGBA8, 128, 128, 0, GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE, texels);
      GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_LINEAR);
      GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR);
      GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_CLAMP_TO_EDGE);
      GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_CLAMP_TO_EDGE);
      GL33C.glUniform1i(GL33C.glGetUniformLocation(haloProgram, "Sampler0"), 0);
      float[][] corners = {{0,0}, {0,1}, {1,1}, {1,0}};
      ByteBuffer vertices = ByteBuffer.allocateDirect(6 * 28).order(ByteOrder.nativeOrder());
      for (int index : new int[]{0,1,2,0,2,3}) {
         float u = corners[index][0], v = corners[index][1];
         vertices.putFloat(20 + u * 128).putFloat(16 + v * 128).putFloat(0).putFloat(u).putFloat(v);
         vertices.put((byte)170).put((byte)85).put((byte)255).put((byte)255).putShort((short)0).putShort((short)0);
      }
      vertices.flip(); GL33C.glBufferData(GL33C.GL_ARRAY_BUFFER, vertices, GL33C.GL_STREAM_DRAW);
      GL33C.glDisable(GL33C.GL_BLEND);
      GL33C.glClearColor(0,0,0,0); GL33C.glClear(GL33C.GL_COLOR_BUFFER_BIT);
      GL33C.glDrawArrays(GL33C.GL_TRIANGLES, 0, 6);
      ByteBuffer pixels = ByteBuffer.allocateDirect(W * H * 4);
      GL33C.glReadPixels(0,0,W,H,GL33C.GL_RGBA,GL33C.GL_UNSIGNED_BYTE,pixels);
      int haloPixels = 0, corePixels = 0;
      for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) {
         int originalAlpha = image.getRGB(x,y) >>> 24, actual = alpha(pixels,x+20,y+16);
         if (originalAlpha == 0 && actual > 5) haloPixels++;
         if (originalAlpha == 255 && actual > 250) corePixels++;
      }
      require(haloPixels > 500, "reticle glows beyond the original solid outline: " + haloPixels);
      require(corePixels > 300, "reticle keeps a bright crisp core");
      require(alpha(pixels,84,80) == 0, "halo leaves the reticle center transparent");
      GL33C.glEnable(GL33C.GL_BLEND);
      GL33C.glBlendFuncSeparate(GL33C.GL_SRC_ALPHA, GL33C.GL_ONE, GL33C.GL_ZERO, GL33C.GL_ONE);
      GL33C.glClearColor(.04F,.04F,.04F,1); GL33C.glClear(GL33C.GL_COLOR_BUFFER_BIT);
      GL33C.glDrawArrays(GL33C.GL_TRIANGLES,0,6);
      GL33C.glReadPixels(0,0,W,H,GL33C.GL_RGBA,GL33C.GL_UNSIGNED_BYTE,pixels);
      require(alpha(pixels,84,80) == 255, "additive glow preserves destination alpha");
      var preview = new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
      for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
         int i = ((H - 1 - y)*W+x)*4;
         preview.setRGB(x,y,0xFF000000 | (pixels.get(i)&255)<<16 | (pixels.get(i+1)&255)<<8 | pixels.get(i+2)&255);
      }
      Path previewPath = Path.of("build/font-regression/target-esp-glow.png");
      Files.createDirectories(previewPath.getParent());
      ImageIO.write(preview,"PNG",previewPath.toFile());
      GL33C.glDisable(GL33C.GL_BLEND);
      GL33C.glDeleteTextures(source); GL33C.glDeleteProgram(haloProgram);
   }

   private static int program(String name) throws Exception {
      int program = GL33C.glCreateProgram();
      for (String extension : new String[]{"vsh", "fsh"}) {
         String source = Files.readString(Path.of("src/main/resources/assets/oyvey/shaders/core/" + name + "." + extension))
            .replace("#moj_import <minecraft:dynamictransforms.glsl>", "uniform mat4 ModelViewMat;\nuniform vec4 ColorModulator;")
            .replace("#moj_import <minecraft:projection.glsl>", "uniform mat4 ProjMat;");
         int shader = GL33C.glCreateShader(extension.equals("vsh") ? GL33C.GL_VERTEX_SHADER : GL33C.GL_FRAGMENT_SHADER);
         GL33C.glShaderSource(shader, source); GL33C.glCompileShader(shader);
         require(GL33C.glGetShaderi(shader, GL33C.GL_COMPILE_STATUS) != 0, name + "." + extension + ": " + GL33C.glGetShaderInfoLog(shader));
         GL33C.glAttachShader(program, shader); GL33C.glDeleteShader(shader);
      }
      GL33C.glBindAttribLocation(program, 0, "Position"); GL33C.glBindAttribLocation(program, 1, "UV0");
      GL33C.glBindAttribLocation(program, 2, "Color"); GL33C.glBindAttribLocation(program, 3, "UV2");
      GL33C.glLinkProgram(program);
      require(GL33C.glGetProgrami(program, GL33C.GL_LINK_STATUS) != 0, name + " links: " + GL33C.glGetProgramInfoLog(program));
      GL33C.glUseProgram(program);
      GL33C.glUniformMatrix4fv(GL33C.glGetUniformLocation(program, "ProjMat"), false, new Matrix4f().ortho(0, W, H, 0, -1, 1).get(new float[16]));
      GL33C.glUniformMatrix4fv(GL33C.glGetUniformLocation(program, "ModelViewMat"), false, new Matrix4f().get(new float[16]));
      GL33C.glUniform4f(GL33C.glGetUniformLocation(program, "ColorModulator"), 1, 1, 1, 1);
      return program;
   }

   private static ByteBuffer render(GuiElementRenderState state, int program) {
      var vertices = new Capture(); state.buildVertices(vertices);
      require(vertices.vertices.size() == 4, "shape uses one quad");
      ByteBuffer data = ByteBuffer.allocateDirect(6 * 28).order(ByteOrder.nativeOrder());
      for (int index : new int[]{0, 1, 2, 0, 2, 3}) {
         var v = vertices.vertices.get(index);
         data.putFloat(v.x).putFloat(v.y).putFloat(v.z).putFloat(v.u).putFloat(v.v);
         data.put((byte)(v.color >> 16)).put((byte)(v.color >> 8)).put((byte)v.color).put((byte)(v.color >>> 24));
         data.putShort((short)v.radius).putShort((short)v.stroke);
      }
      data.flip(); GL33C.glBufferData(GL33C.GL_ARRAY_BUFFER, data, GL33C.GL_STREAM_DRAW);
      GL33C.glClearColor(0, 0, 0, 0); GL33C.glClear(GL33C.GL_COLOR_BUFFER_BIT);
      GL33C.glUseProgram(program); GL33C.glDrawArrays(GL33C.GL_TRIANGLES, 0, 6);
      ByteBuffer pixels = ByteBuffer.allocateDirect(W * H * 4);
      GL33C.glReadPixels(0, 0, W, H, GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE, pixels);
      return pixels;
   }

   private static double compare(ByteBuffer pixels, float scale, float radius, float stroke) {
      double worst = 0, sum = 0; int edgePixels = 0;
      for (int y = 0; y < (int)(44 * scale + 2); y++) for (int x = 0; x < (int)(130 * scale + 2); x++) {
         int hits = 0;
         // Independent 16x16 area integration, rather than copying the shader's AA.
         for (int sy = 0; sy < 16; sy++) for (int sx = 0; sx < 16; sx++) {
            double px = (x + (sx + 0.5) / 16) / scale - 16.3 - 55;
            double py = (y + (sy + 0.5) / 16) / scale - 12.2 - 14;
            double qx = Math.abs(px) - (55 - radius), qy = Math.abs(py) - (14 - radius);
            double d = Math.hypot(Math.max(qx, 0), Math.max(qy, 0)) + Math.min(Math.max(qx, qy), 0) - radius;
            if (d <= 0 && (stroke == 0 || d >= -stroke)) hits++;
         }
         double expected = hits / 256.0, actual = alpha(pixels, x, y) / 255.0;
         double error = Math.abs(expected - actual); worst = Math.max(worst, error);
         if (expected > 0 && expected < 1 || actual > 0 && actual < 1) { sum += error; edgePixels++; }
      }
      require(edgePixels > 0, "curves contain antialiased pixels at scale " + scale);
      require(worst < 0.22, "pixel coverage is close to area integration: " + worst);
      require(sum / edgePixels < 0.04, "average edge coverage error: " + sum / edgePixels);
      return worst;
   }

   private static int alpha(ByteBuffer pixels, int x, int y) { return pixels.get(((H - 1 - y) * W + x) * 4 + 3) & 255; }
   private static void copy(ByteBuffer pixels, BufferedImage image, int ox, int oy) {
      for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
         int a = alpha(pixels, x, y), background = 26;
         int value = (255 * a + background * (255 - a)) / 255;
         image.setRGB(ox + x, oy + y, 0xFF000000 | value << 16 | value << 8 | value);
      }
   }
   private static void require(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }

   private static final class V { float x, y, z, u, v; int color, radius, stroke; }
   private static final class Capture implements VertexConsumer {
      final ArrayList<V> vertices = new ArrayList<>(); V current;
      @Override public VertexConsumer addVertex(float x, float y, float z) { current = new V(); current.x=x; current.y=y; current.z=z; vertices.add(current); return this; }
      @Override public VertexConsumer setColor(int r, int g, int b, int a) { return setColor(a << 24 | r << 16 | g << 8 | b); }
      @Override public VertexConsumer setColor(int color) { current.color=color; return this; }
      @Override public VertexConsumer setUv(float u, float v) { current.u=u; current.v=v; return this; }
      @Override public VertexConsumer setUv2(int radius, int stroke) { current.radius=radius; current.stroke=stroke; return this; }
      @Override public VertexConsumer setUv1(int u, int v) { return this; }
      @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
      @Override public VertexConsumer setLineWidth(float width) { return this; }
   }
}
