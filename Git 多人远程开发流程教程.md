# Git 多人远程开发流程教程

面向初学者的 Git 团队协作实操指南，照着一步步走即可完成完整开发流程。

---

## 1. 环境配置

### 安装 Git 后设置用户信息

```bash
git config --global user.name "你的名字"
git config --global user.email "你的邮箱@example.com"
```

### 生成 SSH Key 并添加到远程仓库

```bash
# 生成密钥（一路回车即可）
ssh-keygen -t ed25519 -C "你的邮箱@example.com"

# 查看公钥，复制输出内容
cat ~/.ssh/id_ed25519.pub
```

将公钥内容粘贴到 GitHub / GitLab 的 **Settings -> SSH Keys** 中，然后测试连接：

```bash
ssh -T git@github.com
```

---

## 2. 克隆远程仓库到本地

```bash
# 使用 SSH 地址克隆（推荐）
git clone git@github.com:用户名/仓库名.git

# 进入项目目录
cd 仓库名
```

---

## 3. 创建开发分支

开发前从主分支切出新分支，避免直接在 `main` 上改动。

```bash
# 确保在 main 分支且代码最新
git checkout main
git pull origin main

# 创建并切换到新分支
git checkout -b feature/你的功能名
```

**分支命名建议：** `feature/xxx`（新功能）、`fix/xxx`（修复）、`hotfix/xxx`（紧急修复）

---

## 4. 本地开发与提交

### 查看改动状态

```bash
git status
git diff
```

### 添加并提交

```bash
# 添加指定文件
git add 文件名1 文件名2

# 或添加所有改动
git add .

# 提交（写清楚做了什么）
git commit -m "feat: 添加用户登录功能"
```

**Commit 消息规范：** `feat:` 新功能、`fix:` 修复、`docs:` 文档、`refactor:` 重构、`style:` 格式调整

---

## 5. 推送到远程仓库

```bash
# 首次推送新分支，需设置上游追踪
git push -u origin feature/你的功能名

# 后续推送直接
git push
```

---

## 6. 发起 Pull Request 与合并

1. 在 GitHub / GitLab 页面上点击 **New Pull Request**
2. 选择源分支（你的开发分支）和目标分支（通常是 `main`）
3. 填写标题和说明，描述本次改动内容
4. 等待团队成员 Code Review
5. 审核通过后点击 **Merge Pull Request**

合并后删除远程已合并的分支（平台通常会提示），本地也清理一下：

```bash
git checkout main
git pull origin main
git branch -d feature/你的功能名
```

---

## 7. 同步远程最新代码

### 方式一：拉取并合并（pull）

```bash
git checkout main
git pull origin main
```

### 方式二：变基（rebase，保持提交历史整洁）

```bash
# 在开发分支上，将 main 的最新提交变基到当前分支
git checkout feature/你的功能名
git fetch origin
git rebase origin/main
```

如果 rebase 过程中有冲突，解决后继续：

```bash
git add .
git rebase --continue
```

---

## 8. 常见问题速查

### 解决合并冲突

当 `git pull` 或 `git merge` 提示冲突时：

```bash
# 1. 查看哪些文件有冲突
git status

# 2. 打开冲突文件，找到 <<<<<<< HEAD 标记，手动修改保留正确内容

# 3. 标记冲突已解决并提交
git add .
git commit -m "fix: 解决合并冲突"
```

### 撤销工作区的修改（未 add）

```bash
# 撤销单个文件
git checkout -- 文件名

# 撤销所有修改
git checkout -- .
```

### 撤销已 add 但未 commit 的文件

```bash
git reset HEAD 文件名
```

### 撤销最近一次 commit

```bash
# 保留改动，回到未提交状态
git reset --soft HEAD~1

# 丢弃改动（慎用！）
git reset --hard HEAD~1
```

### 暂存当前工作（临时切分支处理其他事）

```bash
# 暂存
git stash

# 切回来后恢复
git stash pop
```

---

## 完整流程速览

```
拉取最新代码 -> 创建分支 -> 开发 -> add -> commit -> push -> 发PR -> 合并 -> 同步main
```
