package myGame;

import java.util.UUID;
import org.joml.Vector3f;
import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;

public class GhostAvatar extends GameObject {
    
    private UUID id;
    
    public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p) {
        super(GameObject.root(), s, t);
        this.id = id;
        setPosition(p);
    }
    
    // Getter for the UUID
    public UUID getID() {
        return id;
    }
    
    // Set the position of the ghost avatar
    public void setPosition(Vector3f p) {
        setLocalLocation(p);
    }
    
    // Get the position of the ghost avatar
    public Vector3f getPosition() {
        return getWorldLocation();
    }
}
