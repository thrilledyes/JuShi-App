package com.jushi.demo.main.utils;

import android.content.Context;

import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.NotificationRule;

import java.util.ArrayList;
import java.util.List;

public final class NotificationRuleStore {
    private NotificationRuleStore() {
    }

    public static List<Rule> loadRules(Context context) {
        List<Rule> rules = new ArrayList<>();
        if (context == null) {
            return rules;
        }

        List<NotificationRule> dbRules = new DBManager(context).getEnabledNotificationRules();
        for (NotificationRule dbRule : dbRules) {
            Rule rule = new Rule(dbRule.packageName, dbRule.groupName);
            if (!rule.packageName.isEmpty() || !rule.groupName.isEmpty()) {
                rules.add(rule);
            }
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
