package ctfbot;

import cz.cuni.amis.utils.Cooldown;

public class CaptureBehavior implements IBehavior {

	
	private CTFBot bot = null;

	public CaptureBehavior(CTFBot ctfBot) {
		this.bot = ctfBot;
	}
	
	@Override
	public void run() {
    	draw();
    	bot.getShoot().stopShooting();
    	if(bot.getGameState().getEnemyFlag().getHolder() == bot.getInfo().getId()) {
    		bot.goTo(bot.getCTF().getOurBase().getLocation());
    	}else {
    		bot.goTo(bot.getGameState().getEnemyFlagLocation());    		    		
    	}
    	
    	
    	bot.navigationPathDrawn = false;
	}
	
	private void draw() {
		if (bot.getNavigation().isNavigating()) {
    		if (CTFBot.DRAW_NAVIGATION_PATH) {
    			if (!bot.navigationPathDrawn) {
    				bot.drawNavigationPath(true);
    				bot.navigationPathDrawn = true;
    			}
    		}
    	}
	}

	@Override
	public boolean isFiring() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getPriority() {
		if(bot.getGameState().getEnemyFlag().getHolder() == bot.getInfo().getId()) {
			return 300;
		}
		return 100;
	}

}
