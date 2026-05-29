package com.jushi.demo.main.utils;

import java.util.List;

/**
 * 通知消息筛选器接口和实现
 * 支持按包名、群聊名称等条件筛选消息
 */
public class NotificationFilter {

    /**
     * 筛选器接口
     */
    public interface IMessageFilter {
        /**
         * 判断消息是否符合筛选条件
         */
        boolean match(MessageModel message);
    }

    /**
     * 按包名筛选的实现类
     */
    public static class PackageNameFilter implements IMessageFilter {
        private String targetPackageName;

        public PackageNameFilter(String targetPackageName) {
            this.targetPackageName = targetPackageName;
        }

        @Override
        public boolean match(MessageModel message) {
            if (message == null || message.getPackageName() == null) {
                return false;
            }
            return message.getPackageName().contains(targetPackageName);
        }
    }

    /**
     * 按群聊名称筛选的实现类
     */
    public static class GroupNameFilter implements IMessageFilter {
        private String targetGroupName;

        public GroupNameFilter(String targetGroupName) {
            this.targetGroupName = targetGroupName;
        }

        @Override
        public boolean match(MessageModel message) {
            if (message == null || message.getGroupName() == null) {
                return false;
            }
            return message.getGroupName().contains(targetGroupName);
        }
    }

    /**
     * 复合筛选器（AND逻辑）
     * 消息必须同时满足所有筛选条件
     */
    public static class CompositeAndFilter implements IMessageFilter {
        private List<IMessageFilter> filters;

        public CompositeAndFilter(List<IMessageFilter> filters) {
            this.filters = filters;
        }

        @Override
        public boolean match(MessageModel message) {
            if (filters == null || filters.isEmpty()) {
                return true;
            }
            for (IMessageFilter filter : filters) {
                if (!filter.match(message)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 复合筛选器（OR逻辑）
     * 消息只需满足其中一个筛选条件即可
     */
    public static class CompositeOrFilter implements IMessageFilter {
        private List<IMessageFilter> filters;

        public CompositeOrFilter(List<IMessageFilter> filters) {
            this.filters = filters;
        }

        @Override
        public boolean match(MessageModel message) {
            if (filters == null || filters.isEmpty()) {
                return true;
            }
            for (IMessageFilter filter : filters) {
                if (filter.match(message)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 微信应用筛选器（默认预设）
     */
    public static class WeChatFilter implements IMessageFilter {
        private String targetGroupName;
        public static final String WECHAT_PACKAGE = "com.tencent.mm";

        public WeChatFilter(String targetGroupName) {
            this.targetGroupName = targetGroupName;
        }

        @Override
        public boolean match(MessageModel message) {
            if (message == null) {
                return false;
            }
            // 必须是微信应用
            boolean isWeChat = message.getPackageName() != null &&
                    message.getPackageName().contains(WECHAT_PACKAGE);

            if (!isWeChat) {
                return false;
            }

            // 如果指定了群组名称，则需要匹配群组
            if (targetGroupName != null && !targetGroupName.isEmpty()) {
                String groupName = message.getGroupName();
                return groupName != null && groupName.contains(targetGroupName);
            }

            return true;
        }
    }

    /**
     * AI课程群筛选器（默认预设）
     * 用于筛选包含 "人工智能" 或 "AI" 关键字的群聊消息
     */
    public static class AICourseGroupFilter implements IMessageFilter {
        public static final String WECHAT_PACKAGE = "com.tencent.mm";
        private String[] aiKeywords = {"人工智能", "AI课程", "AI", "人工"};

        @Override
        public boolean match(MessageModel message) {
            if (message == null) {
                return false;
            }

            // 必须是微信应用
            boolean isWeChat = message.getPackageName() != null &&
                    message.getPackageName().contains(WECHAT_PACKAGE);

            if (!isWeChat) {
                return false;
            }

            // 检查群组名称或消息内容中是否包含AI相关关键字
            String groupName = message.getGroupName() != null ? message.getGroupName() : "";
            String title = message.getTitle() != null ? message.getTitle() : "";
            String content = message.getContent() != null ? message.getContent() : "";

            String combined = (groupName + title + content).toLowerCase();

            for (String keyword : aiKeywords) {
                if (combined.contains(keyword.toLowerCase())) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * 自定义规则筛选器
     * 提供用户自定义包名和群聊名称的筛选
     */
    public static class CustomRuleFilter implements IMessageFilter {
        private String targetPackageName;  // 目标应用包名
        private String targetGroupName;    // 目标群聊名称

        public CustomRuleFilter(String targetPackageName, String targetGroupName) {
            this.targetPackageName = targetPackageName;
            this.targetGroupName = targetGroupName;
        }

        @Override
        public boolean match(MessageModel message) {
            if (message == null) {
                return false;
            }

            // 包名筛选
            if (targetPackageName != null && !targetPackageName.isEmpty()) {
                if (!message.getPackageName().contains(targetPackageName)) {
                    return false;
                }
            }

            // 群聊名称筛选
            if (targetGroupName != null && !targetGroupName.isEmpty()) {
                String groupName = message.getGroupName();
                if (groupName == null || !groupName.contains(targetGroupName)) {
                    return false;
                }
            }

            return true;
        }
    }
}
