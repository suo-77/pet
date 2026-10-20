# 03 API 接口清单规范

## 统一约定
- 基础前缀：后端 `http://localhost:8080/api`（后端端口可配置，前端 8085 配代理）
- 统一返回：`{ "code": 0, "msg": "ok", "data": {...} }`；code=0 成功，401 未登录/token失效，403 无权限，其他为业务/系统错误
- 需鉴权接口请求头：`Authorization: <token>`
- 分页参数：`page`(默认1)、`pageSize`(默认10)；返回 `data = { total, list }`
- 时间字段返回时间戳，前端统一做友好格式化（几分钟前/几小时前）

## A. 账号模块 `/api/auth`
| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | /api/auth/sendSmsCode | 否 | 发送短信验证码（主接口快速返回，身份 MQ 异步发短信）；body {phone} |
| POST | /api/auth/register | 否 | 账号密码注册/短信注册；body {username,password,phone?,code?,avatar?} |
| POST | /api/auth/login | 否 | 账号密码登录 {username,password} |
| POST | /api/auth/loginBySms | 否 | 短信验证码登录 {phone,code} |
| GET | /api/auth/info | 是 | 获取当前登录用户信息 |
| POST | /api/auth/logout | 是 | 退出登录（删本地token+Redis token） |

## B. 文件上传 `/api/file`
| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | /api/file/upload | 是 | 统一上传（头像/社区图/聊天图/评价图）；multipart `file` → 返回图片URL；MQ 异步压缩 |

## C. 用户与关注 `/api/user`
| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| PUT | /api/user/profile | 是 | 编辑资料 {nickname,intro,avatar} |
| GET | /api/user/{id} | 否 | 查看用户公开主页（含粉丝数/关注数/动态数） |
| GET | /api/user/follow/page | 是 | 我关注的人（分页） |
| GET | /api/user/fans/page | 是 | 我的粉丝（分页） |
| GET | /api/user/recommend | 是 | 推荐用户（分页） |
| POST | /api/user/follow/{id} | 是 | 关注/取关某用户（Redis缓存） |
| GET | /api/user/follow/status/{id} | 是 | 查看是否已关注 |

## D. 社区 `/api/post`
| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | /api/post/page | 否 | 社区信息流（分页，含作者信息/是否点赞） |
| GET | /api/post/{id} | 否 | 帖子详情 |
| POST | /api/post | 是 | 发布帖子 {content,images[]}（敏感词校验） |
| DELETE | /api/post/{id} | 是 | 删除自己的帖子（权限校验） |
| GET | /api/post/user/{userId}/page | 否 | 某用户发布的所有帖子（分页） |
| POST | /api/post/{id}/like | 是 | 点赞/取消（Redis+MQ） |
| GET | /api/post/{id}/comment/page | 否 | 帖子评论（分页） |
| POST | /api/post/{id}/comment | 是 | 发表评论（敏感词过滤） |

## E. 搜索 `/api/search`
| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | /api/search?keyword=&type=| 否 | 首页搜索：0店铺 1宠物商品 2用户昵称（分页返回） |

## F. 店铺与商品 `/api/shop` `/api/goods`
| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | /api/shop/page | 否 | 店铺列表（分页） |
| GET | /api/shop/{id} | 否 | 店铺详情（简介+商品+优惠券+秒杀券） |
| GET | /api/goods/page?shopId= | 否 | 商品列表（分页，可按店铺） |
| GET | /api/goods/{id} | 否 | 商品详情（含是否收藏/已在购物车） |
| GET | /api/goods/{id}/evaluate/page | 否 | 商品评价（分页） |
| POST | /api/goods/{id}/evaluate | 是 | 发表评价（敏感词过滤） |
| POST | /api/goods/{id}/fav | 是 | 收藏/取消收藏 |
| GET | /api/goods/favs/page | 是 | 我的收藏（分页） |

## G. 优惠券/秒杀/购物车/订单 `/api/coupon` `/api/cart` `/api/order`
| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | /api/coupon/{couponId}/receive | 是 | 领取普通券（库存/上限校验） |
| GET | /api/coupon/mine | 是 | 我的优惠券（按未用/已用/过期分类） |
| POST | /api/order/seckill/{couponId} | 是 | 秒杀下单（Redis预扣库存→MQ异步建单） |
| GET | /api/cart | 是 | 购物车列表（Redis） |
| POST | /api/cart/add | 是 | 加购 {goodsId,num} |
| PUT | /api/cart/{goodsId} | 是 | 修改数量 |
| DELETE | /api/cart/{goodsId} | 是 | 删除购物车项 |
| POST | /api/order/create | 是 | 普通下单（MQ异步建单） body {goodsId,num,couponId?} |
| GET | /api/order/page | 是 | 我的订单（分页，按状态筛选） |

## H. 聊天（WebSocket + REST）`/api/chat`
| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| WS | /ws/{userId} | token(sub协议) | 建立私信连接，实时收发 {type,toUserId,content,imageUrl} |
| GET | /api/chat/session/page | 是 | 会话列表（分页，含未读数） |
| GET | /api/chat/message/page?sessionId= | 是 | 某会话历史消息（分页） |
| POST | /api/chat/send | 是 | 兜底发送（非WS场景） |
| PUT | /api/chat/read/{sessionId} | 是 | 标记会话已读（清未读红点） |

## WebSocket 消息协议
- 客户端→服务端：`{"type":"text|image","toUserId":1,"content":".."}` 或 `{"type":"image","toUserId":1,"imageUrl":".."}`
- 服务端→客户端：`{"type":"text|image|read","fromUserId":1,"toUserId":1,"content":"..","imageUrl":"..","sendTime":...,"sessionId":...}`
- 发给在线用户实时推送；同时发 MQ `chat.notify` 做未读/通知兜底。

## 统一错误码
| code | 含义 |
|------|------|
| 0 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未登录 / token失效 |
| 403 | 无权限（删除他人帖子/越权） |
| 404 | 资源不存在 |
| 429 | 频率限制（短信验证码） |
| 500 | 系统异常 |