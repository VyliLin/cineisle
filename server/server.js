const express = require("express");
const cors = require("cors");
const app = express();
const PORT = process.env.PORT || 8787;
const TOKEN = process.env.CINEISLE_TOKEN || "";

app.use(cors());
app.use(express.json({ limit: "2mb" }));
app.use(express.static("public"));

const rooms = new Map();

function code() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  return Array.from({ length: 6 }, () => chars[Math.floor(Math.random()*chars.length)]).join("");
}
function now(){ return new Date().toISOString(); }
function ensure(id) {
  id = String(id || "").trim().toUpperCase();
  if (!id) throw new Error("ROOM_REQUIRED");
  if (!rooms.has(id)) rooms.set(id, {
    id, createdAt: now(), updatedAt: now(), title:"未命名影片", fileName:"",
    duration:0, currentTime:0, paused:true, lastActor:"", members:[],
    messages:[], notes:[], card:null, theme:"cream", partner:"观影人 A × 观影人 B", mood:"夜航", inviteNote:"今晚一起登岛看一场电影。"
  });
  return rooms.get(id);
}
function pub(r){
  return {...r, messages:r.messages.slice(-80), notes:r.notes.slice(-80)};
}
function getTokenFromReq(req) {
  return (req.headers.authorization || "").replace(/^Bearer\s+/i,"")
    || req.headers["x-cineisle-token"]
    || (req.query && req.query.token)
    || (req.body && req.body.token)
    || (req.body && req.body.params && req.body.params.token)
    || (req.body && req.body.arguments && req.body.arguments.token)
    || "";
}

function isAuthed(req) {
  if (!TOKEN) return true;
  return getTokenFromReq(req) === TOKEN;
}

function auth(req,res,next){
  if (!isAuthed(req)) return res.status(403).json({ok:false,error:"CINEISLE_BAD_TOKEN"});
  next();
}

app.get("/", (req,res)=>res.sendFile(__dirname + "/public/index.html"));
app.get("/api/health",(req,res)=>res.json({ok:true, app:"CineIsle Server", version:"0.2.0-public", rooms:rooms.size, tokenRequired:Boolean(TOKEN), time:now()}));

app.post("/api/rooms",(req,res)=>{
  const r = ensure(code());
  r.title = req.body.title || r.title;
  r.theme = req.body.theme || r.theme;
  r.partner = req.body.partner || r.partner;
  r.mood = req.body.mood || r.mood;
  r.inviteNote = req.body.inviteNote || r.inviteNote;
  res.json({ok:true, room: pub(r)});
});
app.get("/api/rooms/:id",(req,res)=>{
  const r = rooms.get(String(req.params.id).toUpperCase());
  if (!r) return res.status(404).json({ok:false,error:"ROOM_NOT_FOUND"});
  res.json({ok:true, room: pub(r)});
});
app.post("/api/rooms/:id/message", auth, (req,res)=>{
  const r = ensure(req.params.id);
  const m = { id:Date.now()+"", name:req.body.name || "观影人", text:String(req.body.text || "").slice(0,500), at:now() };
  r.messages.push(m); r.updatedAt = now();
  res.json({ok:true, message:m, room:pub(r)});
});
app.post("/api/rooms/:id/playback", auth, (req,res)=>{
  const r = ensure(req.params.id);
  if (typeof req.body.currentTime === "number") r.currentTime = Math.max(0, req.body.currentTime);
  if (typeof req.body.duration === "number") r.duration = Math.max(0, req.body.duration);
  if (typeof req.body.paused === "boolean") r.paused = req.body.paused;
  if (req.body.title) r.title = String(req.body.title).slice(0,100);
  if (req.body.fileName) r.fileName = String(req.body.fileName).slice(0,180);
  if (req.body.partner) r.partner = String(req.body.partner).slice(0,80);
  if (req.body.mood) r.mood = String(req.body.mood).slice(0,80);
  if (req.body.inviteNote) r.inviteNote = String(req.body.inviteNote).slice(0,240);
  r.lastActor = req.body.actor || req.body.name || "观影人";
  r.updatedAt = now();
  res.json({ok:true, room:pub(r)});
});
app.post("/api/rooms/:id/note", auth, (req,res)=>{
  const r = ensure(req.params.id);
  const n = { id:Date.now()+"", name:req.body.name || "观影人", text:String(req.body.text || "").slice(0,800), type:req.body.type || "note", time:req.body.time || r.currentTime, at:now() };
  r.notes.push(n); r.updatedAt = now();
  res.json({ok:true, note:n, room:pub(r)});
});
app.post("/api/rooms/:id/card", auth, (req,res)=>{
  const r = ensure(req.params.id);
  r.card = {
    title:req.body.title || r.title,
    rating:req.body.rating || 4.5,
    template:req.body.template || "ticket",
    partner:req.body.partner || r.partner || "",
    mood:req.body.mood || r.mood || "",
    inviteNote:req.body.inviteNote || r.inviteNote || "",
    quote:req.body.quote || "",
    note:req.body.note || "",
    zhiQuote:req.body.zhiQuote || req.body.userQuote || "",
    linQuote:req.body.linQuote || req.body.aiQuote || "",
    zhiNote:req.body.zhiNote || req.body.userNote || "",
    linNote:req.body.linNote || req.body.aiNote || "",
    generatedAt:now()
  };
  r.updatedAt = now();
  res.json({ok:true, card:r.card, room:pub(r)});
});


function mcpTools() {
  return [
    {
      name: "create_room",
      description: "创建一个 CineIsle 观影房间",
      inputSchema: {
        type: "object",
        properties: {
          title: { type: "string", description: "电影或房间标题" },
          theme: { type: "string", description: "主题皮肤，可选 cream/night/galaxy/matcha/film/dusk" },
          partner: { type: "string", description: "观影人显示名" },
          mood: { type: "string", description: "今晚观影氛围" },
          inviteNote: { type: "string", description: "观影邀请卡开场备注" }
        }
      }
    },
    {
      name: "get_room_state",
      description: "读取观影房间状态、播放进度、聊天、笔记和小卡片",
      inputSchema: {
        type: "object",
        properties: {
          room: { type: "string", description: "房间号" }
        },
        required: ["room"]
      }
    },
    {
      name: "send_room_message",
      description: "向观影房间发送聊天或弹幕",
      inputSchema: {
        type: "object",
        properties: {
          room: { type: "string", description: "房间号" },
          name: { type: "string", description: "发送者昵称" },
          text: { type: "string", description: "消息内容" },
          danmaku: { type: "boolean", description: "是否作为弹幕发送" }
        },
        required: ["room", "text"]
      }
    },
    {
      name: "control_playback",
      description: "同步控制播放状态，例如暂停、继续、跳转到某个秒数",
      inputSchema: {
        type: "object",
        properties: {
          room: { type: "string", description: "房间号" },
          currentTime: { type: "number", description: "播放进度，单位秒" },
          paused: { type: "boolean", description: "是否暂停" },
          actor: { type: "string", description: "操作者" }
        },
        required: ["room"]
      }
    },
    {
      name: "add_note",
      description: "给观影房间添加一条观影笔记",
      inputSchema: {
        type: "object",
        properties: {
          room: { type: "string", description: "房间号" },
          name: { type: "string", description: "记录者昵称" },
          text: { type: "string", description: "笔记内容" },
          time: { type: "number", description: "对应播放时间，单位秒" }
        },
        required: ["room", "text"]
      }
    },
    {
      name: "generate_card",
      description: "生成或更新观影小卡片",
      inputSchema: {
        type: "object",
        properties: {
          room: { type: "string", description: "房间号" },
          title: { type: "string", description: "卡片标题" },
          rating: { type: "number", description: "评分" },
          quote: { type: "string", description: "摘录" },
          note: { type: "string", description: "观影感想" },
          template: { type: "string", description: "卡片模板：ticket/receipt/postcard" },
          zhiQuote: { type: "string", description: "观影人 A 喜欢的台词" },
          linQuote: { type: "string", description: "观影人 B 喜欢的台词" },
          zhiNote: { type: "string", description: "观影人 A 观后感" },
          linNote: { type: "string", description: "观影人 B 观后感" },
          partner: { type: "string", description: "观影人显示名" },
          mood: { type: "string", description: "观影氛围" },
          inviteNote: { type: "string", description: "观影邀请卡开场备注" }
        },
        required: ["room"]
      }
    }
  ];
}

function mcpText(obj) {
  return {
    content: [
      {
        type: "text",
        text: typeof obj === "string" ? obj : JSON.stringify(obj, null, 2)
      }
    ]
  };
}

function callCinemaTool(name, args) {
  args = args || {};

  if (name === "create_room") {
    const r = ensure(code());
    r.title = args.title || r.title;
    r.theme = args.theme || r.theme;
    r.partner = args.partner || r.partner;
    r.mood = args.mood || r.mood;
    r.inviteNote = args.inviteNote || r.inviteNote;
    return pub(r);
  }

  if (name === "get_room_state") {
    const r = rooms.get(String(args.room || args.room_id || "").toUpperCase());
    if (!r) throw new Error("ROOM_NOT_FOUND");
    return pub(r);
  }

  if (name === "send_room_message") {
    const r = ensure(args.room || args.room_id);
    const text = args.danmaku ? "弹幕：" + String(args.text || "") : String(args.text || "");
    const m = {
      id: Date.now() + "",
      name: args.name || "观影人",
      text,
      at: now()
    };
    r.messages.push(m);
    r.updatedAt = now();
    return { message: m, room: pub(r) };
  }

  if (name === "control_playback") {
    const r = ensure(args.room || args.room_id);
    if (typeof args.currentTime === "number") r.currentTime = Math.max(0, args.currentTime);
    if (typeof args.paused === "boolean") r.paused = args.paused;
    if (args.partner) r.partner = String(args.partner).slice(0,80);
    if (args.mood) r.mood = String(args.mood).slice(0,80);
    if (args.inviteNote) r.inviteNote = String(args.inviteNote).slice(0,240);
    r.lastActor = args.actor || "观影人";
    r.updatedAt = now();
    return pub(r);
  }

  if (name === "add_note") {
    const r = ensure(args.room || args.room_id);
    const n = {
      id: Date.now() + "",
      name: args.name || "观影人",
      text: String(args.text || ""),
      type: args.type || "note",
      time: args.time || r.currentTime,
      at: now()
    };
    r.notes.push(n);
    r.updatedAt = now();
    return { note: n, room: pub(r) };
  }

  if (name === "generate_card") {
    const r = ensure(args.room || args.room_id);
    r.card = {
      title: args.title || r.title,
      rating: args.rating || 4.5,
      template: args.template || "ticket",
      partner: args.partner || r.partner || "",
      mood: args.mood || r.mood || "",
      inviteNote: args.inviteNote || r.inviteNote || "",
      quote: args.quote || "",
      note: args.note || "",
      zhiQuote: args.zhiQuote || args.userQuote || "",
      linQuote: args.linQuote || args.aiQuote || args.quote || "",
      zhiNote: args.zhiNote || args.userNote || "",
      linNote: args.linNote || args.aiNote || args.note || "",
      generatedAt: now()
    };
    r.updatedAt = now();
    return { card: r.card, room: pub(r) };
  }

  throw new Error("UNKNOWN_TOOL: " + name);
}

function rpcResult(id, result) {
  return { jsonrpc: "2.0", id, result };
}

function rpcError(id, code, message) {
  return { jsonrpc: "2.0", id, error: { code, message } };
}

function handleMcpMessage(req, msg) {
  const id = msg.id;
  const method = msg.method || msg.tool || msg.name;
  const params = msg.params || {};
  const args = params.arguments || params || msg.arguments || {};

  // notifications usually have no id; do not answer them
  if (!id && method && method.startsWith("notifications/")) return null;

  if (method === "initialize") {
    return rpcResult(id, {
      protocolVersion: "2024-11-05",
      capabilities: { tools: {} },
      serverInfo: {
        name: "映屿 CineIsle",
        version: "0.1.3"
      }
    });
  }

  if (method === "tools/list" || method === "list_tools") {
    return rpcResult(id, { tools: mcpTools() });
  }

  if (method === "tools/call") {
    if (!isAuthed(req)) return rpcError(id, -32001, "CINEISLE_BAD_TOKEN");
    const toolName = params.name;
    const toolArgs = params.arguments || {};
    try {
      const result = callCinemaTool(toolName, toolArgs);
      return rpcResult(id, mcpText(result));
    } catch (e) {
      return rpcError(id, -32000, e.message);
    }
  }

  // 兼容旧写法：直接 method=create_room / send_room_message
  if (["create_room", "get_room_state", "send_room_message", "control_playback", "add_note", "generate_card"].includes(method)) {
    if (!isAuthed(req)) return rpcError(id || 1, -32001, "CINEISLE_BAD_TOKEN");
    try {
      const result = callCinemaTool(method, args);
      return id ? rpcResult(id, mcpText(result)) : { ok: true, result };
    } catch (e) {
      return id ? rpcError(id, -32000, e.message) : { ok: false, error: e.message };
    }
  }

  return rpcError(id || 1, -32601, "Method not found: " + method);
}

app.get("/mcp", (req, res) => {
  res.type("text/plain").send("CineIsle MCP endpoint is running. Use POST JSON-RPC.");
});

app.post("/mcp", (req, res) => {
  try {
    const body = req.body || {};
    if (Array.isArray(body)) {
      const out = body.map(msg => handleMcpMessage(req, msg)).filter(Boolean);
      if (out.length === 0) return res.status(204).end();
      return res.json(out);
    }
    const out = handleMcpMessage(req, body);
    if (!out) return res.status(204).end();
    return res.json(out);
  } catch (e) {
    return res.status(500).json(rpcError(1, -32000, e.message));
  }
});

app.listen(PORT, () => console.log(`CineIsle server: http://localhost:${PORT}`));
