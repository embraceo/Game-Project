package myGame;

import tage.ai.behaviortrees.BTCondition;

public class OneSecPassed extends BTCondition {
    private NPCcontroller npcCtrl;
    private NPC npc;
    private long lastTime;
    private boolean initialized = false;

    public OneSecPassed(NPCcontroller c, NPC n, boolean negated) {
        super(negated);
        npcCtrl = c;
        npc = n;
    }

    @Override
    protected boolean check() {
        long now = System.currentTimeMillis();
        if (!initialized) {
            lastTime = now;
            initialized = true;
            return false;
        }

        if (now - lastTime >= 1000) { // 1 second passed
            lastTime = now;
            System.out.println("OneSecPassed true");
            return true;
        }

        return false;
    }
}
