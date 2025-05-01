package myGame;

import tage.ai.behaviortrees.BTCondition;

public class GetBig extends BTCondition {
    private NPC npc;

    public GetBig(NPC n) {
        super(false); // Not negated
        npc = n;
    }

    @Override
    protected boolean check() {
        System.out.println("TRYING TO GET BIGGGGGG");
        npc.getBig(); // Grows the NPC
        return true;
    }
}