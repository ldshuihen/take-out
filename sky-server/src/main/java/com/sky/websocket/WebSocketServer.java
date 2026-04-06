package com.sky.websocket;

import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket服务
 *
 * 这段代码是一个基于Java + Spring框架实现的**WebSocket服务端组件**，核心作用是建立服务器与客户端之间的双向实时通信，具体功能和细节如下：
 *
 * ### 一、核心定位
 * 通过JSR-356标准的WebSocket API（`javax.websocket`）实现，结合Spring的`@Component`注解托管为Spring Bean，
 * 对外暴露WebSocket连接端点，支持客户端（如浏览器、APP）与服务端的实时消息交互。
 *
 * ### 二、关键功能拆解
 * #### 1. 注解与端点配置
 * - `@ServerEndpoint("/ws/{sid}")`：定义WebSocket服务的访问端点为`/ws/{sid}`，其中`{sid}`是路径参数（可理解为客户端唯一标识，如用户ID、设备ID），不同客户端可通过不同的`sid`建立独立连接。
 * - `@Component`：将该类交给Spring容器管理，确保服务端能正常加载并提供WebSocket服务。
 *
 * #### 2. 连接生命周期管理
 * 维护一个静态的`sessionMap`（`HashMap<String, Session>`），用于存储每个客户端连接的`Session`对象（key为`sid`，value为`Session`），核心生命周期方法：
 *
 * | 注解       | 方法          | 作用                                                                 |
 * |------------|---------------|----------------------------------------------------------------------|
 * | `@OnOpen`  | `onOpen()`    | 客户端建立连接时触发：打印连接日志，将客户端`sid`和`Session`存入`sessionMap` |
 * | `@OnMessage`| `onMessage()` | 收到客户端消息时触发：打印客户端`sid`和发送的消息内容                 |
 * | `@OnClose` | `onClose()`   | 客户端断开连接时触发：打印断开日志，从`sessionMap`中移除该客户端的`Session` |
 *
 * #### 3. 群发消息功能
 * `sendToAllClient(String message)` 方法实现**向所有已连接的客户端群发消息**：
 * - 遍历`sessionMap`中所有的`Session`对象；
 * - 通过`session.getBasicRemote().sendText(message)`向每个客户端发送文本消息；
 * - 捕获发送过程中的异常并打印堆栈（避免单个客户端发送失败导致整体群发中断）。
 *
 * ### 三、使用场景
 * 适用于需要**服务端主动推送、客户端与服务端实时双向通信**的场景，例如：
 * - 实时聊天系统；
 * - 后台系统的实时通知（如订单状态变更、告警推送）；
 * - 实时数据展示（如大屏监控、股票行情）。
 *
 * ### 四、核心注意点
 * 1. `sessionMap` 是静态变量，属于类级别，确保所有`WebSocketServer`实例共享同一个连接映射；
 * 2. 仅实现了基础的文本消息交互，未处理二进制消息、异常（如连接异常断开）、心跳检测等进阶场景；
 * 3. 群发消息为同步实现，高并发场景下可能需要优化（如异步发送、线程池）。
 */
@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

    // sessionMap 核心存储 服务端与每个客户端的 WebSocket 长连接会话对象，Key 是客户端唯一标识，Value 是对应连接的 Session；
    //存放会话对象
    private static Map<String, Session> sessionMap = new HashMap();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        System.out.println("客户端：" + sid + "建立连接");
        sessionMap.put(sid, session);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        System.out.println("收到来自客户端：" + sid + "的信息:" + message);
    }

    /**
     * 连接关闭调用的方法
     *
     * @param sid
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        System.out.println("连接断开:" + sid);
        sessionMap.remove(sid);
    }

    /**
     * 向所有已建立WebSocket连接的客户端群发消息
     *
     * @param message 要发送的文本消息内容，不能为空（若需空消息需额外处理）
     */
    public void sendToAllClient(String message) {
        // 1. 获取所有已保存的WebSocket会话对象集合
        // sessionMap：键为客户端标识（如Session ID），值为对应WebSocket会话对象，存储所有活跃连接
        Collection<Session> sessions = sessionMap.values();

        // 2. 遍历每个会话，逐个向客户端发送消息
        for (Session session : sessions) {
            try {
                // 3. 通过会话的基础远程通信对象发送文本消息
                // getBasicRemote()：获取同步的远程通信对象，sendText()：发送文本格式的WebSocket消息
                // 注：若需异步发送可使用getAsyncRemote()，但同步更易控制发送顺序
                session.getBasicRemote().sendText(message);

                // 4. 日志打印发送成功标识（生产环境建议替换为日志框架如logback/log4j）
                System.out.println("发送websocket：消息发送成功，目标会话ID=" + session.getId());

            } catch (Exception e) {
                // 5. 异常处理：捕获发送过程中的所有异常（如连接断开、网络异常等）
                // 注：生产环境需细化异常类型（如IOException），并增加失败重试/离线消息等逻辑
                e.printStackTrace();
                System.err.println("发送websocket失败：目标会话ID=" + session.getId() + "，异常信息=" + e.getMessage());
            }
        }
    }

}
