# 论坛附件上传链路调研

调研日期：2026-05-28

本文记录 Stage1 论坛网页原生附件上传链路，用于后续在 App 内实现论坛自带附件上传能力。调研过程中只保留接口结构和必要示例，不记录 cookie、auth、完整 hash 等敏感信息。

## 结论

- 上传闭环应以发帖、回帖、编辑的 editor helper 为入口，而不是帖子详情页。
- 发帖、回帖、引用回帖、编辑帖子都可以请求 `inajax=yes` 版本，返回更轻的 XML/CDATA 编辑表单。
- `inajax=yes` 编辑表单已经包含上传所需的全部上下文：`upload_url`、`uid`、`hash`、`fid`、`formhash`、已有附件列表等。
- 引用回复不需要单独的 quote helper 语义；`forum.php?mod=post&action=reply&tid=...&repquote=...&inajax=yes` 已同时提供引用通知字段和上传配置。
- 上传接口成功后只返回附件 id，即 aid，例如 `2075290`。
- 普通编辑模式下，图片插入正文使用 Discuz BBCode：`[attachimg]2075290[/attachimg]`。
- aid 转图片 URL 不是上传接口完成的，而是后续 `attachlist` 或最终帖子渲染阶段完成。
- `attachlist` 查询多个历史未消费附件时，`aids` 参数不是逗号分隔，而是网页同款的管道格式：`|2075398|2075396|2075383`。
- editor helper 里的历史未消费附件不一定直接出现在 DOM 节点里，也可能藏在脚本字符串里，例如 `ATTACHUNUSEDAID[2075398]` 或 `name="unused[]" value="2075398"`。
- `attachlist` 返回的动态预览 URL 会带 `nocache=yes` 和 `ramdom=<random>`。App 内展示和缓存时应移除这两个随机参数，但保留 `aid`、`size`、`key`、`type`。
- 删除接口会真正删除附件，并导致已有 `forum.php?mod=image&aid=...&key=...` 动态图片 URL 失效。因此图片选择页里的“移除”不应直接调用论坛删除接口，除非用户明确要删除服务端附件。
- Web 标准提交流程可以消费 `[attachimg]aid[/attachimg]`，并在最终帖子里渲染出稳定的 `img.stage1st.com` 图片地址。
- 当前移动 API 对论坛附件 aid 的自动消费不稳定。若正文中包含论坛附件，切到标准网页提交流程；普通文本、外链图片或非论坛附件仍继续走现有移动 API。

## 1. 进入回帖编辑页

回帖 editor helper 示例：

```text
GET https://stage1st.com/2b/forum.php?mod=post&action=reply&tid=2038487&inajax=yes
```

引用回帖只需要额外带 `repquote`：

```text
GET https://stage1st.com/2b/forum.php?mod=post&action=reply&tid=2038487&repquote=69641396&inajax=yes
```

发帖和编辑也可以使用同样的轻量 editor helper：

```text
GET https://stage1st.com/2b/forum.php?mod=post&action=newthread&fid=51&inajax=yes
GET https://stage1st.com/2b/forum.php?mod=post&action=edit&fid=51&tid=2038487&pid=69687073&inajax=yes
```

响应是 XML/CDATA 包裹的编辑表单。解出 HTML 后，页面中包含上传配置，形态类似：

```js
var upload = new SWFUpload({
  upload_url: "https://stage1st.com/2b/misc.php?mod=swfupload&action=swfupload&operation=upload&fid=51",
  post_params: {
    "uid": "223963",
    "hash": "<32位上传hash>"
  },
  custom_settings: {
    progressTarget: "attachlist",
    uploadSource: "forum",
    uploadType: "attach",
    uploadFrom: "fastpost"
  }
});
```

这说明 `hash` 等上传参数应从当前编辑页解析，并由外部传给上传模块。上传模块不应自己猜测或额外从帖子详情页拉取参数。

引用回复 editor helper 还会提供：

```text
noticeauthor
noticetrimstr
noticeauthormsg
```

这些字段覆盖了旧 quote helper 的用途。

## 2. 上传图片

上传请求：

```text
POST https://stage1st.com/2b/misc.php?mod=swfupload&action=swfupload&operation=upload&fid=51
Referer: https://stage1st.com/2b/forum.php?mod=post&action=reply&fid=51&tid=2038487&cedit=yes
Content-Type: multipart/form-data
```

multipart 字段：

```text
uid: 223963
hash: <32位上传hash>
id: WU_FILE_0
type: image/jpeg
size: 4594
filetype: jpeg
Filedata: filename="猫-我.jpeg", contentType="image/jpeg"
```

成功响应体是纯数字 aid：

```text
2075290
```

失败时可能返回负数错误码，例如测试无效图片曾返回：

```text
-13
```

因此解析时只有正整数 aid 才应视为上传成功。

## 3. aid 换附件列表与预览 URL

上传成功后，网页会继续请求附件列表。单个附件可以直接传 aid：

```text
GET https://stage1st.com/2b/forum.php?mod=ajax&action=attachlist&aids=2075290&fid=51&inajax=1&ajaxtarget=WU_FILE_0
```

多个历史未消费附件需要使用网页脚本里的管道格式：

```text
GET https://stage1st.com/2b/forum.php?mod=ajax&action=attachlist&aids=%7C2075398%7C2075396%7C2075383&fid=51&inajax=1&ajaxtarget=attachlist
```

不要使用逗号分隔。实测 `aids=2075398,2075396,2075383` 只会返回第一个附件。

响应是 XML 包裹的 HTML 片段，关键内容包括：

```html
<tbody id="attach_2075290">
  <a
    id="attachname2075290"
    isimage="1"
    onclick="insertAttachimgTag('2075290');hideMenu('attach_preview_2075290_menu');doane(event);">
    猫-我.jpeg
  </a>
  <div id="attach_preview_2075290_menu" class="attach_preview">
    <img
      id="image_2075290"
      src="forum.php?mod=image&aid=2075290&size=300x300&key=<image_key>&nocache=yes&type=fixnone&ramdom=<random>"
      cwidth="226" />
  </div>
</tbody>
```

浏览器随后加载预览图：

```text
GET https://stage1st.com/2b/forum.php?mod=image&aid=2075290&size=300x300&key=<image_key>&nocache=yes&type=fixnone&ramdom=<random>
```

这个 URL 依赖 `attachlist` 返回的 `key`，上传接口本身不给 `key`。

其中 `nocache=yes` 和 `ramdom=<random>` 会导致每次 URL 都不同。App 内用于 Glide/OkHttp 缓存和正文插入时，可以标准化为：

```text
https://stage1st.com/2b/forum.php?mod=image&aid=2075290&size=300x300&key=<image_key>&type=fixnone
```

不要移除 `key`。没有 `key` 的图片 URL 不能稳定访问。

## 4. 插入正文

网页里的插入函数：

```js
function insertAttachimgTag(aid) {
  if (wysiwyg) {
    insertText('<img src="' + $('image_' + aid).src + '" border="0" aid="attachimg_' + aid + '" alt="" />', false);
  } else {
    var txt = '[attachimg]' + aid + '[/attachimg]';
    insertText(txt, strlen(txt), 0);
  }
}
```

普通编辑模式下不会把 aid 转成 URL，而是直接插入：

```text
[attachimg]2075290[/attachimg]
```

富文本模式才会使用当前 DOM 中的 `image_<aid>.src`，即 `forum.php?mod=image&aid=...&key=...`。

如果当前 App 仍使用移动 API 提交发帖/回帖，不能依赖服务端消费 `[attachimg]aid[/attachimg]`。为了能显示图片，曾经的兼容方案是插入动态图片 URL：

```text
[img]https://stage1st.com/2b/forum.php?mod=image&aid=2075290&size=300x300&key=<image_key>&type=fixnone[/img]
```

这个 URL 来自 `attachlist` 响应。

但这不是最终推荐方案，原因是：

- 动态 URL 仍依赖附件未被删除且 key 有效。
- 移动 API 对论坛附件的自动消费不稳定，可能导致下次进入编辑页仍展示为未消费附件。
- 服务端不会把这类 `[img]forum.php?mod=image...[/img]` 转成最终稳定的 `img.stage1st.com` 静态地址。

当前实现方案是：图片上传页和编辑器内部都以 aid 作为论坛附件的真实身份，插入正文时直接使用 `[attachimg]aid[/attachimg]`。提交弹窗会检测正文里的论坛附件 aid；如果发现论坛附件，就改走标准网页表单提交。

## 5. 提交回复

提交请求示例：

```text
POST https://stage1st.com/2b/forum.php?mod=post&action=reply&fid=51&tid=2038487&extra=&replysubmit=yes
Content-Type: application/x-www-form-urlencoded
```

关键表单字段：

```text
formhash: <已打码>
posttime: 1779961449
wysiwyg: 1
subject:
message: [attachimg]2075290[/attachimg]
usesig: 1
replysubmit: yes
attachnew[2075290][description]:
attachnew[2075290][readperm]:
```

如果编辑页里有未插入正文的旧附件，网页会通过 `unused[]` 上报：

```text
unused[]: 2075287
unused[]: 2075284
```

这类附件由服务端清理或忽略。

已验证：移动 API 提交时依赖服务端“自动消费”不可靠。为了保证附件从未消费列表中正确消失，包含论坛附件的提交已改走标准网页表单。表单需要至少携带：

```text
formhash: <editor helper 解析值>
posttime: <editor helper 解析值>
wysiwyg: 1
subject: <标题或空>
message: <包含 [attachimg]aid[/attachimg] 的正文>
usesig: 1
attachnew[2075290][description]:
attachnew[2075290][readperm]:
```

引用回复还要保留 editor helper 解析出的通知字段：

```text
noticeauthor
noticetrimstr
noticeauthormsg
```

普通纯文本、外链图片、SM.MS/RIP 图片等不涉及论坛附件 aid 的提交，可以继续走当前移动 API，减少迁移风险。

提交路由规则：

- 正文包含 `[attachimg]aid[/attachimg]`：走网页表单提交。
- 正文包含历史兼容形态 `[img]forum.php?mod=image&aid=...&key=...[/img]`：提交前先转换成 `[attachimg]aid[/attachimg]`，再走网页表单提交。
- 正文不包含论坛附件 aid：继续走现有移动 API。

不要在提交前把附件本地标记为已消费。提交成功后也不再维护独立的本地已消费列表，后续展示以 editor helper 返回的服务端未消费附件列表为准。

## 6. 最终帖子渲染

服务端渲染帖子后，`[attachimg]aid[/attachimg]` 会变成标准静态图地址。

示例 HTML：

```html
<img
  id="aimg_2075290"
  class="zoom"
  aid="2075290"
  src="https://static.stage1st.com/image/common/none.gif"
  file="https://img.stage1st.com/forum/202605/28/174424shdroskmeampsjmm.jpeg"
  zoomfile="https://img.stage1st.com/forum/202605/28/174424shdroskmeampsjmm.jpeg" />
```

下载附件链接则是另一条动态 URL：

```text
forum.php?mod=attachment&aid=<base64_payload>&nothumb=yes
```

因此最终标准图片 URL 只有服务端渲染帖子后才稳定出现在 `file` / `zoomfile` 属性里。

## 7. 删除附件

网页删除附件请求：

```text
GET https://stage1st.com/2b/forum.php?mod=ajax&action=deleteattach&inajax=yes&formhash=<formhash>&tid=2038487&pid=0&aids[]=2070543&aids[]=2070569
```

响应示例：

```xml
<?xml version="1.0" encoding="utf-8"?>
<root><![CDATA[2]]></root>
```

返回值看起来表示成功删除的附件数量。

已验证：删除成功后，原本可访问的 `forum.php?mod=image&aid=...&key=...` 图片 URL 会失效，`attachlist` 也不再返回该附件。因此当前 App 的图片选择页应区分两种行为：

- 从编辑器里移除：只做本地移除，不调用删除接口。
- 明确删除服务端附件：调用 `deleteattach`，并从本地历史未消费附件列表移除。

## App 实现建议

1. 发帖、回帖、编辑页面各自先请求对应的 `inajax=yes` editor helper，并统一通过 `PostEditor.fromHtml(...)` 解析编辑上下文。
2. 上传 Manager 只接收外部传入的上传参数，不在内部自行请求或猜测 `hash`。
3. 上传成功响应必须校验为正整数 aid，负数或其他文本视为失败。
4. 上传成功后请求 `attachlist` 获取预览 URL；用于 App 内展示时移除 `nocache`、`ramdom`、`random` 等随机参数。
5. editor helper 返回的未消费附件列表可展示在图片上传页，方便用户复用上次上传但未删除/未消费的图片。多个 aid 请求 `attachlist` 时必须使用 `|aid1|aid2` 格式。
6. 图片选择页里的移除操作默认只做本地移除，不调用 `deleteattach`。真实删除需要单独的用户确认或独立入口。
7. 提交时按内容路由：
   - 正文没有论坛附件 aid：继续走现有移动 API。
   - 正文包含论坛附件 aid：走标准网页表单提交，正文使用 `[attachimg]aid[/attachimg]`，并携带 `attachnew[aid][description]`、`attachnew[aid][readperm]`。
8. 网页表单提交成功后，直接刷新/退出编辑页；不要维护独立的本地“已使用附件”状态。
9. 图片上传页设置收进默认折叠的设置条，当前包含：
   - 论坛附件上传 / 外链图床上传。
   - 原图分辨率 / 压缩上传。
10. 已上传图片列表使用更密的网格展示，一行按屏幕宽度显示 3 到 5 个缩略图。

## 待验证点

- 新主题、普通回复、引用回复、编辑帖子四种网页提交的完整字段差异。
- `unused[]` 在 App 自己构造网页表单时是否必须携带；如果携带，是否会影响未使用附件后续复用。
- 网页提交失败时附件是否仍保持未消费状态。
- 网页提交成功后的响应结构如何稳定判断成功、失败、验证码或权限问题。
