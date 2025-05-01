package myGame;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;

import org.joml.Vector3f;

import tage.networking.server.IClientInfo;
import tage.networking.IGameConnection.ProtocolType;
import tage.networking.server.GameConnectionServer;

public class GameServerUDP extends GameConnectionServer<UUID> {
    private HashMap<UUID, Vector3f> gamePositions;
    private HashMap<UUID, InetAddress> clientAddresses;
    private HashMap<UUID, Integer> clientPorts;
    public NPCcontroller npcCtrl;

    public GameServerUDP(int localPort, NPCcontroller npc) throws IOException {
        super(localPort, ProtocolType.UDP);
        gamePositions = new HashMap<UUID, Vector3f>();
        clientAddresses = new HashMap<UUID, InetAddress>();
        clientPorts = new HashMap<UUID, Integer>();
        npcCtrl = npc;
    }

    @Override
    public void processPacket(Object o, InetAddress senderAddress, int senderPort) {
        String message = (String) o;
        String[] messageTokens = message.split(",");

        if (messageTokens.length > 0) {
            // Handle JOIN message
            if (messageTokens[0].compareTo("join") == 0) {
                try {
                    IClientInfo ci = getServerSocket().createClientInfo(senderAddress, senderPort);
                    UUID clientID = UUID.fromString(messageTokens[1]);

                    // Check if this client is already connected - FIXED
                    if (clientAddresses.containsKey(clientID)) {
                        System.out.println("Client " + clientID + " is already connected");

                        // Send success message back to client
                        try {
                            String joinResponse = "join,success";
                            getServerSocket().sendPacket(senderAddress, senderPort, joinResponse);
                        } catch (IOException e) {
                            System.out.println("Error sending join success response");
                        }
                        return;
                    }

                    // Store the client's address and port
                    clientAddresses.put(clientID, senderAddress);
                    clientPorts.put(clientID, senderPort);

                    // Add the client to the server
                    addClient(ci, clientID);
                    System.out.println("Client " + clientID.toString() + " has joined");

                    // Store the client's position
                    Vector3f position;
                    if (messageTokens.length >= 5) {
                        position = new Vector3f(
                                Float.parseFloat(messageTokens[2]),
                                Float.parseFloat(messageTokens[3]),
                                Float.parseFloat(messageTokens[4]));
                    } else {
                        position = new Vector3f(0.0f, 1.0f, 0.0f);
                        System.out.println("Warning: Client joined without position data");
                    }
                    gamePositions.put(clientID, position);

                    // Explicitly respond to client with success message
                    try {
                        String joinResponse = "join,success";
                        getServerSocket().sendPacket(senderAddress, senderPort, joinResponse);
                        System.out.println("Sent join success response to client " + clientID);
                    } catch (IOException e) {
                        System.out.println("Error sending join success response");
                    }

                    // Debug: Print all known clients
                    System.out.println("=== CURRENT CLIENTS IN GAME ===");
                    for (UUID id : gamePositions.keySet()) {
                        System.out.println("Client: " + id.toString() + " at position: " + gamePositions.get(id));
                    }
                    System.out.println("===============================");

                    // Tell the newly joined client about ALL existing clients
                    for (UUID existingID : new ArrayList<>(gamePositions.keySet())) {
                        if (existingID.equals(clientID))
                            continue;

                        Vector3f existingPosition = gamePositions.get(existingID);
                        if (existingPosition != null) {
                            String createMessage = "create," + existingID.toString();
                            createMessage += "," + existingPosition.x() + "," + existingPosition.y() + ","
                                    + existingPosition.z();

                            try {
                                System.out.println("SENDING TO NEW CLIENT: " + createMessage);
                                getServerSocket().sendPacket(senderAddress, senderPort, createMessage);
                            } catch (IOException e) {
                                System.out.println("Failed to send create message to new client");
                            }
                        }
                    }

                    // Tell all existing clients about the new client
                    String createMessage = "create," + clientID.toString();
                    createMessage += "," + position.x() + "," + position.y() + "," + position.z();

                    for (UUID existingID : new ArrayList<>(gamePositions.keySet())) {
                        if (existingID.equals(clientID))
                            continue;

                        try {
                            InetAddress clientAddr = clientAddresses.get(existingID);
                            int clientPort = clientPorts.get(existingID);

                            System.out.println("SENDING TO EXISTING CLIENT " + existingID + ": " + createMessage);
                            getServerSocket().sendPacket(clientAddr, clientPort, createMessage);
                        } catch (IOException e) {
                            System.out.println("Failed to notify existing client");
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Error in join message handling: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Handle MOVE message
            else if (messageTokens[0].compareTo("move") == 0) {
                try {
                    UUID clientID = UUID.fromString(messageTokens[1]);

                    // Update the client's position in our storage
                    Vector3f position = new Vector3f(
                            Float.parseFloat(messageTokens[2]),
                            Float.parseFloat(messageTokens[3]),
                            Float.parseFloat(messageTokens[4]));
                    gamePositions.put(clientID, position);

                    System.out.println("Move from client " + clientID + " to position " + position);

                    // Forward movement to all clients INDIVIDUALLY for better reliability
                    for (UUID existingID : new ArrayList<>(clientAddresses.keySet())) {
                        if (existingID.equals(clientID))
                            continue;

                        try {
                            InetAddress clientAddr = clientAddresses.get(existingID);
                            int clientPort = clientPorts.get(existingID);
                            getServerSocket().sendPacket(clientAddr, clientPort, message);
                        } catch (IOException e) {
                            System.out.println("Error forwarding move to client " + existingID);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error processing move message: " + e.getMessage());
                }
            }

            // Handle BYE message
            else if (messageTokens[0].compareTo("bye") == 0) {
                try {
                    UUID clientID = UUID.fromString(messageTokens[1]);

                    // Tell all clients this client is leaving
                    String byeMessage = "bye," + clientID.toString();

                    for (UUID existingID : new ArrayList<>(clientAddresses.keySet())) {
                        if (existingID.equals(clientID))
                            continue;

                        try {
                            InetAddress clientAddr = clientAddresses.get(existingID);
                            int clientPort = clientPorts.get(existingID);
                            getServerSocket().sendPacket(clientAddr, clientPort, byeMessage);
                        } catch (IOException e) {
                            System.out.println("Error sending bye message to client " + existingID);
                        }
                    }

                    // Remove the client
                    removeClient(clientID);
                    gamePositions.remove(clientID);
                    clientAddresses.remove(clientID);
                    clientPorts.remove(clientID);
                    System.out.println("Client " + clientID.toString() + " has left");
                } catch (Exception e) {
                    System.out.println("Error processing bye message: " + e.getMessage());
                }
            }

            // Handle DETAILS message
            else if (messageTokens[0].compareTo("details") == 0) {
                // Find which client sent this message
                UUID senderId = null;
                for (UUID id : clientAddresses.keySet()) {
                    if (clientAddresses.get(id).equals(senderAddress) &&
                            clientPorts.get(id) == senderPort) {
                        senderId = id;
                        break;
                    }
                }

                if (senderId != null) {
                    for (UUID existingID : new ArrayList<>(clientAddresses.keySet())) {
                        if (existingID.equals(senderId))
                            continue;

                        try {
                            InetAddress clientAddr = clientAddresses.get(existingID);
                            int clientPort = clientPorts.get(existingID);
                            getServerSocket().sendPacket(clientAddr, clientPort, message);
                        } catch (IOException e) {
                            System.out.println("Error forwarding details to client " + existingID);
                        }
                    }
                }
            }
            // Case where server receives request for NPCs
            // Received Message Format: (needNPC,id)
            if (messageTokens[0].compareTo("needNPC") == 0) {
                System.out.println("server got a needNPC message");
                UUID clientID = UUID.fromString(messageTokens[1]);
                sendNPCstart(clientID);
            }
            // Case where server receives notice that an av is close to the npc
            // Received Message Format: (isnear,id)
            if (messageTokens[0].compareTo("isnear") == 0) {
                UUID clientID = UUID.fromString(messageTokens[1]);
                handleNearTiming(clientID);
            }

            else {
                System.out.println("Unknown message type: " + messageTokens[0]);
            }
        }
    }

    public void sendCheckForAvatarNear() {
        try {
            String message = new String("isnear");
            message += "," + (npcCtrl.getNPC()).getX();
            message += "," + (npcCtrl.getNPC()).getY();
            message += "," + (npcCtrl.getNPC()).getZ();
            message += "," + (npcCtrl.getCriteria());
            sendPacketToAll(message);
        } catch (IOException e) {
            System.out.println("couldnt send msg");
            e.printStackTrace();
        }
    }

    public void sendNPCinfo() {
        try {
            String message = new String("mnpc");
            message += "," + (npcCtrl.getNPC()).getX();
            message += "," + (npcCtrl.getNPC()).getY();
            message += "," + (npcCtrl.getNPC()).getZ();
            sendPacketToAll(message);
        } catch (IOException e) {
            System.out.println("couldnt send msg");
            e.printStackTrace();
        }
    }

    public void sendNPCstart(UUID clientID) {
    }

    public void handleNearTiming(UUID clientID) {
        npcCtrl.setNearFlag(true);
    }

    public void sendCreateNPCmsg(UUID clientID, String[] position) {
        try {
            System.out.println("server telling clients about an NPC");
            String message = new String("createNPC," + clientID.toString());
            message += "," + position[0];
            message += "," + position[1];
            message += "," + position[2];
            forwardPacketToAll(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void acceptClient(IClientInfo ci, Object o) {
        System.out.println("Client connected via acceptClient");
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            System.out.println("Server shutting down");
        } catch (IOException e) {
            System.out.println("Error during shutdown");
        }
    }
}