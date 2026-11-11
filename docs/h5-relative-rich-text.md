# H5 富文本图片相对路径接口

现有接口、字段保持兼容。后台编辑一次，服务层在同一事务中自动保存原有绝对图片地址版本和相对图片地址版本。前端无需自行拆出图片，也无需提交两份内容。

## 新增公共内容接口

`GET /api/config/content-relative?lang=en_US`

匿名访问，与 `GET /api/config/content` 使用相同返回结构、语言选择、逐字段语言回退和证书开关规则。不同之处是以下九个字段的完整富文本中，站内图片使用 `/profile/...`：

| 字段 | 内容 |
| --- | --- |
| `data.protocolContent` | 注册协议 |
| `data.aboutContent` | 关于我们 |
| `data.certificateContent` | 证书 |
| `data.helpContent` | 帮助中心 |
| `data.termsContent` | 条款 |
| `data.eventContent` | 事件 |
| `data.transactionDescription` | 交易说明 |
| `data.orderDescription` | 订单说明 |
| `data.usageDescription` | 使用说明 |

`lang` 和 `certificateTip` 保持原接口语义。证书开关开启时仍返回空的 `certificateContent` 及对应语言提示语。

示例（仅展示部分返回字段）：

```json
{
  "code": 200,
  "msg": "Success",
  "data": {
    "lang": "en_US",
    "certificateContent": "<h2>Certificate</h2><p>Full text</p><img src=\"/profile/upload/a.jpg\"><img src=\"/profile/upload/b.jpg\">"
  }
}
```

## 其它五个接口的新增字段

| 接口 | 原字段（保留） | 新字段（完整相对图片富文本） |
| --- | --- | --- |
| `GET /api/config/member-levels` | `data[].description` | `data[].descriptionRelative` |
| `GET /api/user/getInfo` | `data.memberLevel.description` | `data.memberLevel.descriptionRelative` |
| `POST /api/user/login` | `data.user.memberLevel.description` | `data.user.memberLevel.descriptionRelative` |
| `GET /api/messages/{id}` | `data.noticeContent` | `data.noticeContentRelative` |
| `GET /api/goods/getGoodsList` | `data[].description` | `data[].descriptionRelative` |

会员等级描述在登录、用户信息和等级接口中共用。相对副本始终跟随原字段选中的语言：请求语言缺少某个字段时，两种版本一起回退到英文，再回退到基础内容。

站内信列表 `GET /api/messages` 仍只返回摘要。证书开关接口 `GET /api/config/certificate` 仍只返回开关和提示语。

## 保存与历史数据

新增数据库列：

- `order_config.content_relative`：保存完整配置 JSON 或历史 HTML 的相对版本。
- `goods.description_relative`、`goods_member_level.description_relative`：保存完整说明的相对版本。
- `order_site_message.content_relative`：保存完整站内信的相对版本。
- `translations.relative_content`：JSON 对象，键为语言代码（如 `en_US`），值为对应语言完整内容的相对版本。

以上新列均为可空 `LONGTEXT`。新增、修改、批量图片导入和多语言编辑通过服务层同步生成两份内容；客户端传入的派生字段不会覆盖自动生成结果。未提交原内容的部分更新保留现有两份内容，显式提交空字符串会同时清空两份。多语言的部分更新会锁定翻译记录并合并其它语言，避免覆盖。

追加迁移 `V2026_11_10_00__rich_text_relative_copies` 创建新列并分批回填已有记录，只写新增列，不改旧内容；无需管理员逐条重新保存。新接口对缺少副本的历史值也会兜底转换。

迁移使用冻结的转换代码和固定语言列表；未来的业务转换器或语言列表调整不会改变这次已发布迁移的行为。不要修改已发布迁移及其冻结工具，后续变更应追加新迁移。

## 图片地址规则

本项目的上传资源命名空间是 `/profile/`。相对副本移除这类图片地址的域名和历史代理前缀，保留文件路径、查询参数、锚点及富文本其它内容。处理 `img src/srcset` 和 `picture` 内的 `source srcset`，不改普通链接、文本、样式或脚本。其它路径的外部图片、`data:` 和 `blob:` 图片保持原样。这里以 `/profile/` 命名空间识别本站资源，不按历史域名识别；外站若也使用相同路径，也会按本站资源转换。

旧版 HTML 已有绝对图片地址时，保存后仍保留该地址。若提交的是 `/profile/...` 相对地址，旧字段的绝对版本优先使用：

```yaml
order:
  resource-base-url: https://api.example.com
```

也可以设置环境变量 `RESOURCE_BASE_URL`；有反代前缀时填写完整公开根地址，例如 `https://api.example.com/backend`，不要填写 `/profile`。如果 HTTPS 在反代处终止，或反代会剥离公开路径前缀，接收相对图片输入时应配置此公开地址，避免保存出的绝对版本缺少 HTTPS 或反代前缀。未配置时，Web 保存请求沿用当前请求的域名与应用上下文；后台任务没有请求上下文且没有配置时保留原地址，不猜测域名。因此后台任务需要接收相对图片输入时，应配置此值。

新皮肤可以在渲染前按部署的资源根地址补全相对图片；也可以在页面域名上配置 `/profile/` 到资源后端的反代。反代信息无需写进每份富文本。

## 升级与验证

后端默认 `order.migration-version` 已提升到 `1.2.6`。部署保留生产数据库、Redis、上传目录及现有外置配置；若外置配置固定了迁移发布号，也需将该项提升到 `1.2.6` 后启动新后端，让受控 Flyway 执行迁移。不能只更新 JAR 却继续固定旧迁移号。

新增接口、原字段兼容、九个完整富文本、同语言回退、证书开关、五个新增字段和保存同步均有自动测试；另有隔离 MySQL 迁移回填和真实 Mapper 保存测试。验证结果见项目外 `diagnostics/2026-11-10-richtext-relative/`。
