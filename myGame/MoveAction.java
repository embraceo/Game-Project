package myGame;

import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

public class MoveAction extends AbstractInputAction {
    private MyGame game;
    private net.java.games.input.Component.Identifier.Key keyPressed;

    public MoveAction(MyGame g, net.java.games.input.Component.Identifier.Key k) {
        game = g;
        keyPressed = k;
    }

    @Override
    public void performAction(float time, Event e) {
        GameObject avatar = game.getAvatar();
        Vector3f oldPos = avatar.getWorldLocation();
        Vector3f fwdDirection = avatar.getWorldForwardVector();
        Vector3f newPos = new Vector3f(oldPos);
        
        float moveSpeed = 0.050f; // Adjust speed as needed
        
        if (keyPressed == net.java.games.input.Component.Identifier.Key.W) {
            newPos.add(new Vector3f(fwdDirection).mul(moveSpeed));
        } else if (keyPressed == net.java.games.input.Component.Identifier.Key.S) {
            newPos.sub(new Vector3f(fwdDirection).mul(moveSpeed));
        }
        
        avatar.setLocalLocation(newPos);
        
        // Send move message to server if connected
        if (game.getProtocolClient() != null && game.isClientConnected()) {
            game.getProtocolClient().sendMoveMessage(newPos);
        }
    }
}