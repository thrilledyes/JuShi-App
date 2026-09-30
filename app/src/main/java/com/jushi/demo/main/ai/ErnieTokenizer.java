package com.jushi.demo.main.ai;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ErnieTokenizer {
    public static class UIEEncoding {
        public final long[][] tensors;
        public final int[] tokenToChar;

        UIEEncoding(long[][] tensors, int[] tokenToChar) {
            this.tensors = tensors;
            this.tokenToChar = tokenToChar;
        }
    }

    @VisibleForTesting
    Map<String, Integer> vocab = new HashMap<>();

    private int clsId = 1;
    private int sepId = 2;
    @VisibleForTesting
    int unkId = 3;
    private int padId = 0;

    public void init(Context context, String vocabAssetPath) throws Exception {
        InputStream is = context.getAssets().open(vocabAssetPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String line;
        int index = 0;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                vocab.put(line, index);
            }
            index++;
        }
        reader.close();

        clsId = vocab.containsKey("[CLS]") ? vocab.get("[CLS]") : 1;
        sepId = vocab.containsKey("[SEP]") ? vocab.get("[SEP]") : 2;
        unkId = vocab.containsKey("[UNK]") ? vocab.get("[UNK]") : 3;
        padId = vocab.containsKey("[PAD]") ? vocab.get("[PAD]") : 0;
    }

    @VisibleForTesting
    List<Integer> tokenizeSentence(String sentence) {
        List<Integer> tokenIds = new ArrayList<>();
        if (sentence == null || sentence.isEmpty()) return tokenIds;

        String normalized = sentence.toLowerCase();
        for (int i = 0; i < normalized.length(); i++) {
            String charStr = String.valueOf(normalized.charAt(i));
            if (charStr.trim().isEmpty()) {
                continue;
            }
            tokenIds.add(vocab.containsKey(charStr) ? vocab.get(charStr) : unkId);
        }
        return tokenIds;
    }

    public long[][] encodeForUIE(String prompt, String text, int maxLength) {
        return encodeForUIEWithOffsets(prompt, text, maxLength).tensors;
    }

    public UIEEncoding encodeForUIEWithOffsets(String prompt, String text, int maxLength) {
        List<Integer> inputIds = new ArrayList<>();
        List<Integer> tokenTypeIds = new ArrayList<>();
        List<Integer> tokenToChar = new ArrayList<>();

        inputIds.add(clsId);
        tokenTypeIds.add(0);
        tokenToChar.add(-1);

        List<Integer> promptIds = tokenizeSentence(prompt);
        inputIds.addAll(promptIds);
        for (int i = 0; i < promptIds.size(); i++) {
            tokenTypeIds.add(0);
            tokenToChar.add(-1);
        }

        inputIds.add(sepId);
        tokenTypeIds.add(0);
        tokenToChar.add(-1);

        addTextTokens(text, inputIds, tokenTypeIds, tokenToChar);

        inputIds.add(sepId);
        tokenTypeIds.add(1);
        tokenToChar.add(-1);

        int targetLength = Math.min(inputIds.size(), maxLength);
        long[] finalInputIds = new long[maxLength];
        long[] finalTokenTypeIds = new long[maxLength];
        long[] finalAttentionMask = new long[maxLength];
        int[] finalTokenToChar = new int[maxLength];

        for (int i = 0; i < maxLength; i++) {
            if (i < targetLength) {
                finalInputIds[i] = inputIds.get(i);
                finalTokenTypeIds[i] = tokenTypeIds.get(i);
                finalAttentionMask[i] = 1;
                finalTokenToChar[i] = tokenToChar.get(i);
            } else {
                finalInputIds[i] = padId;
                finalTokenTypeIds[i] = 0;
                finalAttentionMask[i] = 0;
                finalTokenToChar[i] = -1;
            }
        }

        return new UIEEncoding(
                new long[][]{finalInputIds, finalTokenTypeIds, finalAttentionMask},
                finalTokenToChar
        );
    }

    private void addTextTokens(
            String text,
            List<Integer> inputIds,
            List<Integer> tokenTypeIds,
            List<Integer> tokenToChar
    ) {
        if (text == null || text.isEmpty()) return;

        String normalized = text.toLowerCase();
        for (int i = 0; i < normalized.length(); i++) {
            String charStr = String.valueOf(normalized.charAt(i));
            if (charStr.trim().isEmpty()) {
                continue;
            }
            inputIds.add(vocab.containsKey(charStr) ? vocab.get(charStr) : unkId);
            tokenTypeIds.add(1);
            tokenToChar.add(i);
        }
    }
}
