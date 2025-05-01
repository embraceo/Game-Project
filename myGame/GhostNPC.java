package myGame;

import tage.GameObject;
import tage.*;
import org.joml.*;
import java.util.*;
import tage.shapes.*;

public class GhostNPC extends GameObject {
    private int id;

    public GhostNPC(int id, ObjShape s, TextureImage t, Vector3f p) {
        super(GameObject.root(), s, t);
        this.id = id;
        this.setLocalLocation(p);
    }

    public void setSize(boolean big) {
        if (!big) {
            this.setLocalScale((new Matrix4f()).scaling(0.5f));
        } else {
            this.setLocalScale((new Matrix4f()).scaling(1.0f));
        }
    }
}