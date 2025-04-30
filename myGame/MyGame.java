package myGame;

import tage.*;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;

import java.lang.Math;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.UUID;

import org.joml.*;
import net.java.games.input.Component;
import net.java.games.input.Event;

import tage.networking.IGameConnection.ProtocolType;

public class MyGame extends VariableFrameRateGame {
    private static Engine engine;

    private boolean paused = false;
    private int counter = 0;
    private double lastFrameTime, currFrameTime, elapsTime;

    private GameObject dol, vase;
    private ObjShape dolS;
    private TextureImage doltx;
    private TextureImage dolWireframeTx;
    private Light light1;

    // shapes
    private ObjShape waterPlaneShape, cubeShape, sphereShape, torusShape, vaseShape;
    private TextureImage waterTex, cubeTex, sphereTex, torusTex, vaseTex;
    private GameObject waterPlane, cube, sphere, torus;
    private ArrayList<GameObject> targetObjects = new ArrayList<>();

    // Added for networking
    private GhostManager gm;
    private String serverAddress;
    private int serverPort;
    private ProtocolType serverProtocol;
    private ProtocolClient protClient;
    private boolean isClientConnected = false;
    private InputManager im;

    // Added for skyboxes
    private int fluffyCloudsSkybox;
    private int lakeIslandsSkybox;

    private AnimatedShape robotS;
    private GameObject robot;

    public MyGame(String serverAddress, int serverPort, String protocol) {
        super();
        gm = new GhostManager(this);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        if (protocol.toUpperCase().compareTo("TCP") == 0)
            this.serverProtocol = ProtocolType.TCP;
        else
            this.serverProtocol = ProtocolType.UDP;
    }

    // Default constructor (for backwards compatibility)
    public MyGame() {
        super();
        gm = new GhostManager(this);
    }

    public static void main(String[] args) {
        MyGame game;

        if (args.length > 2) {
            game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
        } else {
            game = new MyGame();
            System.out.println("Using default single-player mode - no networking");
        }

        engine = new Engine(game);
        game.initializeSystem();
        game.game_loop();
    }

    @Override
    public void loadShapes() {
        dolS = new ImportedModel("dolphinHighPoly.obj");
        vaseShape = new ImportedModel("vase.obj");

        // Add new shapes
        waterPlaneShape = new TerrainPlane(100); // More detailed plane for water
        cubeShape = new Cube();
        sphereShape = new Sphere();
        torusShape = new Torus(0.5f, 0.2f, 48); // Inner radius, outer radius, precision
        robotS = new AnimatedShape("robot mesh.rkm", "robot skeleton.rks");
        robotS.loadAnimation("DEFAULT", "robot animation animation.rka");
    }

    @Override
    public void loadTextures() {
        // Load the existing dolphin texture
        doltx = new TextureImage("Dolphin_HighPolyUV.png");

        // Load the wireframe texture for ghosts
        dolWireframeTx = new TextureImage("Dolphin_HighPolyUV_wireframe.png");

        // Add new textures
        waterTex = new TextureImage("water.png");
        cubeTex = new TextureImage("brick.png");
        sphereTex = new TextureImage("metal.png");
        torusTex = new TextureImage("wood.png");
        vaseTex = new TextureImage("vasetest2.png");
    }

    @Override
    public void loadSkyBoxes() {
        // Load skybox textures
        fluffyCloudsSkybox = engine.getSceneGraph().loadCubeMap("fluffyClouds");
        lakeIslandsSkybox = engine.getSceneGraph().loadCubeMap("lakeIslands");

        // Set the active skybox
        engine.getSceneGraph().setSkyBoxEnabled(true);
        engine.getSceneGraph().setActiveSkyBoxTexture(fluffyCloudsSkybox);

        System.out.println("Skyboxes loaded successfully");
    }

    @Override
    public void buildObjects() {
        Matrix4f initialTranslation, initialScale, initialRotation;
        initialTranslation = (new Matrix4f()).translation(0, 2, 0);
        initialScale = (new Matrix4f()).scaling(3.0f);
        robot = new GameObject(GameObject.root(), robotS);
        robot.setLocalTranslation(initialTranslation);
        robot.setLocalScale(initialScale);

        vase = new GameObject(GameObject.root(), vaseShape, vaseTex);
        initialTranslation = (new Matrix4f()).translation(0, 2, 0);
        initialScale = (new Matrix4f()).scaling(3.0f);
        vase.setLocalTranslation(initialTranslation);
        vase.setLocalScale(initialScale);

        // Build the dolphin
        dol = new GameObject(GameObject.root(), dolS, doltx);
        initialTranslation = (new Matrix4f()).translation(0, 1, 0); // Dolphin starts slightly above water
        initialScale = (new Matrix4f()).scaling(3.0f);
        dol.setLocalTranslation(initialTranslation);
        dol.setLocalScale(initialScale);

        // Build water plane
        waterPlane = new GameObject(GameObject.root(), waterPlaneShape, waterTex);
        initialScale = (new Matrix4f()).scaling(100.0f, 1.0f, 100.0f); // Make water large
        initialTranslation = (new Matrix4f()).translation(0, -1, 0); // Below the dolphin
        waterPlane.setLocalScale(initialScale);
        waterPlane.setLocalTranslation(initialTranslation);

        // Build cubes at specific positions - further away from the dolphin
        GameObject cube1 = new GameObject(GameObject.root(), cubeShape, cubeTex);
        initialTranslation = (new Matrix4f()).translation(0, 0, -30); // Far ahead of dolphin
        initialScale = (new Matrix4f()).scaling(2.0f);
        cube1.setLocalTranslation(initialTranslation);
        cube1.setLocalScale(initialScale);
        targetObjects.add(cube1);

        GameObject cube2 = new GameObject(GameObject.root(), cubeShape, cubeTex);
        initialTranslation = (new Matrix4f()).translation(-25, 0, -25); // Far ahead and to the left
        initialScale = (new Matrix4f()).scaling(2.0f);
        cube2.setLocalTranslation(initialTranslation);
        cube2.setLocalScale(initialScale);
        targetObjects.add(cube2);

        GameObject cube3 = new GameObject(GameObject.root(), cubeShape, cubeTex);
        initialTranslation = (new Matrix4f()).translation(25, 0, -25); // Far ahead and to the right
        initialScale = (new Matrix4f()).scaling(2.0f);
        cube3.setLocalTranslation(initialTranslation);
        cube3.setLocalScale(initialScale);
        targetObjects.add(cube3);

        // Build spheres at specific positions
        GameObject sphere1 = new GameObject(GameObject.root(), sphereShape, sphereTex);
        initialTranslation = (new Matrix4f()).translation(30, 0, 0); // Far to the right
        initialScale = (new Matrix4f()).scaling(1.5f);
        sphere1.setLocalTranslation(initialTranslation);
        sphere1.setLocalScale(initialScale);
        targetObjects.add(sphere1);

        GameObject sphere2 = new GameObject(GameObject.root(), sphereShape, sphereTex);
        initialTranslation = (new Matrix4f()).translation(-30, 0, 0); // Far to the left
        initialScale = (new Matrix4f()).scaling(1.5f);
        sphere2.setLocalTranslation(initialTranslation);
        sphere2.setLocalScale(initialScale);
        targetObjects.add(sphere2);

        GameObject sphere3 = new GameObject(GameObject.root(), sphereShape, sphereTex);
        initialTranslation = (new Matrix4f()).translation(0, 0, 30); // Far behind
        initialScale = (new Matrix4f()).scaling(1.5f);
        sphere3.setLocalTranslation(initialTranslation);
        sphere3.setLocalScale(initialScale);
        targetObjects.add(sphere3);

        // Build a torus
        GameObject torus = new GameObject(GameObject.root(), torusShape, torusTex);
        initialTranslation = (new Matrix4f()).translation(0, 0, -40); // Far ahead, centered
        initialScale = (new Matrix4f()).scaling(5.0f);
        initialRotation = (new Matrix4f()).rotationX((float) Math.toRadians(90.0f)); // Flat orientation
        torus.setLocalTranslation(initialTranslation);
        torus.setLocalScale(initialScale);
        torus.setLocalRotation(initialRotation);
        targetObjects.add(torus);
    }

    @Override
    public void initializeLights() {
        Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
        light1 = new Light();
        light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
        (engine.getSceneGraph()).addLight(light1);
    }

    @Override
    public void initializeGame() {
        lastFrameTime = System.currentTimeMillis();
        currFrameTime = System.currentTimeMillis();
        elapsTime = 0.0;
        (engine.getRenderSystem()).setWindowDimensions(1900, 1000);

        // ------------- positioning the camera -------------
        (engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0, 0, 5));

        // Initialize input manager for networked gameplay
        if (serverAddress != null) {
            // Set up input manager
            im = new InputManager();
            setupInputs();

            // Set up networking
            setupNetworking();

            // Initialize the GhostManager with proper shape and texture
            gm.setGhostShape(dolS);
            gm.setGhostTexture(dolWireframeTx);
        }
    }

    public boolean isClientConnected() {
        return isClientConnected;
    }

    private void setupInputs() {
        // Use existing action classes with the new association method
        MoveAction moveForwardAction = new MoveAction(this, net.java.games.input.Component.Identifier.Key.W);
        MoveAction moveBackwardAction = new MoveAction(this, net.java.games.input.Component.Identifier.Key.S);
        TurnAction turnLeftAction = new TurnAction(this, net.java.games.input.Component.Identifier.Key.A);
        TurnAction turnRightAction = new TurnAction(this, net.java.games.input.Component.Identifier.Key.D);

        // Associate actions with keyboard keys
        im.associateActionWithAllKeyboards(
                Component.Identifier.Key.W,
                moveForwardAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(
                Component.Identifier.Key.S,
                moveBackwardAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(
                Component.Identifier.Key.A,
                turnLeftAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(
                Component.Identifier.Key.D,
                turnRightAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    }

    private void setupNetworking() {
        isClientConnected = false;
        try {
            protClient = new ProtocolClient(
                    InetAddress.getByName(serverAddress),
                    serverPort,
                    serverProtocol,
                    this);

            // Start connection attempt
            protClient.sendJoinMessage();

        } catch (UnknownHostException e) {
            System.out.println("ERROR: Unknown host: " + serverAddress);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("ERROR: Couldn't create socket to " + serverAddress + ":" + serverPort);
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
        robotS.updateAnimation();
        // Original update code
        lastFrameTime = currFrameTime;
        currFrameTime = System.currentTimeMillis();
        if (!paused)
            elapsTime += (currFrameTime - lastFrameTime) / 1000.0;

        // Update camera position to follow dolphin
        updateCameraPosition();

        // Get connection status string
        String connectionStatus = "";
        if (protClient != null) {
            if (isClientConnected) {
                connectionStatus = "SERVER: Connected";
            } else if (protClient.isAttemptingConnection()) {
                connectionStatus = "SERVER: Connecting... (Attempt " +
                        protClient.getConnectionAttempts() + ")";
            } else {
                connectionStatus = "SERVER: Disconnected (Press R to reconnect)";
            }

            // Check for timeout
            if (!isClientConnected && protClient.isAttemptingConnection()) {
                protClient.checkServerConnection();
            }
        } else {
            connectionStatus = "GAME MODE: Single Player";
        }

        // build and set HUD - combine time and connection status in HUD1, keep keyboard
        // hits in HUD2
        int elapsTimeSec = Math.round((float) elapsTime);
        String elapsTimeStr = Integer.toString(elapsTimeSec);
        String counterStr = Integer.toString(counter);
        String dispStr1 = "Time = " + elapsTimeStr + "  |  " + connectionStatus;

        // Pick color based on connection status
        Vector3f hud1Color;
        if (protClient == null) {
            hud1Color = new Vector3f(0, 0.7f, 1); // Light blue for single player
        } else if (isClientConnected) {
            hud1Color = new Vector3f(0, 1, 0); // Green when connected
        } else if (protClient.isAttemptingConnection()) {
            hud1Color = new Vector3f(1, 0.5f, 0); // Orange when connecting
        } else {
            hud1Color = new Vector3f(1, 0, 0); // Red when disconnected
        }

        Vector3f hud2Color = new Vector3f(0, 0, 1);
        (engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);

        // Networking update code
        if (protClient != null) {
            protClient.processPackets();
        }

        // Process inputs if using networked mode
        if (im != null) {
            im.update(0.0f); // Process inputs
        }
    }

    private void updateCameraPosition() {
        // Get the dolphin's position and orientation
        Vector3f dolPos = dol.getWorldLocation();
        Vector3f dolFwd = dol.getWorldForwardVector();

        // Calculate camera position: behind and above the dolphin
        // First, get the backwards direction (opposite of forward)
        Vector3f backDir = new Vector3f(dolFwd).mul(-1.0f);

        // Set the camera offset - distance behind and height above
        float cameraDistance = 5.0f; // distance behind
        float cameraHeight = 2.5f; // height above

        // Calculate camera position: dolphin position + (backDir * distance) + (up *
        // height)
        Vector3f cameraPos = new Vector3f(dolPos);
        cameraPos.add(new Vector3f(backDir).mul(cameraDistance)); // Move backward from dolphin
        cameraPos.add(new Vector3f(0.0f, cameraHeight, 0.0f)); // Move up

        // Update camera position
        (engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(cameraPos);

        // Make the camera look at the dolphin (or slightly above it)
        Vector3f lookAtPoint = new Vector3f(dolPos);
        lookAtPoint.add(new Vector3f(0.0f, 0.5f, 0.0f)); // Look slightly above the dolphin
        (engine.getRenderSystem().getViewport("MAIN").getCamera()).lookAt(lookAtPoint);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_C:
                counter++;
                break;
            case KeyEvent.VK_1:
                paused = !paused;
                break;
            case KeyEvent.VK_2:
                dol.getRenderStates().setWireframe(true);
                break;
            case KeyEvent.VK_3:
                dol.getRenderStates().setWireframe(false);
                break;
            case KeyEvent.VK_4:
                (engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0, 0, 0));
                break;
            case KeyEvent.VK_5:
                // Switch to fluffy clouds skybox
                (engine.getSceneGraph()).setActiveSkyBoxTexture(fluffyCloudsSkybox);
                System.out.println("Switched to Fluffy Clouds skybox");
                break;
            case KeyEvent.VK_6:
                // Switch to lake islands skybox
                (engine.getSceneGraph()).setActiveSkyBoxTexture(lakeIslandsSkybox);
                System.out.println("Switched to Lake Islands skybox");
                break;
            case KeyEvent.VK_ESCAPE:
                // Send bye message before shutting down if connected
                if (protClient != null && isClientConnected) {
                    protClient.sendByeMessage();
                }
                break;
            case KeyEvent.VK_R:
                // Attempt to reconnect if disconnected
                if (protClient != null && !isClientConnected) {
                    System.out.println("Attempting to reconnect to server...");
                    protClient.sendJoinMessage();
                }
                break;
        }
        super.keyPressed(e);
    }

    // --------- Accessor Methods ---------

    public GameObject getAvatar() {
        return dol;
    }

    public ObjShape getGhostShape() {
        return dolS;
    }

    public TextureImage getGhostTexture() {
        return doltx;
    }

    public GhostManager getGhostManager() {
        return gm;
    }

    public Engine getEngine() {
        return engine;
    }

    public Vector3f getPlayerPosition() {
        return dol.getWorldLocation();
    }

    public void setIsConnected(boolean isConnected) {
        this.isClientConnected = isConnected;
    }

    public ProtocolClient getProtocolClient() {
        return protClient;
    }

}