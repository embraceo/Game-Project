package myGame;

import tage.*;
import tage.Light.LightType;
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
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsHingeConstraint;
import tage.physics.PhysicsObject;

import tage.audio.*;

public class MyGame extends VariableFrameRateGame {
    private static Engine engine;

    private boolean paused = false;
    private int counter = 0;
    private double lastFrameTime, currFrameTime, elapsTime;

    private GameObject dol, vase, flag, finishPlatform;
    private ObjShape dolS;
    private TextureImage doltx;
    private TextureImage dolWireframeTx;
    private TextureImage flagTexture;
    private Light light1, spotlight1, spotlight2, spotlight3, spotlight4, spotlight5;
    private Vector3f[] spotlightsPositions = new Vector3f[5];
    private boolean renderLights = true;

    // shapes
    private ObjShape waterPlaneShape, vaseShape;
    private TextureImage waterTex, vaseTex;
    private GameObject waterPlane;
    private ArrayList<GameObject> targetObjects = new ArrayList<>();
    private AnimatedShape vase_AnimatedShape;
    private ObjShape flagShape;

    // For networking
    private GhostManager gm;
    private String serverAddress;
    private int serverPort;
    private ProtocolType serverProtocol;
    private ProtocolClient protClient;
    private boolean isClientConnected = false;
    private InputManager im;

    // For skyboxes
    private int fluffyCloudsSkybox;
    private int lakeIslandsSkybox;
    private int grayVoidSkybox;

    private AnimatedShape robotS;
    private GameObject robot;

    // For terrain
    private GameObject terr;
    private ObjShape terrS;
    private TextureImage hills, grass;

    // For physics
    private PhysicsEngine physicsEngine;
    private PhysicsObject dolPhysics; // for the dolphin/avatar
    private ArrayList<GameObject> platforms = new ArrayList<>();
    private ArrayList<PhysicsObject> platformPhysics = new ArrayList<>();
    private ArrayList<PhysicsObject> pendulumPhysics = new ArrayList<>();
    private ArrayList<PhysicsObject> physicsObjects = new ArrayList<>();
    private ArrayList<GameObject> pendulums = new ArrayList<>();
    private boolean physicsRunning = true;
    private float[] vals = new float[16];

    private ArrayList<GameObject> movingPlatforms = new ArrayList<>();
    private ArrayList<PhysicsObject> movingPlatformPhysics = new ArrayList<>();
    private ArrayList<Vector3f> platformStartPositions = new ArrayList<>();
    private ArrayList<Vector3f> platformEndPositions = new ArrayList<>();
    private float platformMoveSpeed = 1f;

    // Pendulum swinging boosts
    private long lastBoostTime = 0;
    private float boostInterval = 2000; // Shorter interval (1 second instead of 2)
    private float boostForce = 100.0f;
    private float pendulumMass = 50.0f;

    // Movement flags
    private boolean dolphinMovingForward = false;
    private boolean dolphinMovingBackward = false;
    private boolean dolphinMovingLeft = false;
    private boolean dolphinMovingRight = false;
    private boolean dolphinJumping = false;

    // Audio
    private IAudioManager audioMgr;
    private Sound pendulumSound, splashSound, victorySound, backgroundSound;
    private boolean victoryPlayed = false;
    private long lastPendulumSoundTime = 0;
    private static final long SOUND_COOLDOWN = 2000; // 2 seconds

    private boolean soundEnabled = true; // Track if sound is enabled or not
    private boolean physicsRenderEnabled = false; // Track if physics rendering is enabled

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

    public ObjShape getNPCshape() {
        return vaseShape;
    }

    public AnimatedShape getNPCAnimatedShape() {
        return vase_AnimatedShape;
    }

    TextureImage getNPCtexture() {
        return vaseTex;
    }

    @Override
    public void loadShapes() {
        flagShape = new ImportedModel("finish_flag.obj");
        dolS = new ImportedModel("dolphinHighPoly.obj");
        vaseShape = new ImportedModel("vase.obj");
        vase_AnimatedShape = new AnimatedShape("vase_rkm.rkm", "vase_rks.rks");
        vase_AnimatedShape.loadAnimation("ANIMATION", "vase_rka.rka");

        // Add new shapes
        waterPlaneShape = new TerrainPlane(100); // More detailed plane for water
        robotS = new AnimatedShape("robot mesh.rkm", "robot skeleton.rks");
        robotS.loadAnimation("DEFAULT", "robot animation animation.rka");

        terrS = new TerrainPlane(1000); // 1000x1000 resolution
    }

    @Override
    public void loadTextures() {
        // Load the existing dolphin texture
        doltx = new TextureImage("Dolphin_HighPolyUV.png");

        // Load the wireframe texture for ghosts
        dolWireframeTx = new TextureImage("Dolphin_HighPolyUV_wireframe.png");

        // Add new textures
        waterTex = new TextureImage("water.png");
        vaseTex = new TextureImage("vasetest2.png");

        hills = new TextureImage("hills.png");
        grass = new TextureImage("grass.png");

        // Add platform texture
        TextureImage platformTex = new TextureImage("metal.png");

        flagTexture = new TextureImage("checker_texture.png");
    }

    @Override
    public void loadSkyBoxes() {
        // Load skybox textures
        fluffyCloudsSkybox = engine.getSceneGraph().loadCubeMap("fluffyClouds");
        lakeIslandsSkybox = engine.getSceneGraph().loadCubeMap("lakeIslands");
        grayVoidSkybox = engine.getSceneGraph().loadCubeMap("grayVoid");

        // Set the active skybox
        engine.getSceneGraph().setSkyBoxEnabled(true);
        engine.getSceneGraph().setActiveSkyBoxTexture(grayVoidSkybox);

        System.out.println("Skyboxes loaded successfully");
    }

    @Override
    public void loadSounds() {
        // Get audio manager from engine (don't create a new one)
        audioMgr = engine.getAudioManager();

        // Create audio resources
        AudioResource resource1, resource2, resource3, resource4;

        try {
            // resource1 = audioMgr.createAudioResource("whoosh_mono.wav",
            // AudioResourceType.AUDIO_SAMPLE);
            resource1 = audioMgr.createAudioResource("pendulum_whoosh.wav",
                    AudioResourceType.AUDIO_SAMPLE);
            // resource2 = audioMgr.createAudioResource("splash.wav",
            // AudioResourceType.AUDIO_SAMPLE);
            // resource3 = audioMgr.createAudioResource("victory.wav",
            // AudioResourceType.AUDIO_SAMPLE);
            // resource4 = audioMgr.createAudioResource("ocean.wav",
            // AudioResourceType.AUDIO_STREAM);

            // Create sounds with the correct constructor pattern
            pendulumSound = new Sound(resource1, SoundType.SOUND_EFFECT, 5, false);
            // splashSound = new Sound(resource2, SoundType.SOUND_EFFECT, 5, false);
            // victorySound = new Sound(resource3, SoundType.SOUND_EFFECT, 5, false);
            // backgroundSound = new Sound(resource4, SoundType.SOUND_MUSIC, 5, true);

            // Initialize sounds
            pendulumSound.initialize(audioMgr);
            // splashSound.initialize(audioMgr);
            // victorySound.initialize(audioMgr);
            // backgroundSound.initialize(audioMgr);

            // Set 3D sound properties
            pendulumSound.setMaxDistance(10.0f);
            pendulumSound.setMinDistance(0.5f);
            pendulumSound.setRollOff(5.0f);

            // splashSound.setMaxDistance(20.0f);
            // splashSound.setMinDistance(1.0f);
            // splashSound.setRollOff(5.0f);

            // victorySound.setMaxDistance(10.0f);
            // victorySound.setMinDistance(0.5f);
            // victorySound.setRollOff(5.0f);

            System.out.println("Sound resources loaded successfully");
        } catch (Exception e) {
            System.out.println("Error loading sound resources");
            e.printStackTrace();
        }
    }

    @Override
    public void buildObjects() {

        Matrix4f initialTranslation, initialScale, initialRotation;

        flag = new GameObject(GameObject.root(), flagShape, flagTexture);
        initialTranslation = (new Matrix4f()).translation(0, 0, 0);
        initialScale = (new Matrix4f()).scaling(1.0f);
        flag.setLocalTranslation(initialTranslation);
        flag.setLocalScale(initialScale);
        flag.setShape(flagShape);
        flag.setTextureImage(flagTexture);

        // flag.setLocalLocation(new Vector3f(0, 20, 35));

        initialTranslation = (new Matrix4f()).translation(0, 2, 0);
        initialScale = (new Matrix4f()).scaling(3.0f);
        robot = new GameObject(GameObject.root(), robotS);
        robot.setLocalTranslation(initialTranslation);
        robot.setLocalScale(initialScale);

        vase = new GameObject(GameObject.root(), vase_AnimatedShape, vaseTex);
        initialTranslation = (new Matrix4f()).translation(0, 4, 0);
        initialScale = (new Matrix4f()).scaling(3.0f);
        vase.setLocalTranslation(initialTranslation);
        vase.setLocalScale(initialScale);
        vase_AnimatedShape.playAnimation("ANIMATION", 0.5f, AnimatedShape.EndType.LOOP, 0);
        // Build the dolphin
        dol = new GameObject(GameObject.root(), dolS, doltx);
        initialTranslation = (new Matrix4f()).translation(0, 2, 0); // Dolphin starts slightly above water
        initialScale = (new Matrix4f()).scaling(3.0f);
        dol.setLocalTranslation(initialTranslation);
        dol.setLocalScale(initialScale);

        // Build water plane
        waterPlane = new GameObject(GameObject.root(), waterPlaneShape, waterTex);
        initialScale = (new Matrix4f()).scaling(300.0f, 1.0f, 300.0f); // Make water large
        initialTranslation = (new Matrix4f()).translation(0, -1, 0); // Below the dolphin
        waterPlane.setLocalScale(initialScale);
        waterPlane.setLocalTranslation(initialTranslation);

        // Add terrain object
        terr = new GameObject(GameObject.root(), terrS, grass);
        Matrix4f terrainTranslation = (new Matrix4f()).translation(0f, -2.0f, 0f); // move up or down
        terr.setLocalTranslation(terrainTranslation);
        Matrix4f terrainScale = (new Matrix4f()).scaling(300.0f, 4.0f, 300.0f); // size of terrain
        terr.setLocalScale(terrainScale);
        terr.setHeightMap(hills);

        // Set tiling for terrain texture
        terr.getRenderStates().setTiling(1);
        terr.getRenderStates().setTileFactor(40);

        // Create platforms - a series of platforms for the player to jump across
        for (int i = 0; i < 5; i++) {
            GameObject platform = new GameObject(GameObject.root(), new Cube(), new TextureImage("wood.png"));
            float x = (i % 2 == 0) ? 3.0f : -3.0f; // Zig-zag pattern left and right
            float z = i * 15.0f; // Space them out in z direction
            float y = 20.0f + ((i % 3) * 1.0f); // Varying heights
            spotlightsPositions[i] = new Vector3f(x, y + 1.0f, z);
            float width = 6.0f;
            float height = 0.5f;
            float depth = 5.0f;

            platform.setLocalTranslation((new Matrix4f()).translation(x, y, z));
            platform.setLocalScale((new Matrix4f()).scaling(width, height, depth));
            platforms.add(platform);
        }

        // create ending platform - hierarchical object with finish line

        // Add moving platforms
        createMovingPlatforms();

        // Create pendulum obstacles - ONLY VISUAL SETUP, NO PHYSICS YET
        for (int i = 0; i < 3; i++) {
            GameObject pendulum = new GameObject(GameObject.root(), new Sphere(), new TextureImage("metal.png"));
            float x = (i % 2 == 0) ? 0.0f : 0.0f; // Center aligned
            float z = (i + 1) * 15.0f - 7.5f; // Match platform spacing
            float y = 25.0f; // Hanging from above

            pendulum.setLocalTranslation((new Matrix4f()).translation(x, y, z));
            pendulum.setLocalScale((new Matrix4f()).scaling(1.5f));
            pendulums.add(pendulum);
        }
    }

    private void createMovingPlatforms() {
        // Create 3 moving platforms as the next stage of the obstacle course
        // Platform 1: Side-to-side movement
        GameObject platform1 = new GameObject(GameObject.root(), new Cube(), new TextureImage("brick.png"));
        float z1 = 75.0f; // 15 units after the last static platform
        float y1 = 20.0f; // Same height as starting platforms
        platform1.setLocalTranslation((new Matrix4f()).translation(0, y1, z1));
        platform1.setLocalScale((new Matrix4f()).scaling(5.0f, 0.5f, 4.0f));
        movingPlatforms.add(platform1);

        // Define horizontal movement path
        Vector3f startPos1 = new Vector3f(-8.0f, y1, z1);
        Vector3f endPos1 = new Vector3f(8.0f, y1, z1);
        platformStartPositions.add(startPos1);
        platformEndPositions.add(endPos1);

        // Platform 2: Up-and-down movement
        GameObject platform2 = new GameObject(GameObject.root(), new Cube(), new TextureImage("brick.png"));
        float z2 = 90.0f; // 15 units after the first moving platform
        float y2 = 19.0f; // Slightly lower base height
        platform2.setLocalTranslation((new Matrix4f()).translation(0, y2, z2));
        platform2.setLocalScale((new Matrix4f()).scaling(5.0f, 0.5f, 4.0f));
        movingPlatforms.add(platform2);

        Vector3f startPos2 = new Vector3f(-8f, y2, z2);
        Vector3f endPos2 = new Vector3f(8f, y2, z2);
        platformStartPositions.add(startPos2);
        platformEndPositions.add(endPos2);

        // finish platform, moves up and down with flag
        finishPlatform = new GameObject(GameObject.root(), new Cube(), new TextureImage("metal.png"));
        finishPlatform.setLocalTranslation((new Matrix4f()).translation(0, 10.0f, 115.0f));
        finishPlatform.setLocalScale((new Matrix4f()).scaling(5.0f, 0.5f, 4.0f));
        movingPlatforms.add(finishPlatform);

        flag.setParent(finishPlatform);
        flag.propagateTranslation(true);

        // Define vertical movement path
        startPos2 = new Vector3f(0, 5f, 115);
        endPos2 = new Vector3f(0, 10f, 115);
        platformStartPositions.add(startPos2);
        platformEndPositions.add(endPos2);

        // Platform 3: Diagonal movement with larger amplitude
        GameObject platform3 = new GameObject(GameObject.root(), new Cube(), new TextureImage("brick.png"));
        float z3 = 105.0f; // 15 units after the second moving platform
        float y3 = 21.0f; // Higher than other platforms
        platform3.setLocalTranslation((new Matrix4f()).translation(0, y3, z3));
        platform3.setLocalScale((new Matrix4f()).scaling(5.0f, 0.5f, 4.0f));
        movingPlatforms.add(platform3);

        // Define diagonal movement path
        Vector3f startPos3 = new Vector3f(-10.0f, y3 - 2.0f, z3);
        Vector3f endPos3 = new Vector3f(10.0f, y3 + 2.0f, z3);
        platformStartPositions.add(startPos3);
        platformEndPositions.add(endPos3);
    }

    private void setupPendulumPhysics() {
        System.out.println("Setting up pendulum physics for " + pendulums.size() + " pendulums");

        // Set proper gravity
        float[] gravity = { 0f, -9.8f, 0f };
        physicsEngine.setGravity(gravity);

        // Create an empty GameObject to hold the anchor points
        GameObject anchorsParent = new GameObject(GameObject.root());

        for (int i = 0; i < pendulums.size(); i++) {
            GameObject pendulum = pendulums.get(i);

            // Get original pendulum position
            Vector3f pendPos = pendulum.getWorldLocation();

            // Create anchor GameObject (invisible)
            GameObject anchor = new GameObject(anchorsParent);
            anchor.setLocalTranslation(new Matrix4f().translation(
                    pendPos.x(), pendPos.y() + 5.0f, pendPos.z()));

            // Add physics to anchor (static)
            Matrix4f anchorMatrix = anchor.getLocalTranslation();
            float[] vals = new float[16];
            anchorMatrix.get(vals);
            double[] anchorTransform = toDoubleArray(vals);

            PhysicsObject anchorPhysics = engine.getSceneGraph().addPhysicsBox(
                    0.0f, // Zero mass = static
                    anchorTransform,
                    new float[] { 0.5f, 0.5f, 0.5f });
            anchor.setPhysicsObject(anchorPhysics);

            // Move pendulum to offset position
            float xOffset = (i % 2 == 0) ? 5.0f : -5.0f;
            pendulum.setLocalTranslation(new Matrix4f().translation(
                    pendPos.x() + xOffset, pendPos.y(), pendPos.z()));

            // Get pendulum scale
            Vector3f scale = new Vector3f();
            pendulum.getLocalScale().getScale(scale);
            float radius = scale.x();

            // Add physics to pendulum
            Matrix4f pendulumMatrix = pendulum.getLocalTranslation();
            pendulumMatrix.get(vals);
            double[] pendulumTransform = toDoubleArray(vals);

            // Use the SceneGraph's method to add physics
            PhysicsObject pendulumPhysics = engine.getSceneGraph().addPhysicsSphere(
                    pendulumMass,
                    pendulumTransform,
                    radius);

            // Make sure collision detection is enabled
            pendulumPhysics.setBounciness(0.9f);
            pendulumPhysics.setFriction(0.1f); // Add friction
            pendulumPhysics.setDamping(0.0f, 0.0f);

            // Attach physics to GameObject
            pendulum.setPhysicsObject(pendulumPhysics);

            // Create hinge constraint
            int hingeID = physicsEngine.nextUID();
            PhysicsHingeConstraint hinge = physicsEngine.addHingeConstraint(
                    hingeID,
                    anchorPhysics,
                    pendulumPhysics,
                    0.0f, 0.0f, 1.0f // Z axis for left-right swinging
            );

            // Track physics objects
            physicsObjects.add(pendulumPhysics);

            System.out.println("Created pendulum " + i + " with offset " + xOffset);
        }

        // Print physics objects count
        System.out.println("Total physics objects: " + physicsObjects.size());
    }

    @Override
    public void initializeLights() {
        Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
        light1 = new Light();
        light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
        (engine.getSceneGraph()).addLight(light1);
        spotlight1 = new Light();
        spotlight2 = new Light();
        spotlight3 = new Light();
        spotlight4 = new Light();
        spotlight5 = new Light();

        spotlight1.setLocation(spotlightsPositions[0]);
        spotlight2.setLocation(spotlightsPositions[1]);
        spotlight3.setLocation(spotlightsPositions[2]);
        spotlight4.setLocation(spotlightsPositions[3]);
        spotlight5.setLocation(spotlightsPositions[4]);

        spotlight1.setType(LightType.SPOTLIGHT);
        spotlight2.setType(LightType.SPOTLIGHT);
        spotlight3.setType(LightType.SPOTLIGHT);
        spotlight4.setType(LightType.SPOTLIGHT);
        spotlight5.setType(LightType.SPOTLIGHT);

        spotlight1.setDirection(new Vector3f(0, -1, 0));
        spotlight2.setDirection(new Vector3f(0, -1, 0));
        spotlight3.setDirection(new Vector3f(0, -1, 0));
        spotlight4.setDirection(new Vector3f(0, -1, 0));
        spotlight5.setDirection(new Vector3f(0, -1, 0));

        (engine.getSceneGraph()).addLight(spotlight1);
        (engine.getSceneGraph()).addLight(spotlight2);
        (engine.getSceneGraph()).addLight(spotlight3);
        (engine.getSceneGraph()).addLight(spotlight4);
        (engine.getSceneGraph()).addLight(spotlight5);

    }

    @Override
    public void initializeGame() {
        lastFrameTime = System.currentTimeMillis();
        currFrameTime = System.currentTimeMillis();
        elapsTime = 0.0;
        (engine.getRenderSystem()).setWindowDimensions(1920, 1080);

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

        // Initialize physics system
        float[] gravity = { 0f, -9.8f, 0f };
        physicsEngine = (engine.getSceneGraph()).getPhysicsEngine();
        physicsEngine.setGravity(gravity);

        // Add physics to platforms (static)
        for (int i = 0; i < platforms.size(); i++) {
            GameObject platform = platforms.get(i);
            Matrix4f translation = new Matrix4f(platform.getLocalTranslation());

            translation.get(vals);
            double[] tempTransform = toDoubleArray(vals);

            // Get platform scale
            Vector3f scale = new Vector3f();
            platform.getLocalScale().getScale(scale);
            float[] size = { scale.x() * 2.0f, scale.y() * 2.0f, scale.z() * 2.0f };

            // Create static physics object (0 mass = static)
            PhysicsObject physics = (engine.getSceneGraph()).addPhysicsBox(0.0f, tempTransform, size);
            physics.setBounciness(0.3f);
            physics.setFriction(0.8f);
            platform.setPhysicsObject(physics);
            platformPhysics.add(physics);
        }

        // Add physics to moving platforms
        for (int i = 0; i < movingPlatforms.size(); i++) {
            GameObject platform = movingPlatforms.get(i);
            Matrix4f translation = new Matrix4f(platform.getLocalTranslation());
            translation.get(vals);
            double[] tempTransform = toDoubleArray(vals);

            // Get platform scale
            Vector3f scale = new Vector3f();
            platform.getLocalScale().getScale(scale);
            float[] size = { scale.x() * 2.0f, scale.y() * 2.0f, scale.z() * 2.0f };

            // Create kinematic physics object with very large mass
            // This will make it behave like a kinematic object
            PhysicsObject physics = (engine.getSceneGraph()).addPhysicsBox(10.0f, tempTransform, size);
            physics.setBounciness(0.3f);
            physics.setFriction(0.8f);

            // Set high damping to prevent movement from collisions
            physics.setDamping(0.9f, 0.9f);

            platform.setPhysicsObject(physics);
            movingPlatformPhysics.add(physics);
        }

        // Initial sound settings
        if (!pendulums.isEmpty() && pendulumSound != null) {
            pendulumSound.setLocation(pendulums.get(0).getWorldLocation());
        }

        if (splashSound != null) {
            splashSound.setLocation(waterPlane.getWorldLocation());
        }

        if (backgroundSound != null) {
            backgroundSound.play();
        }

        setEarParameters();

        // Add physics to ground plane (water)
        Matrix4f groundTranslation = new Matrix4f(waterPlane.getLocalTranslation());
        double[] groundTransform = toDoubleArray(groundTranslation.get(vals));
        float[] up = { 0, 1, 0 };
        PhysicsObject groundPhysics = (engine.getSceneGraph()).addPhysicsStaticPlane(groundTransform, up, 0.0f);
        groundPhysics.setBounciness(0.1f);
        groundPhysics.setFriction(0.8f);
        waterPlane.setPhysicsObject(groundPhysics);

        // Add physics to the player (dolphin)
        Matrix4f dolTranslation = new Matrix4f(dol.getLocalTranslation());
        double[] dolTransform = toDoubleArray(dolTranslation.get(vals));

        // Position dolphin on first platform
        if (!platforms.isEmpty()) {
            Vector3f platformPos = platforms.get(0).getWorldLocation();
            Vector3f platformScale = new Vector3f();
            platforms.get(0).getLocalScale().getScale(platformScale);

            // Adjust position to be on top of first platform
            dolTransform[12] = platformPos.x();
            dolTransform[13] = platformPos.y() + platformScale.y() + 1.0f; // Position above platform
            dolTransform[14] = platformPos.z();
        }

        // Create box physics for player instead of capsule
        float[] size = { 1.0f, 1.0f, 1.0f }; // Width, height, depth of box
        dolPhysics = (engine.getSceneGraph()).addPhysicsBox(1.0f, dolTransform, size);
        dolPhysics.setBounciness(0.2f); // Lower bounciness
        dolPhysics.setFriction(1.0f); // Maximum friction to prevent sliding

        // Keep the avatar from rotating too easily by increasing angular damping
        dolPhysics.setDamping(0.1f, 0.9f); // Low linear damping, high angular damping

        // Improve physics behavior
        dolPhysics.setSleepThresholds(0.0f, 0.0f); // Prevent the object from going to sleep

        // Connect physics to game object
        dol.setPhysicsObject(dolPhysics);

        // Enable physics debugging (commented out)
        engine.enablePhysicsWorldRender();

        // Clear any existing pendulum physics and set up new ones
        clearPendulumPhysics();
        setupPendulumPhysics();
    }

    public boolean detectIfAtFinishLine() {
        if (flag.getWorldLocation().distance(dol.getWorldLocation()) < 2) {
            return true;
        }
        return false;
    }

    private void clearPendulumPhysics() {
        // Remove physics objects from pendulums
        for (GameObject pendulum : pendulums) {
            PhysicsObject physics = pendulum.getPhysicsObject();
            if (physics != null) {
                pendulum.setPhysicsObject(null); // Detach physics object

                // Optional: try to remove from physics engine if possible
                // This depends on what methods are available in your physics engine
                // physicsEngine.removeObject(physics);
            }
        }

        // Clear the pendulum physics lists
        pendulumPhysics.clear();
        physicsObjects.clear();

        System.out.println("Cleared existing pendulum physics objects");
    }

    // Utility method
    private double[] toDoubleArray(float[] arr) {
        if (arr == null)
            return null;
        int n = arr.length;
        double[] ret = new double[n];
        for (int i = 0; i < n; i++) {
            ret[i] = (double) arr[i];
        }
        return ret;
    }

    public boolean isClientConnected() {
        return isClientConnected;
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
        vase_AnimatedShape.updateAnimation();
        lastFrameTime = currFrameTime;
        currFrameTime = System.currentTimeMillis();
        if (detectIfAtFinishLine()) {
            System.exit(0);
        }
        if (!paused)
            elapsTime += (currFrameTime - lastFrameTime) / 1000.0;

        // Update camera position to follow dolphin
        updateCameraPosition();

        updateSounds();

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

        // Update altitude of avatar based on height map
        Vector3f loc = dol.getWorldLocation();
        float height = terr.getHeight(loc.x(), loc.z());
        height += 0.5f; // add an offset to prevent sinking into terrain
        dol.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));

        // Physics update for obstacle course
        if (physicsRunning) {
            // Get current velocity
            float[] currentVel = dolPhysics.getLinearVelocity();

            // Keep vertical velocity from physics (for gravity)
            float yVel = currentVel[1];

            // REDUCED SPEED for better obstacle course navigation
            float moveSpeed = 8.0f; // Reduced from 15.0f to 8.0f

            // Initialize movement vector to accumulate directions
            Vector3f moveDir = new Vector3f(0, 0, 0);
            boolean movementKeyPressed = false;

            // Get camera reference properly from the engine
            Camera camera = engine.getRenderSystem().getViewport("MAIN").getCamera();

            // Get camera direction vectors
            Vector3f cameraFwd = camera.getN();
            Vector3f cameraRight = camera.getU();

            // accumulate movement directions instead of overriding
            if (dolphinMovingForward) { // W key
                moveDir.add(new Vector3f(cameraFwd.x(), 0, cameraFwd.z()));
                movementKeyPressed = true;
            }
            if (dolphinMovingBackward) { // S key
                moveDir.add(new Vector3f(-cameraFwd.x(), 0, -cameraFwd.z()));
                movementKeyPressed = true;
            }
            if (dolphinMovingRight) { // D key
                moveDir.add(new Vector3f(cameraRight.x(), 0, cameraRight.z()));
                movementKeyPressed = true;
            }
            if (dolphinMovingLeft) { // A key
                moveDir.add(new Vector3f(-cameraRight.x(), 0, -cameraRight.z()));
                movementKeyPressed = true;
            }

            // Set velocity based on accumulated directions
            if (movementKeyPressed) {
                // Normalize direction vector if it has length
                if (moveDir.length() > 0.1f) {
                    moveDir.normalize();
                }

                // Apply movement speed
                moveDir.mul(moveSpeed);

                // Set final velocity
                float xVel = moveDir.x();
                float zVel = moveDir.z();

                dolPhysics.setLinearVelocity(new float[] { xVel, yVel, zVel });

                // Force rotation lock by resetting the physics transform rotation part
                double[] currentTransform = dolPhysics.getTransform();
                double[] newTransform = new double[16];

                // Copy the identity rotation part
                newTransform[0] = 1.0;
                newTransform[1] = 0.0;
                newTransform[2] = 0.0;
                newTransform[3] = 0.0;
                newTransform[4] = 0.0;
                newTransform[5] = 1.0;
                newTransform[6] = 0.0;
                newTransform[7] = 0.0;
                newTransform[8] = 0.0;
                newTransform[9] = 0.0;
                newTransform[10] = 1.0;
                newTransform[11] = 0.0;

                // Copy just the translation part from current transform
                newTransform[12] = currentTransform[12];
                newTransform[13] = currentTransform[13];
                newTransform[14] = currentTransform[14];
                newTransform[15] = 1.0;

                // Apply the modified transform
                dolPhysics.setTransform(newTransform);
            } else {
                // If no movement keys are pressed, stop horizontal movement
                dolPhysics.setLinearVelocity(new float[] { 0.0f, yVel, 0.0f });
            }

            // Prevent any physics-based rotation
            dolPhysics.setAngularVelocity(new float[] { 0.0f, 0.0f, 0.0f });

            // Jump handling
            if (dolphinJumping) {
                // Only jump if near ground (small vertical velocity)
                if (Math.abs(yVel) < 0.5f) {
                    System.out.println("Jumping!");
                    // Get current movement direction but add upward velocity
                    float[] jumpVel = dolPhysics.getLinearVelocity();
                    jumpVel[1] = 10.0f; // Slightly reduced jump height too
                    dolPhysics.setLinearVelocity(jumpVel);
                }
                dolphinJumping = false;
            }

            // Update moving platforms
            updateMovingPlatforms();

            // Update physics world
            physicsEngine.update((float) elapsTime);

            boostPendulums();

            // Update ONLY the position of the dolphin from physics, not rotation
            Matrix4f physMatrix = new Matrix4f();
            float[] transform = toFloatArray(dolPhysics.getTransform());
            physMatrix.set(transform);

            // Create new translation matrix
            Matrix4f translationMatrix = new Matrix4f().identity();
            translationMatrix.setTranslation(physMatrix.m30(), physMatrix.m31(), physMatrix.m32());

            // Apply translation only
            dol.setLocalTranslation(translationMatrix);

            // Update pendulum positions from physics objects
            for (int i = 0; i < pendulums.size(); i++) {
                if (i < physicsObjects.size()) {
                    PhysicsObject physObj = physicsObjects.get(i);
                    GameObject pendulum = pendulums.get(i);

                    // Get transform from physics
                    double[] physTransform = physObj.getTransform();

                    // Create translation matrix from physics position
                    Matrix4f pendulumMatrix = new Matrix4f().identity();
                    pendulumMatrix.setTranslation(
                            (float) physTransform[12],
                            (float) physTransform[13],
                            (float) physTransform[14]);

                    // Update pendulum position only (not rotation)
                    pendulum.setLocalTranslation(pendulumMatrix);
                }
            }

            // Check if player fell below a certain height
            if (dol.getWorldLocation().y() < -8.0f) {
                System.out.println("Player fell into water!");
                resetPlayerPosition();
            }
        }
    }

    private void updateMovingPlatforms() {
        for (int i = 0; i < movingPlatforms.size(); i++) {
            GameObject platform = movingPlatforms.get(i);
            Vector3f startPos = platformStartPositions.get(i);
            Vector3f endPos = platformEndPositions.get(i);
            PhysicsObject physObj = platform.getPhysicsObject();

            // Calculate movement pattern based on platform index
            float time = (float) elapsTime;
            float factor;

            switch (i) {
                case 0: // Side to side movement
                    factor = (float) Math.sin(time * platformMoveSpeed) * 0.5f + 0.5f;
                    break;
                case 1: // Up-down movement
                    factor = (float) Math.sin(time * platformMoveSpeed * 1.5f) * 0.5f + 0.5f;
                    break;
                case 2: // Diagonal movement
                    double sinVal = Math.sin(time * platformMoveSpeed * 0.8f);
                    factor = (float) (Math.pow(sinVal, 3) * 0.5f + 0.5f);
                    break;
                default:
                    factor = (float) Math.sin(time * platformMoveSpeed) * 0.5f + 0.5f;
            }

            // Interpolate between start and end positions
            Vector3f newPos = new Vector3f();
            startPos.lerp(endPos, factor, newPos);

            // Update visual transform
            platform.setLocalLocation(newPos);

            // Update the physics object's transform
            if (physObj != null) {
                // Create a transform matrix from the new position
                Matrix4f trans = new Matrix4f().identity().setTranslation(newPos);
                trans.get(vals);
                physObj.setTransform(toDoubleArray(vals));
            }
        }
    }

    private void boostPendulums() {
        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < pendulums.size() && i < physicsObjects.size(); i++) {
            PhysicsObject physics = physicsObjects.get(i);

            // Get current velocity
            float[] velocity = physics.getLinearVelocity();
            float velocityX = velocity[0];

            // Determine boost direction
            float direction = (i % 2 == 0) ? -1.0f : 1.0f;

            // Get current position
            double[] transform = physics.getTransform();
            float positionX = (float) transform[12];
            float positionY = (float) transform[13];
            float positionZ = (float) transform[14];

            // Determine if this pendulum needs a boost
            boolean needsBoost = false;

            // Case 1: Regular interval boost (maintains swing rhythm)
            if (currentTime - lastBoostTime > boostInterval) {
                needsBoost = true;
            }

            // Case 2: Emergency boost if moving too slowly (post-collision recovery)
            if (Math.abs(velocityX) < 3.0f) {
                needsBoost = true;
            }

            if (needsBoost) {
                // If pendulum has moved past center, boost in the opposite direction
                if ((positionX > 0 && direction > 0) || (positionX < 0 && direction < 0)) {
                    direction *= -1; // Reverse direction
                }

                // Calculate boost force - stronger boost when moving slower
                float actualBoost = boostForce;
                if (Math.abs(velocityX) < 2.0f) {
                    actualBoost = boostForce * 1.5f; // 50% stronger for slow pendulums
                }

                // Apply boost force in X direction
                physics.applyForce(
                        direction * actualBoost, 0.0f, 0.0f, // Force in X direction
                        positionX, positionY, positionZ // Apply at current position
                );
            }
        }

        // Only update the timer on regular interval boosts
        if (currentTime - lastBoostTime > boostInterval) {
            lastBoostTime = currentTime;
        }
    }

    // Update ear position
    private void setEarParameters() {
        Camera camera = engine.getRenderSystem().getViewport("MAIN").getCamera();
        if (audioMgr != null && audioMgr.getEar() != null) {
            audioMgr.getEar().setLocation(camera.getLocation());
            audioMgr.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
        }
    }

    private void updateSounds() {
        if (audioMgr == null || !soundEnabled)
            return; // Skip sound updates if disabled

        // Update ear position
        setEarParameters();

        // Update pendulum sound (find the fastest moving pendulum)
        if (pendulumSound != null && !pendulums.isEmpty() && !physicsObjects.isEmpty()) {
            float maxSpeed = 0.0f;
            int fastestIndex = -1;

            for (int i = 0; i < pendulums.size() && i < physicsObjects.size(); i++) {
                PhysicsObject physics = physicsObjects.get(i);
                if (physics != null) {
                    float[] velocity = physics.getLinearVelocity();
                    // Focus on horizontal movement (X-axis) since that's the main swing direction
                    float speed = Math.abs(velocity[0]);

                    if (speed > maxSpeed) {
                        maxSpeed = speed;
                        fastestIndex = i;
                    }
                }
            }

            // Only play sound if pendulum is moving fast enough
            if (fastestIndex >= 0 && maxSpeed > 8.0f) {
                GameObject pendulum = pendulums.get(fastestIndex);
                Vector3f pendulumPos = pendulum.getWorldLocation();

                // Update sound position
                pendulumSound.setLocation(pendulumPos);

                // Play without distance check, will be heard from anywhere
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastPendulumSoundTime > SOUND_COOLDOWN) {
                    pendulumSound.play();
                    System.out.println("Pendulum sounds played");
                    lastPendulumSoundTime = currentTime;
                }
            }
        }

        // Check if player fell in water
        if (dol != null && splashSound != null) {
            if (dol.getWorldLocation().y() < -0.5f) {
                splashSound.setLocation(dol.getWorldLocation());
                splashSound.play();
            }
        }

        // Check for victory condition
        if (!victoryPlayed && victorySound != null && !platforms.isEmpty()) {
            GameObject finalPlatform = platforms.get(platforms.size() - 1);
            Vector3f playerPos = dol.getWorldLocation();
            Vector3f platformPos = finalPlatform.getWorldLocation();

            // Simplified distance check - much easier to trigger
            float xzDistance = (float) Math.sqrt(
                    Math.pow(playerPos.x() - platformPos.x(), 2) +
                            Math.pow(playerPos.z() - platformPos.z(), 2));

            if (xzDistance < 8.0f) { // Increased distance threshold
                victorySound.setLocation(playerPos);
                victorySound.play();
                victoryPlayed = true;
            }
        }
    }

    @Override
    public void shutdown() {
        // Stop all sounds
        if (audioMgr != null) {
            audioMgr.stopAllSounds();
        }

        // Rest of shutdown code...
        super.shutdown();
    }

    // Helper method to reset player position to the first platform
    private void resetPlayerPosition() {
        if (!platforms.isEmpty()) {
            // Get position of first platform
            Vector3f platformPos = platforms.get(0).getWorldLocation();
            Vector3f platformScale = new Vector3f();
            platforms.get(0).getLocalScale().getScale(platformScale);

            // Create identity matrix for transform
            double[] resetTransform = new double[16];
            for (int i = 0; i < 16; i++) {
                resetTransform[i] = 0.0;
            }
            resetTransform[0] = 1.0;
            resetTransform[5] = 1.0;
            resetTransform[10] = 1.0;
            resetTransform[15] = 1.0;

            // Set position above first platform
            resetTransform[12] = platformPos.x();
            resetTransform[13] = platformPos.y() + platformScale.y() + 1.0f;
            resetTransform[14] = platformPos.z();

            // Reset physics object position and velocity
            dolPhysics.setTransform(resetTransform);
            dolPhysics.setLinearVelocity(new float[] { 0.0f, 0.0f, 0.0f });
            dolPhysics.setAngularVelocity(new float[] { 0.0f, 0.0f, 0.0f });
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
        float cameraHeight = 1.5f; // height above

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

    // Utility method to convert double array to float array
    private float[] toFloatArray(double[] arr) {
        if (arr == null)
            return null;
        int n = arr.length;
        float[] ret = new float[n];
        for (int i = 0; i < n; i++) {
            ret[i] = (float) arr[i];
        }
        return ret;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
                dolphinMovingForward = true;
                break;
            case KeyEvent.VK_S:
                dolphinMovingBackward = true;
                break;
            case KeyEvent.VK_A:
                dolphinMovingLeft = true;
                break;
            case KeyEvent.VK_D:
                dolphinMovingRight = true;
                break;
            case KeyEvent.VK_SPACE:
                dolphinJumping = true;
                break;
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
            case KeyEvent.VK_P:
                // Toggle physics
                physicsRunning = !physicsRunning;
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
            case KeyEvent.VK_E:
                System.out.println("Resetting player position to start platform");
                resetPlayerPosition();
                break;
            case KeyEvent.VK_M:
                soundEnabled = !soundEnabled;
                if (soundEnabled) {
                    System.out.println("Sound effects enabled");
                    if (backgroundSound != null) {
                        backgroundSound.play();
                    }
                } else {
                    System.out.println("Sound effects disabled");
                    if (pendulumSound != null) {
                        pendulumSound.stop();
                    }
                    if (splashSound != null) {
                        splashSound.stop();
                    }
                    if (backgroundSound != null) {
                        backgroundSound.stop();
                    }
                }
                break;
            case KeyEvent.VK_F:
                physicsRenderEnabled = !physicsRenderEnabled;
                if (physicsRenderEnabled) {
                    System.out.println("Physics world rendering enabled");
                    engine.enablePhysicsWorldRender();
                } else {
                    System.out.println("Physics world rendering disabled");
                    engine.disablePhysicsWorldRender();
                }
                break;
            case KeyEvent.VK_K:
                renderLights = !renderLights;
                if (renderLights == true) {
                    spotlight1.enable();
                    spotlight2.enable();
                    spotlight3.enable();
                    spotlight4.enable();
                    spotlight5.enable();

                } else {
                    spotlight1.disable();
                    spotlight2.disable();
                    spotlight3.disable();
                    spotlight4.disable();
                    spotlight5.disable();

                }
                break;

        }
        super.keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
                dolphinMovingForward = false;
                break;
            case KeyEvent.VK_S:
                dolphinMovingBackward = false;
                break;
            case KeyEvent.VK_A:
                dolphinMovingLeft = false;
                break;
            case KeyEvent.VK_D:
                dolphinMovingRight = false;
                break;
        }
        super.keyReleased(e);
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