package ctfbot;

import cz.cuni.amis.pogamut.ut2004.agent.navigation.NavigationState;
import cz.cuni.amis.utils.Cooldown;
import cz.cuni.amis.utils.flag.FlagListener;

public class StuckBehavior implements IBehavior {
	
	int priority = 0;
	private CTFBot bot = null;
	private Cooldown unstuckingCooldown = new Cooldown(5000);

	public StuckBehavior(CTFBot ctfBot) {
		this.bot = ctfBot;
		bot.getNavigation().addStrongNavigationListener(
	      		   new FlagListener<NavigationState>() {
	      		      @Override
	      		      public void flagChanged(NavigationState changedValue){
	      		          switch (changedValue) {
	      		              case STUCK:
	      		            	  increasePriority();
	      		                  break;
	      		              case TARGET_REACHED:
	      		            	  priority = 0;
	      		                  break;
	      		              case PATH_COMPUTATION_FAILED:
	      		            	increasePriority();
	      		                  break;                      
	      		          }
	      		      }
	      		   });
	    	
	}
	
	@Override
	public void run() {
		if(unstuckingCooldown.tryUse()) {
			priority = 0;
		}
		// RANDOM NAVIGATION
    	if (bot.getNavigation().isNavigating()) {
    		if (CTFBot.DRAW_NAVIGATION_PATH) {
    			if (!bot.navigationPathDrawn) {
    				bot.drawNavigationPath(true);
    				bot.navigationPathDrawn = true;
    			}
    		}
    		return;
    	}
    	bot.getNavigation().navigate(bot.getNavPoints().getRandomNavPoint());
    	bot.navigationPathDrawn = false;
    	bot.getLog().info("RUNNING TO: " + bot.getNavigation().getCurrentTarget());
	}

	@Override
	public boolean isFiring() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean terminate() {
		if(unstuckingCooldown.isHot()) {
			return false;
		}else {
			return true;			
		}
	}
	
	public void increasePriority() {
		priority += 6;
	}

	@Override
	public int getPriority() {
		if(unstuckingCooldown.isHot()) {
			return 400;
		}else {
			return priority;			
		}
	}

}
