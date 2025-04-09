package myGame;

import tage.*;
import tage.input.action.AbstractInputAction;
import tage.input.InputManager;
import net.java.games.input.Event;
import org.joml.*;

public class TurnAction extends AbstractInputAction {
    private MyGame game;
    private net.java.games.input.Component.Identifier.Key keyPressed;

    public TurnAction(MyGame g, net.java.games.input.Component.Identifier.Key k) {
        game = g;
        keyPressed = k;
    }

    @Override
    public void performAction(float time, Event e) {
        GameObject avatar = game.getAvatar();
        Matrix4f rot;
        
        // Determine rotation direction based on key
        if (keyPressed == net.java.games.input.Component.Identifier.Key.A) {
            rot = (new Matrix4f()).rotationY(0.01f);  // Turn left
        } else {  // assume D
            rot = (new Matrix4f()).rotationY(-0.01f); // Turn right
        }
        
        avatar.setLocalRotation(avatar.getLocalRotation().mul(rot));
        
        // Send move message to server if connected
        if (game.getProtocolClient() != null && game.isClientConnected()) {
            game.getProtocolClient().sendMoveMessage(avatar.getWorldLocation());
        }
    }
}