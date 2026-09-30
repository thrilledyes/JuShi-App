package com.jushi.demo.main.ai;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class UIEPredictor {
    private static final String TAG = "JUSHI_AI";
    private static final int MAX_SEQ_LEN = 128;
    private static final float PROB_THRESHOLD = 0.5f;
    private static final int MAX_SPAN_TOKENS = 48;

    private OrtEnvironment env;
    private OrtSession session;
    private ErnieTokenizer tokenizer;
    private boolean initialized;

    public void init(Context context) {
        try {
            tokenizer = new ErnieTokenizer();
            tokenizer.init(context, "uie_model/vocab.txt");

            env = OrtEnvironment.getEnvironment();
            File modelFile = ensureModelFile(context);
            session = env.createSession(modelFile.getAbsolutePath(), new OrtSession.SessionOptions());
            initialized = true;
            Log.d(TAG, "===== UIE 模型与 Tokenizer 初始化成功！ =====");
        } catch (Throwable e) {
            initialized = false;
            Log.e(TAG, "初始化失败: " + e.getMessage(), e);
        }
    }

    public String extractInfo(String text, String promptEntity) {
        if (!initialized || session == null || tokenizer == null) {
            return "模型未初始化";
        }
        if (text == null || text.trim().isEmpty()) {
            return "未检测到相关内容";
        }

        OnnxTensor inputTensor = null;
        OnnxTensor typeTensor = null;
        OnnxTensor posTensor = null;
        OnnxTensor maskTensor = null;
        OrtSession.Result result = null;
        try {
            ErnieTokenizer.UIEEncoding encoding = tokenizer.encodeForUIEWithOffsets(
                    promptEntity,
                    text,
                    MAX_SEQ_LEN
            );
            long[] inputIds = encoding.tensors[0];
            long[] tokenTypeIds = encoding.tensors[1];
            long[] attentionMask = encoding.tensors[2];

            long[] positionIds = new long[MAX_SEQ_LEN];
            for (int i = 0; i < MAX_SEQ_LEN; i++) {
                positionIds[i] = i;
            }

            inputTensor = OnnxTensor.createTensor(env, new long[][]{inputIds});
            typeTensor = OnnxTensor.createTensor(env, new long[][]{tokenTypeIds});
            posTensor = OnnxTensor.createTensor(env, new long[][]{positionIds});
            maskTensor = OnnxTensor.createTensor(env, new long[][]{attentionMask});

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputTensor);
            inputs.put("token_type_ids", typeTensor);
            inputs.put("position_ids", posTensor);
            inputs.put("attention_mask", maskTensor);

            result = session.run(inputs);
            float[][] startProbs = (float[][]) result.get(0).getValue();
            float[][] endProbs = (float[][]) result.get(1).getValue();
            return decodeResult(text, startProbs[0], endProbs[0], encoding.tokenToChar);
        } catch (Exception e) {
            Log.e(TAG, "抽取异常: " + e.getMessage(), e);
            return null;
        } finally {
            closeQuietly(result);
            closeQuietly(inputTensor);
            closeQuietly(typeTensor);
            closeQuietly(posTensor);
            closeQuietly(maskTensor);
        }
    }

    public void release() {
        try {
            if (session != null) {
                session.close();
                session = null;
            }
            if (env != null) {
                env.close();
                env = null;
            }
            initialized = false;
        } catch (OrtException e) {
            Log.e(TAG, "释放模型失败", e);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    private String decodeResult(String text, float[] startProbs, float[] endProbs, int[] tokenToChar) {
        int bestStartToken = -1;
        int bestEndToken = -1;
        float bestScore = 0f;

        int tokenLimit = Math.min(Math.min(startProbs.length, endProbs.length), tokenToChar.length);
        for (int start = 0; start < tokenLimit; start++) {
            if (tokenToChar[start] < 0 || startProbs[start] < PROB_THRESHOLD) {
                continue;
            }
            int maxEnd = Math.min(tokenLimit - 1, start + MAX_SPAN_TOKENS);
            for (int end = start; end <= maxEnd; end++) {
                if (tokenToChar[end] < 0 || endProbs[end] < PROB_THRESHOLD) {
                    continue;
                }
                float score = startProbs[start] * endProbs[end];
                if (score > bestScore) {
                    bestScore = score;
                    bestStartToken = start;
                    bestEndToken = end;
                }
            }
        }

        if (bestStartToken < 0 || bestEndToken < 0) {
            return "未检测到相关内容";
        }

        int actualStart = tokenToChar[bestStartToken];
        int actualEnd = tokenToChar[bestEndToken];
        if (actualStart < 0 || actualEnd < actualStart || actualEnd >= text.length()) {
            return "提取越界";
        }

        return text.substring(actualStart, actualEnd + 1).trim();
    }

    private static File ensureModelFile(Context context) throws Exception {
        File modelDir = new File(context.getFilesDir(), "uie_model");
        if (!modelDir.exists() && !modelDir.mkdirs()) {
            throw new IllegalStateException("无法创建模型目录: " + modelDir.getAbsolutePath());
        }

        File modelFile = new File(modelDir, "uie_mini.onnx");
        long assetSize = getAssetSize(context);
        if (modelFile.exists() && (assetSize < 0 || modelFile.length() == assetSize)) {
            return modelFile;
        }

        File tempFile = new File(modelDir, "uie_mini.onnx.tmp");
        try (InputStream is = context.getAssets().open("uie_model/uie_mini.onnx");
             FileOutputStream output = new FileOutputStream(tempFile, false)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }

        if (modelFile.exists() && !modelFile.delete()) {
            throw new IllegalStateException("无法替换旧模型文件: " + modelFile.getAbsolutePath());
        }
        if (!tempFile.renameTo(modelFile)) {
            throw new IllegalStateException("无法保存模型文件: " + modelFile.getAbsolutePath());
        }
        return modelFile;
    }

    private static long getAssetSize(Context context) {
        try {
            return context.getAssets().openFd("uie_model/uie_mini.onnx").getLength();
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
