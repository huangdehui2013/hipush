hipush概要设计
=====================
1. comet服务器，维持客户端长链接，netty
2. web服务器，客户端选择comet服务器，netty
3. admin服务器，消息发送，netty
4. zookeeper服务发现curator
5. redis存储jedis
6. nginx路由web和admin服务器
7. http调用unirest
8. supervisor进程监控
9. maven exec运行进程
10. 自定义二进制协议
11. 消息加密


```
                          |----------------------------|
                          |           nginx            |
                          |____________________________|

|----------o-----------|  |------------|  |------------|
|     socket port      |  |            |  |            |
|                      |  |            |  |            |
|        Comet         |  |     Web    |  |    Admin   |
|                      |  |            |  |            |
|     rpc[http] port   |  | http port  |  |  http port |
|__________o___________|  |______o_____|  |______o_____|
          /|\                                    |
           |-------------------------------------|
           
|------------------------------------------------------|
|                  zookeeper cluster                   |
|------------------------------------------------------|

|--------| |--------| |--------| |---------| |--------|
|        | |        | |        | |         | |        |
|  meta  | |  user  | | route  | | message | | topic  |
| redis  | |  redis | | redis  | |  redis  | | redis  |
|        | |        | |        | |         | |        |
|________| |________| |________| |_________| |________|

```


comet
========================
1. 外网IP端口, 监听客户端长链接请求
2. 内网IP端口，提供内网Http RPC服务（发送消息）
3. 7w长链接 < 0.6G内存[优化后]

```
                    /------io--------\                         /---worker---\
------acceptor----/--------io---------\_____main processor----/----worker----\______
------acceptor----\--------io---------/           /|\         \----worker----/     |
                   \-------io--------/             |           \---worker---/      |
                                                   |                               |
                                                   |                               |
                                                   |-------------------------------|
```

web,admin
=========================
1. 内网IP端口


```
                    /------io---------
------acceptor----/--------io---------
------acceptor----\--------io--------- 
                   \-------io---------

```


物理机部署10台
=========================
1. comet进程各20台
2. web/admin进程各4个
3. zookeeper实例1个
4. nginx实例1个
5. dns映射到10台机器的外网IP


基本词汇
========================
1. deviceId
2. clientId
3. 私有消息
4. 主题消息


协议
======================
command = auth|get_topic_list|unsubscribe|subscribe|get_message_list|heartbeat

response = ok|error|topic_list|message_list|message

auth = msg_len{short} + msg_type_auth{byte} + client_id{string} + token{string}

exchange_key = msg_len{short} + msg_type_exchange_key{byte} + secret_key{bytes}

get_topic_list = msg_len{short} + msg_type_get_topic_list{byte}

get_message_list = msg_len{short} + msg_type_get_message_list{byte}

unsubscribe = msg_len{short} + msg_type_unsubscribe{byte} + topics_size{byte} + topics{string[len]}

subscribe = msg_len{short} + msg_type_subscribe{byte} + topics_size{byte} + topics{string[len]}

heatbeat = msg_len{short} + msg_type_hearbeat{byte}

ok = response_len{short} + response_type_ok{byte}

error = response_len{short} + response_type_error{byte} + error_code{byte} + reason{string}

auth_success = response_len{short} + response_type_auth_success{byte} + encrypt_key{bytes}

topic_list =  response_len{short} + response_type_topic_list{byte} + topics_size{byte} + topics{string[len]}

message_list =  response_len{short} + response_type_message_list{byte} + messages_size{byte} + messages{message_struct[len]}

message = response_len{short} + response_type_message{byte} + body{message_struct}

message_struct = type{byte} + id{string} + job_id{string} + timestamp{long} + encrypted_content{bytes}

string = len[short] + byte{len}

bytes = len[short] + byte[len]


内存数据结构
=============================
1. 所有链接的用户 Set[Channel]
2. 已认证的用户 Map[ClientId, ClientInfo] ClientInfo{appId, topics, messages, channel}
3. [主题消息]缓存 Map[MessageId, MessageInfo] MessageInfo{type, id, content, timestamp}
4. 应用列表 Map[AppKey, AppInfo], Map[AppId, AppInfo] AppInfo{id, key, secret, name}
5. 命令队列 BlockingQueue[ProtocolCommand]


唯一id设计
===========================
prefix{1} + serverId{2} + sequence{5} + timestamp{8}

1. 设备device_id采用taobao deviceId，这个由客户端生成
2. 用户client_id前缀c
3. 私有消息message_id前缀p
4. 主题消息message_id前缀m
5. token直接uuid


redis数据结构
=============================
1. 应用列表apps用hash存储{appkey=>appinfo{json}}
2. 设备到clientId的映射 [device_id, app_id] => client_id
3. clientId到设备的映射 client_id => [device_id, app_id]{json}
4. 路由映射 client_id => server_id
5. 消息体 message_id => messageinfo{json} 过期1天
6. 主题消息体 zset{message_id => timestamp}
7. 用户的消息列表 client_id => messageid{join(',')} 过期1天
8. 用户订阅的主题 client_id => topic_names{join(',')}
9. 主题订阅用户列表 [app_id,topic_name,server_id] => zset{client_id=>timestamp}


redis存储(1亿用户)
=========================
1. meta redis
2. message redis 30G
3. topic redis 80G
4. user redis 50G
5. route redis 10G



安全
==========================
1. 不需要安全机制的可以直接访问web节点获取token
2. 需要安全机制保障的由第三方应用程序提供
3. 第三方会分配appkey appsecret
4. client通过第三方app代理来获取token
5. 第三方需要拿appkey和appsecret以及device_id到admin节点拿token
6. token有效期7天，客户端需要缓存起来


```
client => business app  => admin server
   ||                          ||
  \||/                        \||/
   \/                          \/
comet server    ======>    redis server{token=>client_id}
```


扩容
==========================
1. 应用程序扩容，直接增加节点即可
2. 存储扩容，这个得离线进行


对客户端的要求
==========================
永久保持沉默


应用程序
===========================
1. 维持user_name和client_id的关系  user_name=>client_id[] client_id => user_name
2. 定向发送消息  提供client_id,msg_type,content定向发送
3. 发送主题消息  先存消息得到msg_id，再提供msg_id,topic_name,app_key广播消息


服务器搭建指南
========================================================================
1. git clone git@192.168.6.70:qianwenpin/hipush.git
2. 安装maven
3. 安装supervisor
4. 安装redis-server，用默认端口6379启动
5. 安装zookeeper，用默认端口2181启动
6. 修改supervisor/dev.conf中的192.168.1.106 改成你自己的ip地址
7. mkdir /tmp/hipush
8. mkdir /data/logs/hipush
9. 执行mvn compile
10. 执行supervisord -c supervisor/dev.conf
11. 执行supervisorctl -c supervisor/dev.conf
打开 http://ip:8081   hipush/hipush 访问管理后台

[Java客户端](http://192.168.6.70/push/hipush-client/tree/master)
[API文档](docs/api.md)
[协议文档](docs/protocol.md)
