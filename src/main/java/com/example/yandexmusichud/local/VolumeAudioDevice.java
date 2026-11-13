package com.example.yandexmusichud.local;

import java.lang.reflect.Field;
import javax.sound.sampled.SourceDataLine;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.JavaSoundAudioDevice;

public class VolumeAudioDevice extends JavaSoundAudioDevice {
    private static final Field SOURCE_FIELD;
    private volatile float volume = 1.0f;
    private short[] scratchBuffer = new short[2304];

    static {
        Field f = null;
        try {
            f = JavaSoundAudioDevice.class.getDeclaredField("source");
            f.setAccessible(true);
        } catch (Throwable ignored) {}
        SOURCE_FIELD = f;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(2.0f, volume));
    }

    public float getVolume() {
        return this.volume;
    }

    private SourceDataLine getSourceLine() {
        if (SOURCE_FIELD == null) {
            return null;
        }
        try {
            Object val = SOURCE_FIELD.get(this);
            if (val instanceof SourceDataLine line) {
                return line;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public void pauseLine() {
        SourceDataLine line = this.getSourceLine();
        if (line != null) {
            try {
                line.stop();
            } catch (Exception ignored) {}
        }
    }

    public void resumeLine() {
        SourceDataLine line = this.getSourceLine();
        if (line != null) {
            try {
                line.start();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void writeImpl(short[] samples, int offs, int len) throws JavaLayerException {
        float vol = this.volume;
        if (vol != 1.0f) {
            if (this.scratchBuffer.length < len) {
                this.scratchBuffer = new short[len];
            }
            short[] adjusted = this.scratchBuffer;
            for (int i = 0; i < len; i++) {
                int val = Math.round((float)samples[offs + i] * vol);
                if (val > 32767) {
                    val = 32767;
                } else if (val < -32768) {
                    val = -32768;
                }
                adjusted[i] = (short)val;
            }
            super.writeImpl(adjusted, 0, len);
        } else {
            super.writeImpl(samples, offs, len);
        }
    }
}
