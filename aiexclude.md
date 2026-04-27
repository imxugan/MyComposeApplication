# .aiexclude 配置模版，以及规则

超简单！3 步搞定 .aiexclude 排除敏感文件（复制即用）

这个文件和 .gitignore 用法完全一样，专门用来告诉 Gemini：这些文件 / 文件夹绝对不能读取、上传，安全又省心。

第一步：创建文件
在你的项目根目录（代码最外层文件夹），新建一个文件，文件名必须叫：
.aiexclude

⚠️ 注意：前面有个点，不要写错！

第二步：直接复制我的模板（开箱即用）
把下面内容全部粘贴进 .aiexclude，按需修改即可：

## --------------- 敏感配置文件 必选 ---------------

## 密钥、密码、token、环境变量

.env

.env.local

.env.development

.env.production

.secret

secrets.json

credentials.json

## 配置文件里的敏感信息

config.js

config.ts

application.properties

application.yml

## --------------- 个人隐私/私密代码 ---------------

## 你的私人文件夹

private/

secret/

敏感资料/

账号信息/

## --------------- 无需 AI 读取的大文件 ---------------

node_modules/

dist/

build/

*.log

.DS_Store

第三步：语法规则（一看就懂）
你只需要记住 3 条最简单的规则：

直接写文件名 → 排除这个文件

密码.txt
key.pem

写文件夹名 + / → 排除整个文件夹

私密代码/

公司机密/

## 开头 → 注释，不会生效

举个栗子（你的项目怎么配）
假设你的项目里：
有个 账号密码.txt 不能泄露
有个 secret-project/ 文件夹是机密
你的 .aiexclude 就写：
plaintext
账号密码.txt
secret-project/

✅ 搞定！Gemini 永远不会碰这些文件
