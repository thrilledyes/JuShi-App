package com.jushi.demo.main.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

/**
 * 通知消息抓取模块 - 独立测试类
 * 用途：局部单独测试模块功能，不依赖主程序
 * 
 * 使用场景：
 * 1. 模块开发阶段的单元测试
 * 2. 集成到主程序前的功能验证
 * 3. 快速验证筛选规则是否正确
 */
public class NotificationModuleTest {
    private static final String TAG = "NotificationTest";
    private NotificationModuleAPI api;

    public NotificationModuleTest(Context context) {
        this.api = new NotificationModuleAPI(context);
    }

    /**
     * 运行所有测试用例
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void runAllTests() {
        Log.i(TAG, "\n\n==================== 开始执行所有测试 ====================");

        testCaptureLatestMessage();
        testCaptureAllMessages();
        testFilterByCustomRule();
        testFilterWeChatMessage();
        testFilterAICourseMessage();
        testFilterWeChatAICourse();

        Log.i(TAG, "==================== 所有测试执行完毕 ====================\n\n");
    }

    /**
     * 测试1：捕获最新消息
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void testCaptureLatestMessage() {
        Log.i(TAG, "\n【测试1】捕获最新消息");
        Log.i(TAG, "----------------------------------------");

        String result = api.captureLatestMessageAsString();
        Log.i(TAG, "结果:\n" + result);
        Log.i(TAG, "----------------------------------------");
    }

    /**
     * 测试2：捕获所有消息
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void testCaptureAllMessages() {
        Log.i(TAG, "\n【测试2】捕获所有活跃消息");
        Log.i(TAG, "----------------------------------------");

        String result = api.captureAllMessagesAsString();
        Log.i(TAG, "结果:\n" + result);
        Log.i(TAG, "----------------------------------------");
    }

    /**
     * 测试3：自定义规则筛选 - 微信应用
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void testFilterByCustomRule() {
        Log.i(TAG, "\n【测试3】自定义规则筛选");
        Log.i(TAG, "----------------------------------------");

        // 筛选条件：包名 = 微信，群聊 = null（所有群聊）
        String packageName = "com.tencent.mm";
        String groupName = null;

        Log.i(TAG, "筛选条件: 包名=" + packageName + ", 群聊=" + groupName);
        String result = api.filterLatestMessageByRule(packageName, groupName);
        Log.i(TAG, "结果:\n" + result);
        Log.i(TAG, "----------------------------------------");
    }

    /**
     * 测试4：筛选微信消息
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void testFilterWeChatMessage() {
        Log.i(TAG, "\n【测试4】筛选微信消息（所有群聊）");
        Log.i(TAG, "----------------------------------------");

        String result = api.filterWeChatMessage(null);
        Log.i(TAG, "结果:\n" + result);
        Log.i(TAG, "----------------------------------------");
    }

    /**
     * 测试5：筛选AI课程群消息
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void testFilterAICourseMessage() {
        Log.i(TAG, "\n【测试5】筛选AI课程相关消息（微信应用）");
        Log.i(TAG, "----------------------------------------");

        String result = api.filterAICourseGroupMessage();
        Log.i(TAG, "结果:\n" + result);
        Log.i(TAG, "----------------------------------------");
    }

    /**
     * 测试6：筛选微信AI课程群消息（组合筛选）
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void testFilterWeChatAICourse() {
        Log.i(TAG, "\n【测试6】筛选微信AI课程群消息（组合筛选）");
        Log.i(TAG, "----------------------------------------");

        String result = api.filterWeChatAICourseMessage();
        Log.i(TAG, "结果:\n" + result);
        Log.i(TAG, "----------------------------------------");
    }

    /**
     * 高级测试：验证筛选逻辑
     * 捕获所有消息后按规则筛选，验证筛选结果
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void testAdvancedFiltering() {
        Log.i(TAG, "\n【高级测试】验证筛选逻辑");
        Log.i(TAG, "----------------------------------------");

        // 步骤1：获取所有消息
        Log.i(TAG, "步骤1: 获取所有活跃消息");
        java.util.List<MessageModel> allMessages = api.captureAllMessages();
        Log.i(TAG, "总共捕获: " + allMessages.size() + " 条消息");

        // 步骤2：手动筛选微信消息
        Log.i(TAG, "\n步骤2: 筛选微信消息");
        int wechatCount = 0;
        for (MessageModel msg : allMessages) {
            if (msg.getPackageName().contains("com.tencent.mm")) {
                wechatCount++;
                Log.i(TAG, "微信消息: " + msg.getTitle());
            }
        }
        Log.i(TAG, "微信消息总数: " + wechatCount);

        // 步骤3：筛选包含AI关键字的消息
        Log.i(TAG, "\n步骤3: 筛选AI相关消息");
        String[] aiKeywords = {"人工智能", "AI", "AI课程"};
        int aiCount = 0;
        for (MessageModel msg : allMessages) {
            String combined = (msg.getTitle() + msg.getContent() + (msg.getGroupName() != null ? msg.getGroupName() : "")).toLowerCase();
            for (String keyword : aiKeywords) {
                if (combined.contains(keyword.toLowerCase())) {
                    aiCount++;
                    Log.i(TAG, "AI消息: " + msg.getTitle());
                    break;
                }
            }
        }
        Log.i(TAG, "AI消息总数: " + aiCount);

        Log.i(TAG, "----------------------------------------");
    }

    /**
     * 快速测试方法 - 一行代码快速测试
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public String quickTest() {
        return api.filterWeChatAICourseMessage();
    }

    /**
     * 打印帮助信息
     */
    public static void printTestGuide() {
        Log.i(TAG, "\n========== 模块测试指南 ==========");
        Log.i(TAG, "\n【快速开始】");
        Log.i(TAG, "1. 在 Activity 中创建测试对象:");
        Log.i(TAG, "   NotificationModuleTest tester = new NotificationModuleTest(this);");
        Log.i(TAG, "\n2. 运行所有测试:");
        Log.i(TAG, "   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {");
        Log.i(TAG, "       tester.runAllTests();");
        Log.i(TAG, "   }");
        Log.i(TAG, "\n3. 运行单个测试:");
        Log.i(TAG, "   tester.testCaptureLatestMessage();");
        Log.i(TAG, "   tester.testFilterWeChatAICourse();");
        Log.i(TAG, "\n【查看结果】");
        Log.i(TAG, "在 Android Studio Logcat 中搜索标签: NotificationTest");
        Log.i(TAG, "\n【测试用例包括】");
        Log.i(TAG, "✓ testCaptureLatestMessage - 捕获最新消息");
        Log.i(TAG, "✓ testCaptureAllMessages - 捕获所有消息");
        Log.i(TAG, "✓ testFilterByCustomRule - 自定义规则筛选");
        Log.i(TAG, "✓ testFilterWeChatMessage - 微信筛选");
        Log.i(TAG, "✓ testFilterAICourseMessage - AI课程群筛选");
        Log.i(TAG, "✓ testFilterWeChatAICourse - 组合筛选");
        Log.i(TAG, "✓ testAdvancedFiltering - 高级逻辑验证");
        Log.i(TAG, "\n================================\n");
    }
}
