const WebSocket = require('ws');

const wss = new WebSocket.Server({ port: 8080 });

// Map to store rooms: roomId -> Set of WebSockets
const rooms = new Map();

// Helper to find which room a client is in
function getRoomIdForClient(ws) {
  for (const [roomId, clients] of rooms.entries()) {
    if (clients.has(ws)) {
      return roomId;
    }
  }
  return null;
}

console.log('Signaling Server running on port 8080');

wss.on('connection', function connection(ws) {
  console.log('Client connected');

  ws.on('message', function incoming(message) {
    let data;
    try {
      data = JSON.parse(message);
    } catch (e) {
      console.error('Invalid JSON');
      return;
    }

    console.log('Received:', data.type);

    switch (data.type) {
      case 'join':
        const roomId = data.room;
        if (!roomId) return;

        if (!rooms.has(roomId)) {
          rooms.set(roomId, new Set());
        }

        const roomClients = rooms.get(roomId);

        // Notify existing peers that a new peer joined
        roomClients.forEach(client => {
          if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify({
              type: 'peer_joined'
            }));
          }
        });

        // Add new client to room
        roomClients.add(ws);
        console.log(`Client joined room ${roomId}. Total clients: ${roomClients.size}`);
        break;

      case 'offer':
      case 'answer':
      case 'candidate':
        // Broadcast to other clients in the SAME room
        const currentRoomId = getRoomIdForClient(ws);
        if (currentRoomId && rooms.has(currentRoomId)) {
          rooms.get(currentRoomId).forEach(client => {
            if (client !== ws && client.readyState === WebSocket.OPEN) {
              client.send(message);
            }
          });
        }
        break;

      default:
        console.log('Unknown message type:', data.type);
        break;
    }
  });

  ws.on('close', function () {
    console.log('Client disconnected');
    const roomId = getRoomIdForClient(ws);
    if (roomId && rooms.has(roomId)) {
      const roomClients = rooms.get(roomId);
      roomClients.delete(ws);
      if (roomClients.size === 0) {
        rooms.delete(roomId);
        console.log(`Room ${roomId} deleted (empty)`);
      }
    }
  });

  // Heartbeat
  ws.isAlive = true;
  ws.on('pong', function heartbeat() {
    this.isAlive = true;
  });
});

// Interval to check for dead connections
const interval = setInterval(function ping() {
  wss.clients.forEach(function each(ws) {
    if (ws.isAlive === false) return ws.terminate();

    ws.isAlive = false;
    ws.ping();
  });
}, 30000);

wss.on('close', function close() {
  clearInterval(interval);
});
