package myGame;

import java.io.IOException;

import tage.networking.IGameConnection.ProtocolType;

public class NetworkingServer {
    private static GameServerUDP thisUDPServer; // Made static
    
    public NetworkingServer(int serverPort, String protocol) {
        try {
            if(protocol.toUpperCase().compareTo("TCP") == 0) {
                // TCP server not implemented in this example
                System.out.println("TCP Server not implemented");
            } else {
                thisUDPServer = new GameServerUDP(serverPort);
                // No need to call getServerSocket() and setTimeouts - just start the server
                System.out.println("UDP Server started on port " + serverPort);
            }
        } catch (IOException e) {
            System.out.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        if(args.length > 1) {
            NetworkingServer app = new NetworkingServer(
                Integer.parseInt(args[0]), 
                args[1]
            );
            
            // Keep main thread running to maintain server
            while(true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Server main thread interrupted");
                    break;
                }
            }
            
            // try/catch block
            if(thisUDPServer != null) {
                try {
                    thisUDPServer.shutdown();
                } catch (Exception e) {  // Changed to Exception instead of IOException
                    System.out.println("Error during server shutdown");
                }
            }
        } else {
            System.out.println("Usage: java NetworkingServer <port> <protocol>");
        }
    }
}