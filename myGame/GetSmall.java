package myGame;

import tage.ai.behaviortrees.BTCondition;

public class GetSmall extends BTCondition {
    private NPC npc;

    public GetSmall(NPC n) {
        super(false); // Not negated
        npc = n;
    }

    @Override
    protected boolean check() {
        npc.getSmall(); // Shrinks the NPC
        return true;
    }
}