package myGame;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import org.joml.Vector3f;
import org.joml.Matrix4f;

import tage.networking.client.GameConnectionClient;
import tage.GameObject;
import tage.networking.IGameConnection.ProtocolType;

public class ProtocolClient extends GameConnectionClient {
    
    private MyGame game;
    private UUID id;
    private GhostManager ghostManager;
    
    // Add connection checking fields
    private boolean isAttemptingConnection = false;
    private long connectionAttemptTime;
    private long connectionTimeout = 5000; // 5 seconds timeout
    private int connectionAttempts = 0;
    private int maxConnectionAttempts = 3;
    
    public ProtocolClient(InetAddress remAddr, int remPort, 
                          ProtocolType pType, MyGame game) throws IOException {
        super(remAddr, remPort, pType);
        this.game = game;
        this.id = UUID.randomUUID();
        ghostManager = game.getGhostManager();
        
        // Initialize connection tracking
        this.connectionAttemptTime = System.currentTimeMillis();
        System.out.println("ProtocolClient created - connecting to server at " + 
                           remAddr.getHostAddress() + ":" + remPort);
    }
    
    @Override
    protected void processPacket(Object message) {
        String strMessage = (String)message;

        if (message == null) {
            System.out.println("Warning: Received null packet");
            return;
        }
        
        String[] msgTokens = strMessage.split(",");
        
        if(msgTokens.length > 0) {
            // Handle JOIN response
            if(msgTokens[0].compareTo("join") == 0) {
                // format: join, success or join, failure
                if(msgTokens[1].compareTo("success") == 0) {
                    game.setIsConnected(true);
                    isAttemptingConnection = false;
                    System.out.println("Successfully connected to server");
                    sendCreateMessage(game.getPlayerPosition());
                }
                if(msgTokens[1].compareTo("failure") == 0) {
                    game.setIsConnected(false);
                    isAttemptingConnection = false;
                    System.out.println("Server rejected connection");
                }
            }
            
            // Handle BYE message
            if(msgTokens[0].compareTo("bye") == 0) {
                // format: bye, remoteId
                UUID ghostID = UUID.fromString(msgTokens[1]);
                ghostManager.removeGhostAvatar(ghostID);
            }
            
            // Handle CREATE message
            if(msgTokens[0].equals("create")) {
                // IMPORTANT DEBUG PRINT
                System.out.println("RECEIVED CREATE MESSAGE: " + strMessage);
                
                // Check that this create message isn't for our own avatar
                if(!msgTokens[1].equals(id.toString())) {
                    System.out.println("Creating ghost avatar for client: " + msgTokens[1]);
                    
                    // Create position for ghost avatar based on received data
                    Vector3f ghostPosition = new Vector3f(
                        Float.parseFloat(msgTokens[2]),
                        Float.parseFloat(msgTokens[3]), 
                        Float.parseFloat(msgTokens[4])
                    );
                    
                    try {
                        ghostManager.createGhostAvatar(UUID.fromString(msgTokens[1]), ghostPosition);
                    } catch (IOException e) {
                        System.out.println("Error creating ghost avatar: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } 
            // Handle DSFR messages
            else if(msgTokens[0].compareTo("dsfr") == 0) {
                // format: dsfr, remoteId, x,y,z
                UUID ghostID = UUID.fromString(msgTokens[1]);
                Vector3f ghostPosition = new Vector3f(
                    Float.parseFloat(msgTokens[2]),
                    Float.parseFloat(msgTokens[3]),
                    Float.parseFloat(msgTokens[4]));
                try {
                    ghostManager.createGhostAvatar(ghostID, ghostPosition);
                } catch (IOException e) {
                    System.out.println("error creating ghost avatar");
                }
            }
            
            // Handle WANTS DETAILS message
            if(msgTokens[0].compareTo("wsds") == 0) {
                // format: wsds, remoteId
                UUID ghostID = UUID.fromString(msgTokens[1]);
                sendDetailsForMessage(ghostID, game.getPlayerPosition());
            }
            
            // Handle MOVE message
            if(msgTokens[0].compareTo("move") == 0) {
                // format: move, remoteId, x, y, z, rotX, rotZ
                UUID ghostID = UUID.fromString(msgTokens[1]);
                Vector3f ghostPosition = new Vector3f(
                    Float.parseFloat(msgTokens[2]),
                    Float.parseFloat(msgTokens[3]),
                    Float.parseFloat(msgTokens[4]));

                float rotX = 1.0f;
                float rotZ = 0.0f;
                if (msgTokens.length >= 7) {
                    rotX = Float.parseFloat(msgTokens[5]);
                    rotZ = Float.parseFloat(msgTokens[6]);
                    System.out.println("Received rotation: " + rotX + ", " + rotZ);
                }
                ghostManager.updateGhostAvatar(ghostID, ghostPosition, rotX, rotZ);
            }
        }
    }
    
    /**
     * Checks if we're still attempting to connect, and if a timeout has occurred.
     * Returns true if we're connected or still trying, false if all attempts failed.
     */
    public boolean checkServerConnection() {
        if (game.isClientConnected()) {
            return true;
        }
        
        if (!isAttemptingConnection) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Check if we've timed out waiting for connection
        if (currentTime - connectionAttemptTime > connectionTimeout) {
            connectionAttempts++;
            
            if (connectionAttempts >= maxConnectionAttempts) {
                System.out.println("Failed to connect to server after " + maxConnectionAttempts + " attempts");
                isAttemptingConnection = false;
                return false;
            }
            
            // Try again
            System.out.println("Connection attempt " + connectionAttempts + " timed out. Retrying...");
            sendJoinMessage();
            connectionAttemptTime = currentTime;
        }
        
        return true;
    }
    
    public void sendJoinMessage() {
        // format: join, localId, x, y, z
        try {
            Vector3f position = game.getPlayerPosition();
            System.out.println("Sending join message to server...");
            String message = "join," + id.toString();
            message += "," + position.x() + "," + position.y() + "," + position.z();
            sendPacket(message);
            isAttemptingConnection = true;
            connectionAttemptTime = System.currentTimeMillis();
        } catch (IOException e) {
            System.out.println("ERROR: Failed to send join message: " + e.getMessage());
            e.printStackTrace();
        }
    }    
    
    public void sendCreateMessage(Vector3f pos) {
        // format: (create, localId, x,y,z)
        try {
            String message = new String("create," + id.toString());
            message += "," + pos.x() + "," + pos.y() + "," + pos.z();
            sendPacket(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void sendByeMessage() {
        // format: bye, localId
        try {
            String message = new String("bye," + id.toString());
            sendPacket(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void sendDetailsForMessage(UUID remId, Vector3f pos) {
        // format: dsfr, remoteId, localId, x, y, z
        try {
            String message = new String("dsfr," + remId.toString() + "," + id.toString());
            message += "," + pos.x() + "," + pos.y() + "," + pos.z();
            sendPacket(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void sendMoveMessage(Vector3f pos) {
        // Get avatar rotation information
        GameObject avatar = game.getAvatar();
        Matrix4f rotation = avatar.getLocalRotation();
        
        // Extract rotation values
        float rotX = rotation.m00();
        float rotZ = rotation.m02();
        
        // format: move, localId, x, y, z, rotX, rotZ
        try {
            String message = new String("move," + id.toString());
            message += "," + pos.x() + "," + pos.y() + "," + pos.z();
            message += "," + rotX + "," + rotZ;
            sendPacket(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Added getters for new fields
    public boolean isAttemptingConnection() {
        return isAttemptingConnection;
    }
    
    public int getConnectionAttempts() {
        return connectionAttempts;
    }
    
    // Reset connection attempts
    public void resetConnectionAttempts() {
        connectionAttempts = 0;
        isAttemptingConnection = false;
    }
}