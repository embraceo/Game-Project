package myGame;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import tage.VariableFrameRateGame;

public class GhostManager {
    
    private MyGame game;
    private HashMap<UUID, GameObject> ghostAvatars = new HashMap<UUID, GameObject>();
    private ObjShape ghostShape;
    private TextureImage ghostTexture;
    
    public GhostManager(VariableFrameRateGame vfrg) {
        game = (MyGame)vfrg;
    }
    
    public void setGhostShape(ObjShape s) { ghostShape = s; }

    public void setGhostTexture(TextureImage t) { ghostTexture = t; }

    private void setGhostColor(GameObject ghostObj) {
        Vector3f yellowColor = new Vector3f(1.0f, 1.0f, 0.0f);
        ghostObj.getRenderStates().setColor(yellowColor);
        ghostObj.getRenderStates().setHasSolidColor(true);
    }    
    public void createGhostAvatar(UUID id, Vector3f position) throws IOException {
        if (!ghostAvatars.containsKey(id)) {
            GameObject ghostAvatar = new GameObject(GameObject.root(), ghostShape, ghostTexture);
            
            // Set position
            ghostAvatar.setLocalLocation(position);
            
            // Set the same scale as the player's avatar
            Matrix4f scale = (new Matrix4f()).scaling(3.0f); // Same scale as in buildObjects
            ghostAvatar.setLocalScale(scale);

            setGhostColor(ghostAvatar);
            
            ghostAvatars.put(id, ghostAvatar);
        }
    }
    
    public void removeGhostAvatar(UUID id) {
        GameObject ghostAvatar = ghostAvatars.get(id);
        if(ghostAvatar != null) {
            game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
            ghostAvatars.remove(id);
        } else {
            System.out.println("unable to find ghost in list");
        }
    }
    
    public void updateGhostAvatar(UUID id, Vector3f position, float m00, float m02) {
        GameObject ghostAvatar = ghostAvatars.get(id);
        if (ghostAvatar != null) {
            // Update position
            ghostAvatar.setLocalLocation(position);
            
            // Reconstruct rotation matrix directly from the matrix elements
            Matrix4f rotation = new Matrix4f().identity();
            rotation.m00(m00);
            rotation.m02(m02);
            rotation.m20(-m02);  // For a Y-rotation matrix, m20 = -m02
            rotation.m22(m00);   // For a Y-rotation matrix, m22 = m00
            
            ghostAvatar.setLocalRotation(rotation);
        }
    }
    
    public GameObject getGhostAvatar(UUID id) {
        return ghostAvatars.get(id);
    }
    
    public void updateLocalGhostAvatar(UUID id) {
        GameObject avatar = game.getAvatar();
        if (avatar != null) {
            updateGhostAvatar(id, avatar.getLocalLocation(), 
                            avatar.getLocalRotation().m00(), 
                            avatar.getLocalRotation().m02());
        }
    }

    // Add this method to check if the GhostManager is ready to create avatars
    public boolean isReady() {
        return (ghostShape != null && ghostTexture != null);
    }
}