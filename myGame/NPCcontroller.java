package myGame;

import myGame.GameServerUDP;
import java.util.Random;
import tage.ai.behaviortrees.BTCondition;
//import tage.ai.behaviortrees.*;
import tage.*;
import tage.ai.behaviortrees.BehaviorTree;
import tage.ai.behaviortrees.BTCompositeType;
import tage.ai.behaviortrees.BTSequence;

public class NPCcontroller {
    private NPC npc;
    Random rn = new Random();
    BehaviorTree bt = new BehaviorTree(BTCompositeType.SELECTOR);
    boolean nearFlag = false;
    long thinkStartTime, tickStartTime;
    long lastThinkUpdateTime, lastTickUpdateTime;
    GameServerUDP server;
    double criteria = 2.0;

    public void updateNPCs() {
        npc.updateLocation();
    }

    public void start(GameServerUDP s) {
        thinkStartTime = System.nanoTime();
        tickStartTime = System.nanoTime();
        lastThinkUpdateTime = thinkStartTime;
        lastTickUpdateTime = tickStartTime;
        server = s;
        setupNPCs();
        setupBehaviorTree();
        npcLoop();
    }

    public void setupNPCs() {
        npc = new NPC();
        npc.randomizeLocation(rn.nextInt(40), rn.nextInt(40));
    }

    public void npcLoop() {
        while (true) {
            long currentTime = System.nanoTime();
            float elapsedThinkMilliSecs = (currentTime - lastThinkUpdateTime) / (1000000.0f);
            float elapsedTickMilliSecs = (currentTime - lastTickUpdateTime) / (1000000.0f);
            if (elapsedTickMilliSecs >= 25.0f) {
                lastTickUpdateTime = currentTime;
                npc.updateLocation();
                server.sendNPCinfo();
                server.sendCheckForAvatarNear();
            }
            if (elapsedThinkMilliSecs >= 250.0f) {
                lastThinkUpdateTime = currentTime;
                bt.update(elapsedThinkMilliSecs);
            }
            Thread.yield();
        }
    }

    public void setupBehaviorTree() {
        bt.insertAtRoot(new BTSequence(10));
        bt.insertAtRoot(new BTSequence(20));
        bt.insert(10, new OneSecPassed(this, npc, false));
        bt.insert(10, new GetSmall(npc));
        bt.insert(20, new AvatarNear(server, this, npc, false));
        bt.insert(20, new GetBig(npc));
    }

    public boolean getNearFlag() {
        return nearFlag;
    }

    public void setNearFlag(boolean b) {
        nearFlag = b;
    }

    public double getCriteria() {
        return criteria;
    }

    public NPC getNPC() {
        return npc;
    }
}