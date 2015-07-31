Hipush Web API文档
==========================
web.hipush.zhangyue.com 代表从web节点拿数据

admin.hipush.zhangyue.com  代表从admin节点拿数据

api返回数据都是json格式

返回正常结果

```
http code: 200
http body:
{
    "ok": true,
    "result": ${result}
}
```

返回错误结果

```
http code: 403/500/400
http body:
{
	"error": ${error_code},
	"reason": ${reason}
}
```

1. 参数错误 http code=403, error=1024
2. 内部服务器错误 http code=500, error=1025
3. 对象不存在 http code=404, error=1026
4. http方法不允许 http code=405, error=1027
5. 认证错误 http code=401, error=1028

device_id
--------------------------
客户端直接使用支付宝的sdk来生成唯一ID


获取client_id
---------------------------

```
http://web.hipush.zhangyue.com/user/genId?device_id=${device_id}&app_key=${app_key}
{
    "client_id": ${client_id}
}
```

客户端要缓存在本地


获取token
--------------------------

```
http://web.hipush.zhangyue.com/user/token?client_id=${client_id}
{
    "client_id": ${client_id}
}
```

客户端要缓存在本地，token有效时间1周


获取comet节点
--------------------------
客户端优先使用hash方法获取地址，如果拿不到，就使用random方法获取

```
http://web.hipush.zhangyue.com/service/hash?client_id=${client_id}
{
    "ip": ${ip},
    "port": ${port}
}
```

```
http://web.hipush.zhangyue.com/service/random
{
    "ip": ${ip},
    "port": ${port}
}
```
客户端拿到地址后与服务器建立长链接进行通讯


发送消息
=====================================
应用程序通过admin节点向用户推送消息，消息必须和推送任务job挂接，才能对推送任务的发送量到达量进行统计

单播：向单个用户推送消息（比如个性化推荐）

组播：向某个topic下的所有用户推送消息（包含全量推送）

任务：{name,type,job_id}

消息：{job_id, msg_type, content}

在线推送online：是否只向在线用户推送true/false

应用程序如果想要使用hipush，必须有在hipush注册的app_key和app_secret

获取推送任务批次ID
------------------------------------
```
sign=hashlib.md5("app_key=${app_key}&name={$name}&type={type}" + ${app_secret}).hexdigest()

http://admin.hipush.zhangyue.com/job/genId?app_key=${app_key}&name=${name}&type={type}&sign={sign}
{
    "job_id": ${job_id},
}
```

单播
---------------------------------------

```
sign=hashlib.md5("app_key=${app_key}&client_id=${client_id}&content=${content}&job_id=${job_id}&msg_type=${msg_type}&online=${online}" + ${app_secret}).hexdigest()

http://admin.hipush.zhangyue.com/publish/private?app_key=${app_key}&job_id=${job_id}&client_id=${client_id}&msg_type={msg_type}&content=${content}&online={$online}&sign={sign}
OK
```

组播
---------------------------------------
组播一个消息会发送多次，所以需要先存消息，再拿消息ID发送

```
sign=hashlib.md5("app_key=${app_key}&content=${content}&job_id=${job_id}&msg_type=${msg_type}" + ${app_secret}).hexdigest()

http://admin.hipush.zhangyue.com/message/save?app_key=${app_key}&job_id=${job_id}&msg_type={msg_type}&content=${content}&sign=${sign}
{
    "message_id": ${message_id}
}
```

```
sign=hashlib.md5("app_key=${app_key}&msg_id=${msg_id}&online=${online}&topic=${topic}" + ${app_secret}).hexdigest()

http://admin.hipush.zhangyue.com/publish/multi?app_key=${app_key}&topic=${topic}&msg_id=${msg_id}&online={$online}&sign=${sign}
OK
```

后台订阅主题
------------------------------------------

```
sign=hashlib.md5("app_key=${app_key}&client_id=${client_id1}&client_id=${client_id2}&topic=${topic}" + ${app_secret}).hexdigest()

http://admin.hipush.zhangyue.com/topic/subscribe?appkey=${app_key}&client_id=${client_id}&topic=${topic}&sign=${sign}
OK
```

client_id可以传递多个，最多100个

后台取消订阅主题
------------------------------------------

```
sign=hashlib.md5("app_key=${app_key}&client_id=${client_id1}&client_id=${client_id2}&topic=${topic}" + ${app_secret}).hexdigest()

http://admin.hipush.zhangyue.com/topic/unsubscribe?appkey=${app_key}&client_id=${client_id}&topic=${topic}&sign=${sign}
OK
```

client_id可以传递多个，最多100个