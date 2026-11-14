package com.example.yandexmusichud.lyrics;

import java.util.Arrays;

public final class AcousticLagEstimator {
    public static final int HOP_MS = 20;

    public record Estimate(double lagMs, double score, double zScore, double sharpness, boolean reliable) {
        public static Estimate none() {
            return new Estimate(0.0, 0.0, 0.0, 0.0, false);
        }
    }

    public record Pcm(float[] samples, int sampleRate) {
    }

    private AcousticLagEstimator() {
    }

    public static Pcm decodeWav(byte[] wav) {
        if (wav == null || wav.length <= 44) {
            return new Pcm(new float[0], 16000);
        }
        int channels = 1;
        int sampleRate = 16000;
        int bits = 16;
        int format = 1;
        int dataStart = -1;
        int dataLen = 0;
        boolean riff = wav[0] == 'R' && wav[1] == 'I' && wav[2] == 'F' && wav[3] == 'F';
        if (riff) {
            int p = 12;
            while (p + 8 <= wav.length) {
                String id = new String(wav, p, 4, java.nio.charset.StandardCharsets.US_ASCII);
                int size = readInt(wav, p + 4);
                int body = p + 8;
                if ("fmt ".equals(id) && body + 16 <= wav.length) {
                    format = readShort(wav, body);
                    channels = Math.max(1, readShort(wav, body + 2));
                    sampleRate = Math.max(1000, readInt(wav, body + 4));
                    bits = readShort(wav, body + 14);
                } else if ("data".equals(id)) {
                    dataStart = body;
                    dataLen = size <= 0 || body + size > wav.length ? wav.length - body : size;
                    break;
                }
                if (size < 0) {
                    break;
                }
                p = body + size + (size & 1);
            }
        }
        if (dataStart < 0) {
            dataStart = 44;
            dataLen = wav.length - 44;
            channels = 1;
            sampleRate = 16000;
            bits = 16;
            format = 1;
        }
        int bytesPerSample = Math.max(1, bits / 8);
        int frameBytes = bytesPerSample * channels;
        int frames = dataLen / frameBytes;
        float[] out = new float[frames];
        for (int f = 0; f < frames; ++f) {
            double acc = 0.0;
            int base = dataStart + f * frameBytes;
            for (int c = 0; c < channels; ++c) {
                int idx = base + c * bytesPerSample;
                double v;
                if (format == 3 && bits == 32) {
                    v = Float.intBitsToFloat(readInt(wav, idx));
                } else if (bits == 16) {
                    v = (short)((wav[idx] & 0xFF) | (wav[idx + 1] & 0xFF) << 8) / 32768.0;
                } else if (bits == 24) {
                    int s = (wav[idx] & 0xFF) | (wav[idx + 1] & 0xFF) << 8 | wav[idx + 2] << 16;
                    v = s / 8388608.0;
                } else if (bits == 32) {
                    v = readInt(wav, idx) / 2147483648.0;
                } else {
                    v = ((wav[idx] & 0xFF) - 128) / 128.0;
                }
                acc += v;
            }
            out[f] = (float)(acc / channels);
        }
        return new Pcm(out, sampleRate);
    }

    private static int readInt(byte[] b, int i) {
        return (b[i] & 0xFF) | (b[i + 1] & 0xFF) << 8 | (b[i + 2] & 0xFF) << 16 | (b[i + 3] & 0xFF) << 24;
    }

    private static int readShort(byte[] b, int i) {
        return (short)((b[i] & 0xFF) | (b[i + 1] & 0xFF) << 8);
    }

    private static float[] biquad(float[] in, double b0, double b1, double b2, double a0, double a1, double a2) {
        float[] out = new float[in.length];
        double x1 = 0.0;
        double x2 = 0.0;
        double y1 = 0.0;
        double y2 = 0.0;
        double nb0 = b0 / a0;
        double nb1 = b1 / a0;
        double nb2 = b2 / a0;
        double na1 = a1 / a0;
        double na2 = a2 / a0;
        for (int i = 0; i < in.length; ++i) {
            double x0 = in[i];
            double y0 = nb0 * x0 + nb1 * x1 + nb2 * x2 - na1 * y1 - na2 * y2;
            out[i] = (float)y0;
            x2 = x1;
            x1 = x0;
            y2 = y1;
            y1 = y0;
        }
        return out;
    }

    private static float[] highPass(float[] in, int sr, double fc) {
        double w = 2.0 * Math.PI * fc / sr;
        double alpha = Math.sin(w) / (2.0 * 0.7071);
        double cos = Math.cos(w);
        return biquad(in, (1.0 + cos) / 2.0, -(1.0 + cos), (1.0 + cos) / 2.0, 1.0 + alpha, -2.0 * cos, 1.0 - alpha);
    }

    private static float[] lowPass(float[] in, int sr, double fc) {
        double w = 2.0 * Math.PI * fc / sr;
        double alpha = Math.sin(w) / (2.0 * 0.7071);
        double cos = Math.cos(w);
        return biquad(in, (1.0 - cos) / 2.0, 1.0 - cos, (1.0 - cos) / 2.0, 1.0 + alpha, -2.0 * cos, 1.0 - alpha);
    }

    public static double[][] audioFeatures(float[] pcm, int sr) {
        int hop = Math.max(1, sr * HOP_MS / 1000);
        int win = hop * 2;
        if (pcm == null || pcm.length < win * 4) {
            return new double[][]{new double[0], new double[0]};
        }
        float[] band = lowPass(highPass(highPass(pcm, sr, 230.0), sr, 230.0), sr, 3600.0);
        int frames = (pcm.length - win) / hop + 1;
        double[] logE = new double[frames];
        double[] sibE = new double[frames];
        double[] voicing = new double[frames];
        int minLag = Math.max(2, sr / 420);
        int maxLag = Math.min(win - 2, sr / 75);
        for (int f = 0; f < frames; ++f) {
            int off = f * hop;
            double e = 0.0;
            double sibSum = 0.0;
            for (int i = 0; i < win; ++i) {
                double v = band[off + i];
                e += v * v;
                double sib = Math.abs(pcm[off + i] - pcm[Math.max(0, off + i - 1)]);
                sibSum += sib;
            }
            logE[f] = Math.log10(e / win + 1.0E-9);
            sibE[f] = Math.log10(sibSum / win + 1.0E-9);
            if (e < 1.0E-7) {
                voicing[f] = 0.0;
                continue;
            }
            double best = 0.0;
            for (int lag = minLag; lag <= maxLag; ++lag) {
                double acc = 0.0;
                double ea = 0.0;
                double eb = 0.0;
                int n = win - lag;
                for (int i = 0; i < n; i += 2) {
                    double a = band[off + i];
                    double b = band[off + i + lag];
                    acc += a * b;
                    ea += a * a;
                    eb += b * b;
                }
                double denom = Math.sqrt(ea * eb);
                if (denom > 1.0E-12) {
                    double r = acc / denom;
                    if (r > best) {
                        best = r;
                    }
                }
            }
            voicing[f] = best;
        }
        double[] sorted = Arrays.copyOf(logE, frames);
        Arrays.sort(sorted);
        double floor = sorted[(int)(frames * 0.12)];
        double ceil = sorted[Math.min(frames - 1, (int)(frames * 0.97))];
        double range = Math.max(0.3, ceil - floor);
        double[] sortedSib = Arrays.copyOf(sibE, frames);
        Arrays.sort(sortedSib);
        double sibFloor = sortedSib[(int)(frames * 0.12)];
        double sibCeil = sortedSib[Math.min(frames - 1, (int)(frames * 0.97))];
        double sibRange = Math.max(0.3, sibCeil - sibFloor);
        double[] activity = new double[frames];
        double[] sibNorm = new double[frames];
        for (int f = 0; f < frames; ++f) {
            double loud = Math.max(0.0, Math.min(1.2, (logE[f] - floor) / range));
            double voiced = Math.max(0.0, Math.min(1.0, (voicing[f] - 0.30) / 0.50));
            activity[f] = loud * (0.25 + 0.75 * voiced);
            sibNorm[f] = Math.max(0.0, Math.min(1.2, (sibE[f] - sibFloor) / sibRange));
        }
        double[] smooth = new double[frames];
        for (int f = 0; f < frames; ++f) {
            double a = activity[Math.max(0, f - 1)];
            double b = activity[f];
            double c = activity[Math.min(frames - 1, f + 1)];
            smooth[f] = 0.25 * a + 0.5 * b + 0.25 * c;
        }
        double[] onset = new double[frames];
        for (int f = 0; f < frames; ++f) {
            double prev = Math.max(smooth[Math.max(0, f - 1)], smooth[Math.max(0, f - 3)]);
            double sibPrev = Math.max(sibNorm[Math.max(0, f - 1)], sibNorm[Math.max(0, f - 3)]);
            double sibOnset = Math.max(0.0, sibNorm[f] - sibPrev);
            onset[f] = Math.max(0.0, smooth[f] - prev) + 0.25 * sibOnset;
        }
        return new double[][]{smooth, onset};
    }

    public static double[][] expectedFeatures(double gridStartMs, int length, double[] eventTimes, double[] eventWeights, double[][] intervals) {
        double[] onset = new double[length];
        double[] activity = new double[length];
        double sigma = 55.0;
        int reach = (int)Math.ceil(sigma * 3.0 / HOP_MS);
        if (eventTimes != null) {
            for (int e = 0; e < eventTimes.length; ++e) {
                double pos = (eventTimes[e] - gridStartMs) / HOP_MS;
                int center = (int)Math.round(pos);
                double w = eventWeights != null && e < eventWeights.length ? eventWeights[e] : 1.0;
                for (int k = center - reach; k <= center + reach; ++k) {
                    if (k < 0 || k >= length) continue;
                    double dt = (k - pos) * HOP_MS;
                    onset[k] += w * Math.exp(-0.5 * dt * dt / (sigma * sigma));
                }
            }
        }
        if (intervals != null) {
            for (double[] iv : intervals) {
                if (iv == null || iv.length < 2) continue;
                int a = (int)Math.floor((iv[0] - gridStartMs) / HOP_MS);
                int b = (int)Math.ceil((iv[1] - gridStartMs) / HOP_MS);
                for (int k = Math.max(0, a); k < Math.min(length, b); ++k) {
                    activity[k] = 1.0;
                }
            }
        }
        double[] blurred = new double[length];
        for (int k = 0; k < length; ++k) {
            double acc = 0.0;
            int cnt = 0;
            for (int d = -2; d <= 2; ++d) {
                int idx = k + d;
                if (idx < 0 || idx >= length) continue;
                acc += activity[idx];
                ++cnt;
            }
            blurred[k] = cnt > 0 ? acc / cnt : 0.0;
        }
        return new double[][]{blurred, onset};
    }

    private static double prepareCentered(double[] a, int n, double[] daOut) {
        double sa = 0.0;
        for (int i = 0; i < n; ++i) {
            sa += a[i];
        }
        double ma = sa / n;
        double va = 0.0;
        for (int i = 0; i < n; ++i) {
            double d = a[i] - ma;
            daOut[i] = d;
            va += d * d;
        }
        if (va < 1.0E-12) {
            return -1.0;
        }
        return va;
    }

    private static double pearsonPrecomputed(double[] da, double va, double[] b, int bOffset, int n) {
        if (va < 1.0E-12) {
            return Double.NaN;
        }
        double sb = 0.0;
        for (int i = 0; i < n; ++i) {
            sb += b[bOffset + i];
        }
        double mb = sb / n;
        double cov = 0.0;
        double vb = 0.0;
        for (int i = 0; i < n; ++i) {
            double db = b[bOffset + i] - mb;
            cov += da[i] * db;
            vb += db * db;
        }
        if (vb < 1.0E-12) {
            return Double.NaN;
        }
        return cov / Math.sqrt(va * vb);
    }

    private static double pearson(double[] a, double[] b, int bOffset, int n) {
        double sa = 0.0;
        double sb = 0.0;
        for (int i = 0; i < n; ++i) {
            sa += a[i];
            sb += b[bOffset + i];
        }
        double ma = sa / n;
        double mb = sb / n;
        double cov = 0.0;
        double va = 0.0;
        double vb = 0.0;
        for (int i = 0; i < n; ++i) {
            double da = a[i] - ma;
            double db = b[bOffset + i] - mb;
            cov += da * db;
            va += da * da;
            vb += db * db;
        }
        if (va < 1.0E-12 || vb < 1.0E-12) {
            return Double.NaN;
        }
        return cov / Math.sqrt(va * vb);
    }

    public static Estimate estimate(float[] pcm, int sr, double[] eventTimes, double[] eventWeights, double[][] intervals, double tauStartMs, int maxLagMs) {
        double[][] audio = audioFeatures(pcm, sr);
        int n = audio[0].length;
        if (n < 60) {
            return Estimate.none();
        }
        int lagFrames = Math.max(1, maxLagMs / HOP_MS);
        int length = n + 2 * lagFrames;
        double gridStart = tauStartMs - (double)lagFrames * HOP_MS;
        double[][] expected = expectedFeatures(gridStart, length, eventTimes, eventWeights, intervals);
        int lags = 2 * lagFrames + 1;
        double[] score = new double[lags];
        int valid = 0;
        double[] estDa0 = null;
        double[] estDa1 = null;
        double estVa0 = -1.0;
        double estVa1 = -1.0;
        if (lags > 0 && n > 0) {
            estDa0 = new double[n];
            estVa0 = prepareCentered(audio[0], n, estDa0);
            estDa1 = new double[n];
            estVa1 = prepareCentered(audio[1], n, estDa1);
        }
        for (int j = 0; j < lags; ++j) {
            double pa = pearsonPrecomputed(estDa0, estVa0, expected[0], j, n);
            double po = pearsonPrecomputed(estDa1, estVa1, expected[1], j, n);
            double s;
            if (Double.isNaN(pa) && Double.isNaN(po)) {
                s = Double.NaN;
            } else if (Double.isNaN(pa)) {
                s = po;
            } else if (Double.isNaN(po)) {
                s = pa;
            } else {
                s = 0.38 * pa + 0.62 * po;
            }
            score[j] = s;
            if (!Double.isNaN(s)) {
                ++valid;
            }
        }
        if (valid < lags / 2) {
            return Estimate.none();
        }
        int best = -1;
        double bestVal = -2.0;
        double sum = 0.0;
        double sumSq = 0.0;
        int cnt = 0;
        for (int j = 0; j < lags; ++j) {
            double s = score[j];
            if (Double.isNaN(s)) continue;
            sum += s;
            sumSq += s * s;
            ++cnt;
            if (s > bestVal) {
                bestVal = s;
                best = j;
            }
        }
        if (best < 0 || cnt < 3) {
            return Estimate.none();
        }
        double mean = sum / cnt;
        double std = Math.sqrt(Math.max(1.0E-12, sumSq / cnt - mean * mean));
        double z = (bestVal - mean) / std;
        double second = -2.0;
        int exclusion = Math.max(1, 220 / HOP_MS);
        for (int j = 0; j < lags; ++j) {
            double s = score[j];
            if (Double.isNaN(s) || Math.abs(j - best) <= exclusion) continue;
            if (s > second) {
                second = s;
            }
        }
        double sharpness = bestVal - second;
        double delta = 0.0;
        if (best > 0 && best < lags - 1 && !Double.isNaN(score[best - 1]) && !Double.isNaN(score[best + 1])) {
            double l = score[best - 1];
            double r = score[best + 1];
            double denom = l - 2.0 * bestVal + r;
            if (Math.abs(denom) > 1.0E-9) {
                delta = Math.max(-0.5, Math.min(0.5, 0.5 * (l - r) / denom));
            }
        }
        double lagMs = (best + delta) * HOP_MS - (double)lagFrames * HOP_MS;
        boolean atEdge = best <= 1 || best >= lags - 2;
        boolean reliable = !atEdge && bestVal >= 0.22 && z >= 2.35 && sharpness >= 0.032;
        return new Estimate(lagMs, bestVal, z, sharpness, reliable);
    }
}
