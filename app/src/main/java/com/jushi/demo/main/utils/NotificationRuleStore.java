package com.jushi.demo.main.utils;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class NotificationRuleStore {
    private static final String RULE_FILE_NAME = "notification_rules.json";

    private NotificationRuleStore() {
    }

    public static List<Rule> loadRules(Context context) {
        List<Rule> rules = new ArrayList<>();
        if (context == null) {
            return rules;
        }

        File ruleFile = new File(context.getFilesDir(), RULE_FILE_NAME);
        if (!ruleFile.exists()) {
            return rules;
        }

        try (FileInputStream fis = new FileInputStream(ruleFile)) {
            byte[] bytes = new byte[(int) ruleFile.length()];
            int read = fis.read(bytes);
            if (read <= 0) {
                return rules;
            }

            String raw = new String(bytes, 0, read, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(raw);
            JSONArray sources = root.optJSONArray("sources");
            if (sources == null) {
                return rules;
            }

            for (int i = 0; i < sources.length(); i++) {
                JSONObject item = sources.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                Rule rule = new Rule(
                        item.optString("packageName", ""),
                        item.optString("groupName", "")
                );
                if (!rule.packageName.isEmpty() || !rule.groupName.isEmpty()) {
                    rules.add(rule);
                }
            }
        } catch (Exception ignored) {
        }

        return rules;
    }

    public static final class Rule {
        public final String packageName;
        public final String groupName;

        public Rule(String packageName, String groupName) {
            this.packageName = packageName == null ? "" : packageName;
            this.groupName = groupName == null ? "" : groupName;
        }
    }
}
