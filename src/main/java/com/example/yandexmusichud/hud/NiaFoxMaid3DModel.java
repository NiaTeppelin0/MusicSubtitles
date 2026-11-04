package com.example.yandexmusichud.hud;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_7833;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public class NiaFoxMaid3DModel {
    private static float[] HEAD_QUADS;
    private static float[] BODY_QUADS;
    private static float[] TAIL_QUADS;
    private static boolean LOADED;
    private final Vector3f tempPos = new Vector3f();
    private final Vector3f tempNorm = new Vector3f();

    public NiaFoxMaid3DModel() {
        NiaFoxMaid3DModel.ensureLoaded();
    }

    private static synchronized void ensureLoaded() {
        block14: {
            if (LOADED) {
                return;
            }
            try (InputStream raw = NiaFoxMaid3DModel.class.getResourceAsStream("/assets/yandexmusichud/models/nia_foxmaid_mesh.bin");){
                if (raw == null) break block14;
                try (DataInputStream dis = new DataInputStream(new BufferedInputStream(raw));){
                    HEAD_QUADS = NiaFoxMaid3DModel.readQuadArray(dis);
                    BODY_QUADS = NiaFoxMaid3DModel.readQuadArray(dis);
                    TAIL_QUADS = NiaFoxMaid3DModel.readQuadArray(dis);
                    LOADED = true;
                }
            }
            catch (Exception exception) {

            }
        }
    }

    private static float[] readQuadArray(DataInputStream dis) throws Exception {
        int count = dis.readInt();
        float[] arr = new float[count * 23];
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = dis.readFloat();
        }
        return arr;
    }

    public void render(class_4587 matrices, class_4588 buffer, int light, int overlay, int color, float tailSwayAngle, float headBobY, float headTilt) {
        if (!LOADED || buffer == null) {
            return;
        }
        matrices.method_22903();
        if (BODY_QUADS != null) {
            this.renderQuads(matrices.method_23760(), buffer, BODY_QUADS, light, overlay, color);
        }
        if (HEAD_QUADS != null) {
            matrices.method_22903();
            if (headBobY != 0.0f) {
                matrices.method_46416(0.0f, headBobY, 0.0f);
            }
            if (headTilt != 0.0f) {
                matrices.method_22907((Quaternionfc)class_7833.field_40718.rotationDegrees(headTilt));
            }
            this.renderQuads(matrices.method_23760(), buffer, HEAD_QUADS, light, overlay, color);
            matrices.method_22909();
        }
        if (TAIL_QUADS != null) {
            matrices.method_22903();
            matrices.method_46416(0.0f, 9.0f, 1.0f);
            if (tailSwayAngle != 0.0f) {
                matrices.method_22907((Quaternionfc)class_7833.field_40716.rotationDegrees(tailSwayAngle));
            }
            matrices.method_46416(0.0f, -9.0f, -1.0f);
            this.renderQuads(matrices.method_23760(), buffer, TAIL_QUADS, light, overlay, color);
            matrices.method_22909();
        }
        matrices.method_22909();
    }

    private void renderQuads(class_4587.class_4665 entry, class_4588 buffer, float[] quads, int light, int overlay, int color) {
        Matrix4f posMat = entry.method_23761();
        for (int i = 0; i < quads.length; i += 23) {
            float nx = quads[i + 20];
            float ny = quads[i + 21];
            float nz = quads[i + 22];
            Vector3f norm = entry.method_56820(nx, ny, nz, this.tempNorm);
            for (int v = 0; v < 4; ++v) {
                int base = i + v * 5;
                float x = quads[base];
                float y = quads[base + 1];
                float z = quads[base + 2];
                float u = quads[base + 3];
                float vCoord = quads[base + 4];
                Vector3f pos = posMat.transformPosition(x, y, z, this.tempPos);
                buffer.method_22912(pos.x, pos.y, pos.z).method_39415(color).method_22913(u, vCoord).method_22922(overlay).method_60803(light).method_22914(norm.x, norm.y, norm.z);
            }
        }
    }

    static {
        LOADED = false;
    }
}

