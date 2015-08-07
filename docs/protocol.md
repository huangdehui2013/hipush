comet二进制协议
=============================
comet服务器和客户端的交互是通过一系列指令来完成
C. 客户端发送认证auth指令
S. 服务器回答auth_success

C. 客户端发送密钥exchange_key指令
S. 服务器回答ok

C. 客户端发送获取主题列表get_topic_list指令
S. 服务器回答主题列表topic_list指令

C. 客户端发送订阅主题subscribe指令
S. 服务器回答ok

C. 客户端发送取消订阅主题unsubscribe指令
S. 服务器回答ok

C. 客户端发送获取离线消息get_message_list指令
S. 服务器回答消息列表message_list指令
C. 客户端发送消息确认message_ack指令
S. 服务器回答ok

C. 客户端发送心跳heatbeat指令
S. 服务器回答ok

S. 服务器向客户端持续推送在线消息message指令
C. 客户端发送消息确认message_ack指令
S. 服务器回答ok

主题名称必须是以@开头
消息这个概念会有点混淆，可以指协议消息也可以指推送消息
为了避免混淆，文档里将协议消息称为消息，推送消息称为通知


基础类型
-----------------------------
1. byte一个字节,short二个字节,integer4个字节,long8个字节
2. boolean=byte{0,1} 一个字节用0和1表示
3. string=short{length}+byte[length] 二个字节长度+字节数组

协议指令
----------------------------
1. short{length} 二个字节的指令消息长度，包括指令类型和消息体，不包括消息长度本身
2. byte{type} 一个字节的指令类型
3. byte[length]{content} 字节数组消息体，不同的指令结构是不一样的


auth消息体{type=0}
-----------------------------
1. string{client_id} 一个字符串
2. string{token} 一个字符串

exchange_key消息题{type=1}
-----------------------------
1. bytes{secret_key}

get_topic_list消息体{type=2}
-----------------------------
无

subscribe消息体{type=3}
------------------------------
1. byte{count} 一个字节的主题数量
2. string[count] 主题名称列表

unsubscribe消息体{type=4}
------------------------------
1. byte{count} 一个字节的主题数量
2. string[count] 主题名称列表

get_message_list消息体{type=5}
-----------------------------
无

heartbeat消息题{type=6}
----------------------------
无

message_ack消息题{type=7}
-----------------------------
1. byte{count} 一个字节的数量
2. string[count] 消息Id列表

report_environ消息体{type=8}
------------------------------
1. byte{network_type}
2. byte{isp}
3. string{phone_type}
4. string[]{extra}

ok消息题{type=0}
----------------------------
无

error消息题{type=1}
----------------------------
1. byte{error_code} 错误号
2. string{reason} 错误原因

auth_success消息体{type=2}
---------------------------
1. bytes{encrypt_key} 密钥加密公钥

topic_list消息体{type=3}
----------------------------
1. byte{count} 一个字节的主题数量
2. string[count] 主题名称列表

通知结构体
------------------------
单个通知由类型,id,内容,时间戳构成
message_struct={byte{msg_type},string{msg_id},string{content}, long{ts}}

message_list消息体{type=4}
----------------------------
1. byte{count} 一个字节的通知数量
2. message_struct[count] 通知列表

message消息题{type=5}
-----------------------------
一个message_struct


